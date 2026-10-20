package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeLimeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.server.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.*;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeServerGeneration;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeNmeProjectInfoService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.client.HaxeDisplayClient;
import com.intellij.plugins.haxe.display.client.HaxeVersionProbe;
import com.intellij.plugins.haxe.display.client.JsonRpcDisplayClient;
import com.intellij.plugins.haxe.display.haxe3.Haxe3DisplayClient;
import com.intellij.plugins.haxe.display.protocol.Diagnostic;
import com.intellij.plugins.haxe.display.protocol.DiagnosticSeverity;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.FileDiagnostics;
import com.intellij.plugins.haxe.display.protocol.InitializeResult;
import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import com.intellij.plugins.haxe.display.transport.HaxeDisplayTransport;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContext;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContexts;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.PsiErrorElementUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Connects editor features to the compilation server's display protocol.
/// It derives the build context of a haxe source file, turns it into the
/// compiler arguments every display request starts with, keeps the server
/// running and checks which methods the server supports.
///
/// HXML build files supply their arguments directly. Lime-family files
/// (openfl, lime, hxp) run `haxelib run <tool> display <file> <target>`, whose
/// hxml output is the fully evaluated argument set (macros, libraries and
/// conditional sources included); the result is cached per build-file stamp.
/// NMML files use the hxml of the most recent `nme prepare` evaluation, which
/// references that evaluation's retained prepared directory.
///
/// The other compiler services build on this one. Their callers hold the read
/// lock, so they answer from their caches only. A cache miss schedules a
/// background request that fills the cache ("hydration"), and highlighting
/// restarts when the result arrives.
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerDisplayService implements Disposable {

  /**
   * The part of a display request computed under the read lock. Either
   * {@code args} is known already (HXML, NMML), or {@code lime} still has to
   * run on a background thread. The build file's IDE overrides are applied
   * to whichever argument list results.
   */
  public record DisplayContext(@Nullable List<String> args,
                               @Nullable LimeDisplaySpec lime,
                               @Nullable String sdkName,
                               @NotNull HaxeDisplayConfiguration.OverrideArguments overrides,
                               @NotNull String containerId) {
  }

  /** The inputs of a {@code lime display} run, captured under the read lock and run later in the background. */
  public record LimeDisplaySpec(@NotNull String directory,
                                @NotNull String fileName,
                                @NotNull String tool,
                                @NotNull String targetFlag,
                                long fileStamp) {
  }

  /** The display interface a server's haxe speaks: JSON-RPC from haxe 4 on, {@code --display <file>@<offset>} in haxe 3. */
  private enum DisplayProtocol { JSON_RPC, HAXE_3 }

  /** The methods and haxe version a running server reported on initialize, and its protocol; kept per port. */
  private record Capability(@NotNull Set<String> methods,
                            @Nullable InitializeResult.SemVer haxeVersion,
                            @NotNull DisplayProtocol protocol) {
  }

  /**
   * A client for a running server that supports the requested method, with
   * the resolved arguments. {@code port} identifies the server the per-server
   * state (capabilities, unsaved files) is kept for: the shared server's port,
   * or the loopback port a dedicated display process connected to.
   */
  record Connected(@NotNull HaxeDisplayClient client, @NotNull List<String> args, int port) {
  }

  /**
   * The server behind a client, the identity its per-server state is keyed on,
   * and the console entry its request metrics count under.
   */
  private record Endpoint(@NotNull DisplayChannel channel, int id, @NotNull String metricsId) {
  }

  /** The read timeout of a whole-context compile (warm-up, inlay types, dump) on a cold server; a single display answer keeps the client's default. */
  static final int CONTEXT_COMPILE_TIMEOUT_MS = 120_000;
  /** How long a freshly spawned server may take to bind its port before a first request gives up. */
  private static final int STARTUP_RETRY_MS = 10_000;
  private static final int VERSION_PROBE_TIMEOUT_MS = 10_000;

  /** A first request to a server, which {@link #withStartupRetry} sends again while the server is not listening yet. */
  @FunctionalInterface
  private interface StartupRequest<T> {
    T send() throws DisplayRequestException;
  }

  private final Project project;
  /** Keys of the contexts compiled this session; the server's module cache fills only from a compile. */
  private final Set<String> compiledContexts = ConcurrentHashMap.newKeySet();
  /** What each running server answered on initialize, by port; a server state change empties it. */
  private final Map<Integer, Capability> capabilities = new ConcurrentHashMap<>();
  private final HaxeServerUnsavedFiles unsavedFiles = new HaxeServerUnsavedFiles();

  /** A file's display context and the {@link #contextInputsStamp} it was computed under. */
  private record CachedContext(long inputsStamp, @Nullable DisplayContext context) {
  }

  private final Map<VirtualFile, CachedContext> contextCache = new ConcurrentHashMap<>();
  /** Bumped by every build setting, build event and resolve-context choice a display context derives from. */
  private final SimpleModificationTracker contextInputs = new SimpleModificationTracker();

  public HaxeCompilerDisplayService(@NotNull Project project) {
    this.project = project;
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(HaxeBuildSettingsListener.TOPIC, HaxeBuildSettingsListener.onAny(contextInputs::incModificationCount));
    connection.subscribe(HaxeBuildEventListener.TOPIC, HaxeBuildEventListener.onAny(contextInputs::incModificationCount));
    connection.subscribe(HaxeResolveContextListener.TOPIC, (HaxeResolveContextListener)contextInputs::incModificationCount);
  }

  @Override
  public void dispose() {
  }

  @NotNull
  public static HaxeCompilerDisplayService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerDisplayService.class);
  }

  @NotNull
  public Project getProject() {
    return project;
  }

  /**
   * The key of the per-server state for the context's server (see
   * {@link Connected#port()}): the id of its running display process in
   * dedicated mode, else the compilation server's port; -1 while nothing
   * runs. Starts nothing, so it may run under the read lock.
   */
  int runningEndpointIdFor(@NotNull DisplayContext context) {
    HaxeServerGeneration generation = HaxeServerGenerations.known(project, context.sdkName());
    HaxeServerAssignment assignment = HaxeServerAssignment.forContainer(project, context.containerId());
    if (HaxeCompilerSettings.getInstance(project).usesDedicatedDisplayProcess(generation)) {
      int id = HaxeDisplayProcessManager.getInstance(project).runningIdFor(assignment);
      if (id > 0) return id;
    }
    return HaxeCompilationServerManager.getInstance(project).runningPort(assignment.id());
  }

  /**
   * The haxe version of the server serving the context; null while that
   * server does not run or has not answered an initialize yet. Each SDK, and
   * each container with a server of its own, has its own server, so a
   * mixed-version project asks per context.
   */
  @Nullable
  public InitializeResult.SemVer connectedHaxeVersion(@NotNull DisplayContext context) {
    Capability known = knownCapabilityOf(context);
    return known != null ? known.haxeVersion() : null;
  }

  /**
   * Whether the server serving the context is known to lack the method: it
   * runs and its initialize answer did not list it. False while that is
   * unknown; starts nothing, so it may run under the read lock.
   */
  boolean lacksMethod(@NotNull DisplayContext context, @NotNull String method) {
    Capability known = knownCapabilityOf(context);
    return known != null && !known.methods().contains(method);
  }

  /** What the context's running server answered on initialize; null while none runs or it has not answered. */
  @Nullable
  private Capability knownCapabilityOf(@NotNull DisplayContext context) {
    int endpointId = runningEndpointIdFor(context);
    return endpointId > 0 ? capabilities.get(endpointId) : null;
  }

  /**
   * The display context of a haxe source file, derived from the build the
   * file is analysed under ({@link HaxeResolveContexts}). Null when the file
   * has no supported build context. Call in a read action; does not touch
   * the network or spawn processes.
   *
   * Cached per file until {@link #contextInputsStamp} moves: resolve asks
   * for every reference that fails statically, and working the context out
   * walks the build files.
   */
  @Nullable
  public DisplayContext contextFor(@NotNull VirtualFile file) {
    long inputsStamp = contextInputsStamp();
    CachedContext cached = contextCache.get(file);
    if (cached != null && cached.inputsStamp() == inputsStamp) return cached.context();

    HaxeResolveContext resolveContext = HaxeResolveContexts.getInstance(project).contextFor(file);
    DisplayContext context = resolveContext == null ? null : contextOfBuildFile(resolveContext.buildFile().file());
    contextCache.put(file, new CachedContext(inputsStamp, context));
    return context;
  }

  /**
   * Moves whenever a display context may change: any PSI change (a build
   * file edit included), a project-root change, or a build setting, build
   * event or resolve-context choice. Read BEFORE computing, so an input
   * changing during the computation leaves the result stale-stamped.
   */
  long contextInputsStamp() {
    return PsiModificationTracker.getInstance(project).getModificationCount()
           + ProjectRootModificationTracker.getInstance(project).getModificationCount()
           + contextInputs.getModificationCount();
  }

  /** The build inputs alone (project roots, build settings and events, resolve-context choices): an edit does not move it. */
  long buildInputsStamp() {
    return ProjectRootModificationTracker.getInstance(project).getModificationCount() + contextInputs.getModificationCount();
  }

  /**
   * The display context of a module's default build file, for callers
   * without a source file (the type catalog works per module). Same contract
   * as {@link #contextFor(VirtualFile)}.
   */
  @Nullable
  public DisplayContext contextFor(@NotNull Module module) {
    HaxeBuildFile buildFile = HaxeResolveContexts.getInstance(project).defaultBuildFileFor(module);
    return buildFile == null ? null : contextOfBuildFile(buildFile.file());
  }

  @Nullable
  private DisplayContext contextOfBuildFile(@NotNull VirtualFile buildFile) {
    if (buildFile.getParent() == null) return null;
    HaxeBuildFileType type = HaxeBuildFileScanner.detectType(project, buildFile);
    // a plain hxp script generates its compiler args in code - nothing to derive statically
    if (type == null || type == HaxeBuildFileType.HXP_SCRIPT) return null;

    String containerId = HaxeContainers.containerIdFor(project, buildFile);
    HaxeEnvironmentStore environment = HaxeEnvironmentStore.getInstance(project);
    // display requests use the server the module's builds use, so the
    // per-container opt-out applies to them too
    if (!environment.isUsingCompilationServer(containerId)) return null;
    String sdkName = environment.getSdkName(containerId);
    String directory = buildFile.getParent().getPath();
    HaxeBuildFile typedBuildFile = new HaxeBuildFile(buildFile, type);
    HaxeDisplayConfiguration.OverrideArguments overrides = HaxeDisplayConfiguration.overridesFor(project, typedBuildFile, containerId);

    if (type == HaxeBuildFileType.HXML) {
      // hxml paths resolve against the file's work directory, not its folder
      String hxmlDirectory = HaxeBuildWorkDirectories.workDirectory(project, buildFile);
      if (hxmlDirectory == null) return null;
      return new DisplayContext(hxmlContextArgs(buildFile, hxmlDirectory), null, sdkName, overrides, containerId);
    }
    if (type == HaxeBuildFileType.NMML) {
      return nmeContext(buildFile, directory, sdkName, overrides, containerId);
    }
    String tool = LimeProjects.toolFor(type);
    String targetFlag = LimeProjects.selectedTargetFlag(project, type, buildFile);
    LimeDisplaySpec lime =
      new LimeDisplaySpec(directory, buildFile.getName(), tool, targetFlag, buildFile.getModificationStamp());
    return new DisplayContext(null, lime, sdkName, overrides, containerId);
  }

  /**
   * The context arguments of an HXML file. A file with a single section is
   * passed by reference, and the server expands it. A {@code --next} chain is
   * narrowed to the SELECTED section, spelled out as compiler arguments.
   *
   * A chained file passed whole would make every server request carry ALL
   * sections. Every context compile (cache warm-up, failure explanation, each
   * request) would then build every target in turn and delay later
   * {@code --connect} builds. Diagnostics would also answer for the first
   * section instead of the one selected in the tool window.
   */
  @NotNull
  private List<String> hxmlContextArgs(@NotNull VirtualFile buildFile, @NotNull String directory) {
    List<String> byReference = List.of(HxmlArguments.CWD_FLAG, directory, HaxeBuildWorkDirectories.fileArgument(buildFile, directory));
    return List.copyOf(HxmlProjects.scopeToSelectedSection(project, buildFile, byReference));
  }

  /**
   * The NMML context. The generated hxml of the most recent {@code nme prepare}
   * evaluation becomes the argument list. Null until an evaluation has
   * finished; the tool window schedules one on every scan, so the context
   * appears shortly after the project opens.
   */
  @Nullable
  private DisplayContext nmeContext(@NotNull VirtualFile buildFile,
                                    @NotNull String directory,
                                    @Nullable String sdkName,
                                    @NotNull HaxeDisplayConfiguration.OverrideArguments overrides,
                                    @NotNull String containerId) {
    String targetFlag = NmeProjects.selectedTargetFlag(project, buildFile);
    HaxeNmeProjectInfoService.Evaluation evaluation = HaxeNmeProjectInfoService.getInstance(project)
      .getCachedOrSchedule(new HaxeBuildFile(buildFile, HaxeBuildFileType.NMML), targetFlag, sdkName);
    if (evaluation == null) return null;

    List<String> args = new ArrayList<>(List.of(HxmlArguments.CWD_FLAG, directory));
    args.addAll(HxmlArguments.parseLines(evaluation.hxmlContent().lines().toList()));
    return new DisplayContext(List.copyOf(args), null, sdkName, overrides, containerId);
  }

  /**
   * Diagnostics for one file. {@code contents} carries the editor buffer when
   * it differs from disk. The file is then invalidated on the server first,
   * because a cached module would otherwise hide the supplied contents. Null
   * when the server is unavailable or lacks the diagnostics method. Call on a
   * background thread.
   */
  @Nullable
  public List<FileDiagnostics> diagnostics(@NotNull DisplayContext context,
                                           @NotNull String filePath,
                                           @Nullable String contents) {
    Connected connected = connectFor(context, DisplayMethods.DIAGNOSTICS);
    if (connected == null) return null;
    try {
      prepareFile(connected, filePath, contents);
      List<FileDiagnostics> results = connected.client().diagnostics(connected.args(), filePath, contents);
      HaxeContextFailures.getInstance(project).record(context.containerId(), null);
      return results;
    } catch (DisplayRequestException e) {
      String failure = explainRequestFailure(connected, e);
      log.info("display/diagnostics failed: " + failure);
      HaxeContextFailures.getInstance(project).record(context.containerId(), failure);
      return null;
    }
  }

  /**
   * The reason a display request failed. An empty response usually means the
   * build context itself does not compile, and the JSON-RPC channel then
   * gives no reason. A plain compile of the same arguments does report the
   * errors, so this runs one (a define override can, for example, make a
   * library fail to compile).
   */
  @NotNull
  private static String explainRequestFailure(@NotNull Connected connected, @NotNull DisplayRequestException failure) {
    String message = StringUtil.notNullize(failure.getMessage());
    if (!failure.isUnexplained()) {
      return message;
    }
    try {
      DisplayResponse compiled = compileContext(connected);
      boolean explained = compiled.hasError() && !compiled.payload().isBlank();
      return explained ? contextCompileFailure(compiled) : message;
    } catch (DisplayRequestException probeFailure) {
      return message;
    }
  }

  /** The user-facing text for a context compile that reported errors. */
  @NotNull
  private static String contextCompileFailure(@NotNull DisplayResponse compiled) {
    return HaxeBundle.message("haxe.display.context.compile.failed", compiled.payload().strip());
  }

  /**
   * Diagnostics for the whole project. Only this request reports parse errors
   * in OTHER files; the per-file request stays silent about them. Uses the
   * saved state of every file. Null when the server is unavailable. Call on a
   * background thread.
   */
  @Nullable
  public List<FileDiagnostics> projectDiagnostics(@NotNull DisplayContext context) {
    Connected connected = connectFor(context, DisplayMethods.DIAGNOSTICS);
    if (connected == null) return null;
    try {
      return connected.client().projectDiagnostics(connected.args());
    } catch (DisplayRequestException e) {
      log.info("whole-project diagnostics failed: " + e.getMessage());
      return null;
    }
  }

  /**
   * Whether the file parses without errors (no {@code PsiErrorElement}). New
   * server requests are pointless while it does not: the compiler fails on
   * the same text, and the parser's own error highlighting already marks it.
   * Reading cached compiler results is still fine. Cached per PSI
   * modification; call in a read action.
   */
  public static boolean isSyntaxClean(@NotNull Project project, @NotNull VirtualFile file) {
    return !PsiErrorElementUtil.hasErrors(project, file);
  }

  /** A stable string identifying a context's argument source; the other compiler services use it as a cache key. */
  @NotNull
  static String contextKey(@NotNull DisplayContext context) {
    String base;
    if (context.args() != null) {
      base = String.join(" ", context.args());
    }
    else {
      LimeDisplaySpec lime = context.lime();
      base = lime != null ? lime.directory() + "/" + lime.fileName() + "@" + lime.targetFlag() : "?";
    }
    String overrides = context.overrides().signature();
    return overrides.isEmpty() ? base : base + "|" + overrides;
  }

  // --- args resolution ---

  @Nullable
  private List<String> resolveArgs(@NotNull DisplayContext context) {
    List<String> base;
    if (context.args() != null) {
      base = context.args();
    }
    else {
      LimeDisplaySpec lime = context.lime();
      base = lime != null ? limeArguments(lime, context.sdkName()) : null;
    }
    return base == null ? null : HaxeDisplayConfiguration.applyOverrides(base, context.overrides());
  }

  /** The lime-family context's arguments from the info service's display cache; null while the tool fails or the project is untrusted. */
  @Nullable
  private List<String> limeArguments(@NotNull LimeDisplaySpec spec, @Nullable String sdkName) {
    return HaxeLimeProjectInfoService.getInstance(project)
      .displayArguments(spec.directory(), spec.fileName(), spec.tool(), spec.targetFlag(), spec.fileStamp(), sdkName);
  }

  // --- server connection ---


  /**
   * A client for the context's server, starting the server when needed. Null
   * when the server is switched off or the project is not trusted, the
   * arguments cannot be resolved, the server does not start, or it does not
   * support {@code method}. The supported methods are asked once per server.
   * Background threads only.
   */
  @Nullable
  Connected connectFor(@NotNull DisplayContext context, @NotNull String method) {
    if (!mayUseServer()) return null;
    List<String> args = resolveArgs(context);
    if (args == null) return null;
    Endpoint endpoint = endpointFor(context);
    if (endpoint == null) return null;

    Capability known = capabilities.get(endpoint.id());
    if (known == null) {
      known = capabilityOf(endpoint, args, context.sdkName());
      if (known == null) return null;
      capabilities.put(endpoint.id(), known);
    }
    HaxeDisplayClient client = clientFor(endpoint, known.protocol());
    return known.methods().contains(method) ? new Connected(client, args, endpoint.id()) : null;
  }

  /**
   * What the endpoint's server answers on initialize, in the protocol its
   * haxe speaks; null when it does not answer. The handshake is not counted
   * in the server's request metrics: a fresh server refuses connections until
   * it has bound its port, and those retried attempts are no failures of it.
   */
  @Nullable
  private Capability capabilityOf(@NotNull Endpoint endpoint, @NotNull List<String> args, @Nullable String sdkName) {
    DisplayProtocol protocol = protocolOf(endpoint, sdkName);
    if (protocol == null) return null;
    HaxeDisplayClient client = handshakeClientFor(endpoint, protocol);
    InitializeResult initialized = withStartupRetry(endpoint.channel(), () -> client.initialize(args));
    if (initialized == null) return null;
    log.info(protocol + " display (haxe " + initialized.haxeVersion() + ") on port " + endpoint.id() + ", "
             + initialized.methods().size() + " methods");
    return new Capability(Set.copyOf(initialized.methods()), initialized.haxeVersion(), protocol);
  }

  /**
   * The protocol the endpoint's server speaks: the SDK's version tells, and
   * for an SDK of unknown version the server's own {@code -version} answer
   * does. Null when the server does not answer.
   */
  @Nullable
  private DisplayProtocol protocolOf(@NotNull Endpoint endpoint, @Nullable String sdkName) {
    HaxeLanguageLevel level = HaxeLanguageLevelUtil.fromSdkName(project, sdkName);
    if (level != null) return level.isAtLeast(HaxeLanguageLevel.HAXE_4_0) ? DisplayProtocol.JSON_RPC : DisplayProtocol.HAXE_3;
    DisplayChannel channel = endpoint.channel();
    return withStartupRetry(channel, () -> protocolOf(HaxeVersionProbe.versionOf(channel, VERSION_PROBE_TIMEOUT_MS)));
  }

  /** The protocol of a server whose {@code -version} answered the version; haxe 5 answers none and speaks JSON-RPC. */
  @NotNull
  private static DisplayProtocol protocolOf(@Nullable InitializeResult.SemVer version) {
    return version != null && version.major() < 4 ? DisplayProtocol.HAXE_3 : DisplayProtocol.JSON_RPC;
  }

  /** A client for the endpoint in the protocol, whose requests count toward the server's console metrics. */
  @NotNull
  private HaxeDisplayClient clientFor(@NotNull Endpoint endpoint, @NotNull DisplayProtocol protocol) {
    HaxeDisplayClient client = handshakeClientFor(endpoint, protocol);
    client.setObserver(HaxeServerMetrics.getInstance(project).observerFor(endpoint.metricsId()));
    return client;
  }

  /** A client for the endpoint in the protocol whose requests no metrics see. */
  @NotNull
  private static HaxeDisplayClient handshakeClientFor(@NotNull Endpoint endpoint, @NotNull DisplayProtocol protocol) {
    return switch (protocol) {
      case JSON_RPC -> new JsonRpcDisplayClient(endpoint.channel());
      case HAXE_3 -> new Haxe3DisplayClient(endpoint.channel());
    };
  }

  /**
   * Whether the server may be used at all: switched on, and the project
   * trusted. Checked before the arguments are resolved, not only in the
   * server managers, because resolving them can run project code
   * ({@code lime display} runs the project's tool). Package-private for tests.
   */
  boolean mayUseServer() {
    return HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled()
           && HaxeProjectTrust.checkForBackgroundEvaluation(project);
  }

  /**
   * The context's server instance ({@link HaxeServerAssignment}): its
   * dedicated display process when the setting asks for one and it runs,
   * else the compilation server. A display process that fails to start
   * falls back to the compilation server, so the feature stays available.
   */
  @Nullable
  private Endpoint endpointFor(@NotNull DisplayContext context) {
    HaxeServerAssignment assignment = HaxeServerAssignment.forContainer(project, context.containerId());
    HaxeServerGeneration generation = HaxeServerGenerations.of(project, context.sdkName());
    if (HaxeCompilerSettings.getInstance(project).usesDedicatedDisplayProcess(generation)) {
      HaxeDisplayProcessManager.Channel channel = HaxeDisplayProcessManager.getInstance(project).channelFor(assignment);
      if (channel != null) {
        return new Endpoint(channel.channel(), channel.id(), HaxeDisplayProcessManager.idFor(assignment));
      }
    }
    int port = HaxeCompilationServerManager.getInstance(project).ensureRunning(assignment);
    if (port <= 0) return null;
    DisplayChannel socket = HaxeDisplayTransport.socketPerRequest(HaxeCompilationServerManager.SERVER_HOST, port);
    return new Endpoint(socket, port, assignment.id());
  }

  /**
   * The answer to a first request on a server that may still be binding its
   * port: a connection failure is retried for a while. The compiler's own
   * answer and a broken framed channel do not change on a retry. Null when
   * the request failed.
   */
  @Nullable
  private static <T> T withStartupRetry(@NotNull DisplayChannel channel, @NotNull StartupRequest<T> request) {
    long deadline = System.currentTimeMillis() + STARTUP_RETRY_MS;
    while (true) {
      try {
        return request.send();
      } catch (DisplayRequestException e) {
        if (e.isCompilerAnswered() || channel.isBroken() || System.currentTimeMillis() > deadline) {
          log.info("display server request failed: " + e.getMessage());
          return null;
        }
        try {
          Thread.sleep(250);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return null;
        }
      }
    }
  }

  // --- context warm-up ---

  /**
   * Types the whole context once per session, because the server's module
   * cache fills only when every module is typed. Returns whether the context
   * typed without errors. A FAILED warm-up (broken code) is not remembered:
   * remembering it would keep module lookups broken until the caches are
   * purged, even after the code is fixed. The next call retries instead. The
   * outcome is recorded as the container's context failure, so a broken
   * build context shows in the tool window even while compiler diagnostics
   * are off. Background threads only.
   */
  boolean ensureContextCompiled(@NotNull Connected connected, @NotNull String contextKey, @NotNull String containerId) {
    if (compiledContexts.contains(contextKey)) return true;
    HaxeContextFailures failures = HaxeContextFailures.getInstance(project);
    try {
      String failure = typeContext(connected);
      if (failure != null) {
        log.info("context warm-up reported errors - will retry: " + failure);
        failures.record(containerId, failure);
        return false;
      }
      compiledContexts.add(contextKey);
      failures.record(containerId, null);
      return true;
    } catch (DisplayRequestException e) {
      log.info("context warm-up compile failed: " + e.getMessage());
      failures.record(containerId, StringUtil.notNullize(e.getMessage()));
      return false;
    }
  }

  /**
   * Types the connected context and returns the user-facing failure, or null
   * when it types. A project-wide {@code display/diagnostics} request types
   * every module like a compile but stops before generation, so build hooks
   * that run after it - a custom target's tool runs, a macro that expects a
   * main class - do not run for the IDE. A compiler without that method
   * (before 4.3.5) gets a {@code --no-output} compile, which does run them.
   */
  @Nullable
  private String typeContext(@NotNull Connected connected) throws DisplayRequestException {
    Capability known = capabilities.get(connected.port());
    if (known == null || !known.methods().contains(DisplayMethods.DIAGNOSTICS)) {
      DisplayResponse compiled = compileContext(connected);
      return compiled.hasError() ? contextCompileFailure(compiled) : null;
    }
    String errors = contextErrors(connected.client().projectDiagnostics(connected.args(), CONTEXT_COMPILE_TIMEOUT_MS));
    return errors.isEmpty() ? null : HaxeBundle.message("haxe.display.context.compile.failed", errors);
  }

  /** The error-severity diagnostics of a project-wide request, one {@code file:line: message} per line; warnings are left out. */
  @NotNull
  static String contextErrors(@NotNull List<FileDiagnostics> results) {
    List<String> lines = new ArrayList<>();
    for (FileDiagnostics fileDiagnostics : results) {
      for (Diagnostic diagnostic : fileDiagnostics.diagnostics()) {
        if (diagnostic.severity() != DiagnosticSeverity.ERROR) continue;
        int line = diagnostic.range().start().line() + 1;
        lines.add(fileDiagnostics.file() + ":" + line + ": " + HaxeDiagnosticsFetcher.messageOf(diagnostic));
      }
    }
    return String.join("\n", lines);
  }

  /** A plain {@code --no-output} compile of the connected context through the server. */
  @NotNull
  private static DisplayResponse compileContext(@NotNull Connected connected) throws DisplayRequestException {
    List<String> compileArgs = new ArrayList<>(connected.args());
    compileArgs.add(HxmlFileParser.NO_OUTPUT_FLAG);
    return connected.client().exchange(compileArgs, CONTEXT_COMPILE_TIMEOUT_MS);
  }

  /**
   * Readies the server for a request about one file, which sends the file's
   * unsaved {@code contents} or null for the saved file. The file is
   * invalidated first when the request sends text, since a cached module
   * would hide it, and when the server last received unsaved text for it,
   * since that module would otherwise keep answering for text the editor no
   * longer has ({@link HaxeServerUnsavedFiles}).
   */
  void prepareFile(@NotNull Connected connected, @NotNull String filePath, @Nullable String contents)
    throws DisplayRequestException {
    boolean sendsText = contents != null;
    if (unsavedFiles.needsInvalidate(connected.port(), filePath, sendsText)) {
      connected.client().invalidate(connected.args(), filePath);
    }
    unsavedFiles.requestSent(connected.port(), filePath, sendsText);
  }

  /**
   * Forgets which contexts were compiled, what each server answered on
   * initialize, which files' unsaved text each server holds and the cached
   * lime display arguments, so the next request initializes, evaluates and
   * compiles again.
   */
  public void clearCaches() {
    compiledContexts.clear();
    capabilities.clear();
    unsavedFiles.clear();
    HaxeLimeProjectInfoService.getInstance(project).clearDisplayArguments();
    contextCache.clear();
  }

  /** What a per-file request carries: the editor's unsaved text when there is one, and the offset as the server counts it. */
  public record FilePayload(@Nullable String contents, int serverOffset) {
  }

  /** The call of a per-file request, on a prepared connection. */
  @FunctionalInterface
  interface FileRequest<R> {
    R send(@NotNull Connected connected, @NotNull String filePath, @NotNull FilePayload payload) throws DisplayRequestException;
  }

  /** The payload of a request about the file at the offset. Call in a read action. */
  @NotNull
  static FilePayload payloadOf(@NotNull VirtualFile file, int offset) {
    String contents = unsavedContents(file);
    return new FilePayload(contents, HaxeServerOffsets.serverOffset(file, offset, contents));
  }

  /**
   * One per-file request: connects for the method, prepares the file on the
   * server for the payload's text ({@link #prepareFile}) and sends. Null when
   * there is no server or the request fails (logged). Background thread, no
   * read lock.
   */
  @Nullable
  <R> R send(@NotNull DisplayContext context,
             @NotNull String method,
             @NotNull String filePath,
             @NotNull FilePayload payload,
             @NotNull FileRequest<R> request) {
    Connected connected = connectFor(context, method);
    if (connected == null) return null;
    try {
      prepareFile(connected, filePath, payload.contents());
      return request.send(connected, filePath, payload);
    } catch (DisplayRequestException e) {
      log.info(method + " failed for " + filePath + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * The file on disk behind an element; a completion copy maps back to its
   * original, and a light file, such as a generated-code preview, has none.
   */
  @Nullable
  static VirtualFile physicalFileOf(@NotNull PsiElement element) {
    return HaxeDefineContextService.physicalFileOf(element.getContainingFile());
  }

  /**
   * The file's text in the editor when it has unsaved changes, else null. A
   * request sends it as {@code contents} after invalidating the file on the
   * server, since the compiler otherwise reads the saved file. Read action.
   */
  @Nullable
  static String unsavedContents(@NotNull VirtualFile file) {
    FileDocumentManager documents = FileDocumentManager.getInstance();
    Document document = documents.getCachedDocument(file);
    return document != null && documents.isDocumentUnsaved(document) ? document.getText() : null;
  }

}
