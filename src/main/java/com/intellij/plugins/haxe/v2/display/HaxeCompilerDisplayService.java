package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.v2.buildtools.server.HaxeServerMetrics;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeNmeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.server.HaxeContextFailures;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.client.HaxeDisplayClient;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.FileDiagnostics;
import com.intellij.plugins.haxe.display.protocol.InitializeResult;
import com.intellij.plugins.haxe.display.protocol.server.ServerMemory;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import com.intellij.plugins.haxe.display.transport.HaxeDisplayTransport;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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
public final class HaxeCompilerDisplayService {

  /**
   * The part of a display request computed under the read lock. Either
   * {@code args} is known already (HXML, NMML), or {@code lime} still has to
   * run on a background thread. The container's define overrides are applied
   * to whichever argument list results.
   */
  public record DisplayContext(@Nullable List<String> args,
                               @Nullable LimeDisplaySpec lime,
                               @Nullable String sdkName,
                               @NotNull HaxeDisplayConfiguration.DefineOverrides overrides,
                               @NotNull String containerId) {
  }

  /** The inputs of a {@code lime display} run, captured under the read lock and run later in the background. */
  public record LimeDisplaySpec(@NotNull String directory,
                                @NotNull String fileName,
                                @NotNull String tool,
                                @NotNull String targetFlag,
                                long fileStamp) {
  }

  /** The methods and haxe version the running server reported; asked again when the port changes. */
  private record Capability(int port, @NotNull Set<String> methods,
                            @Nullable InitializeResult.SemVer haxeVersion) {
  }

  /** A client for a running server that supports the requested method, with the resolved arguments. */
  record Connected(@NotNull HaxeDisplayClient client, @NotNull List<String> args, int port) {
  }

  private record CachedLimeArgs(long fileStamp, @NotNull String targetFlag, @NotNull List<String> args) {
  }

  private static final int LIME_DISPLAY_TIMEOUT_MS = 60_000;
  private static final int CONTEXT_COMPILE_TIMEOUT_MS = 120_000;

  private final Project project;
  private final Map<String, CachedLimeArgs> limeArgsCache = new ConcurrentHashMap<>();
  /** Keys of the contexts compiled this session; the server's module cache fills only from a compile. */
  private final Set<String> compiledContexts = ConcurrentHashMap.newKeySet();
  private volatile Capability capability;

  public HaxeCompilerDisplayService(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  public static HaxeCompilerDisplayService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerDisplayService.class);
  }

  @NotNull
  public Project getProject() {
    return project;
  }

  /** The connected server's haxe version; null before the first successful initialize. */
  @Nullable
  public InitializeResult.SemVer connectedHaxeVersion() {
    Capability known = capability;
    return known != null ? known.haxeVersion() : null;
  }

  /**
   * The display context of a haxe source file, derived from its module's
   * current build file. Null when the file has no supported build context.
   * Call in a read action; does not touch the network or spawn processes.
   */
  @Nullable
  public DisplayContext contextFor(@NotNull VirtualFile file) {
    Module module = ModuleUtilCore.findModuleForFile(file, project);
    return module == null ? null : contextFor(module);
  }

  /**
   * The display context of a module's current build file, for callers
   * without a source file (the type catalog works per module). Same contract
   * as {@link #contextFor(VirtualFile)}.
   */
  @Nullable
  public DisplayContext contextFor(@NotNull Module module) {
    VirtualFile buildFile = currentBuildFile(module);
    if (buildFile == null || buildFile.getParent() == null) return null;
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
    HaxeDisplayConfiguration.DefineOverrides overrides = HaxeDisplayConfiguration.overridesFor(project, containerId);

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
    List<String> sectionArguments = HaxeBuildSections.selectedSectionArguments(project, buildFile);
    if (sectionArguments == null) {
      return List.of("--cwd", directory, HaxeBuildWorkDirectories.fileArgument(buildFile, directory));
    }
    List<String> args = new ArrayList<>(List.of("--cwd", directory));
    args.addAll(sectionArguments);
    return List.copyOf(args);
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
                                    @NotNull HaxeDisplayConfiguration.DefineOverrides overrides,
                                    @NotNull String containerId) {
    String targetFlag = NmeProjects.selectedTargetFlag(project, buildFile);
    HaxeNmeProjectInfoService.Evaluation evaluation = HaxeNmeProjectInfoService.getInstance(project)
      .getCachedOrSchedule(new HaxeBuildFile(buildFile, HaxeBuildFileType.NMML), targetFlag, sdkName, () -> { });
    if (evaluation == null) return null;

    List<String> args = new ArrayList<>(List.of("--cwd", directory));
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
      if (contents != null) {
        connected.client().invalidate(connected.args(), filePath);
      }
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
    if (!message.endsWith("empty response")) {
      return message;
    }
    try {
      DisplayResponse compiled = compileContext(connected);
      String errors = compiled.hasError() ? compiled.payload().strip() : "";
      return errors.isEmpty() ? message
                              : HaxeBundle.message("haxe.display.context.compile.failed", errors);
    } catch (DisplayRequestException probeFailure) {
      return message;
    }
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
      base = lime != null ? limeArgsFor(lime, context.sdkName()) : null;
    }
    return base == null ? null : HaxeDisplayConfiguration.applyOverrides(base, context.overrides());
  }

  // TODO: keyed on the project file's stamp only - an edited library
  //  include.xml does not invalidate this cache.
  @Nullable
  private List<String> limeArgsFor(@NotNull LimeDisplaySpec spec, @Nullable String sdkName) {
    String cacheKey = spec.directory() + "/" + spec.fileName();
    CachedLimeArgs cached = limeArgsCache.get(cacheKey);
    if (cached != null && cached.fileStamp() == spec.fileStamp() && cached.targetFlag().equals(spec.targetFlag())) {
      return cached.args();
    }
    List<String> args = runLimeDisplay(spec, sdkName);
    if (args != null) {
      limeArgsCache.put(cacheKey, new CachedLimeArgs(spec.fileStamp(), spec.targetFlag(), args));
    }
    return args;
  }

  @Nullable
  private List<String> runLimeDisplay(@NotNull LimeDisplaySpec spec, @Nullable String sdkName) {
    String haxelib = HaxeToolPathResolver.resolveHaxelibExecutable(project, sdkName);
    List<String> args = LimeProjects.displayArguments(
      haxelib, spec.tool(), spec.directory(), spec.fileName(), spec.targetFlag(), LIME_DISPLAY_TIMEOUT_MS);
    if (args == null) {
      log.info("lime display failed for " + spec.fileName());
    }
    return args;
  }

  // --- server connection ---

  /**
   * Memory statistics of the compilation server already RUNNING on {@code port}.
   * Unlike the context-based requests, {@code server/memory} needs no build
   * context: it reports per-context sizes for whatever the server has
   * compiled. Performs a socket round-trip; call on a background thread.
   */
  @NotNull
  public static ServerMemory fetchServerMemory(int port) throws DisplayRequestException {
    return new HaxeDisplayClient(HaxeCompilationServerManager.SERVER_HOST, port).serverMemory(List.of());
  }

  /**
   * A client for the context's server, starting the server when needed. Null
   * when the arguments cannot be resolved, the server does not start, or it
   * does not support {@code method}. The supported methods are asked once
   * per server. Background threads only.
   */
  @Nullable
  Connected connectFor(@NotNull DisplayContext context, @NotNull String method) {
    List<String> args = resolveArgs(context);
    if (args == null) return null;
    int port = HaxeCompilationServerManager.getInstance(project).ensureRunning(context.sdkName());
    if (port <= 0) return null;
    HaxeDisplayClient client = new HaxeDisplayClient(HaxeCompilationServerManager.SERVER_HOST, port);
    String serverId = HaxeToolPathResolver.resolveHaxeExecutable(project, context.sdkName());
    client.setObserver((requestMethod, millis, success) ->
                         HaxeServerMetrics.getInstance(project).record(serverId, millis, success));

    Capability known = capability;
    if (known == null || known.port() != port) {
      InitializeResult initialized = initializeWithRetry(client, args);
      if (initialized == null) return null;
      known = new Capability(port, Set.copyOf(initialized.methods()), initialized.haxeVersion());
      log.info("haxe display protocol " + initialized.protocolVersion()
               + " (haxe " + initialized.haxeVersion() + "), "
               + known.methods().size() + " methods");
      capability = known;
    }
    return known.methods().contains(method) ? new Connected(client, args, port) : null;
  }

  /** A freshly spawned server needs a moment to bind its port; retry briefly before giving up. */
  @Nullable
  private static InitializeResult initializeWithRetry(@NotNull HaxeDisplayClient client, @NotNull List<String> args) {
    long deadline = System.currentTimeMillis() + 10_000;
    while (true) {
      try {
        return client.initialize(args);
      } catch (DisplayRequestException e) {
        if (System.currentTimeMillis() > deadline) {
          log.info("display initialize failed: " + e.getMessage());
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
   * Compiles the context once per session, because the server's module cache
   * fills only from a real compile. Returns whether the context compiled. A
   * FAILED compile (broken code) is not remembered: remembering it would keep
   * module lookups broken until the caches are purged, even after the code is
   * fixed. The next call retries instead. Background threads only.
   */
  boolean ensureContextCompiled(@NotNull Connected connected, @NotNull String contextKey) {
    if (compiledContexts.contains(contextKey)) return true;
    try {
      DisplayResponse compiled = compileContext(connected);
      if (compiled.hasError()) {
        log.info("context warm-up compile reported errors - will retry: " + compiled.payload());
        return false;
      }
      compiledContexts.add(contextKey);
      return true;
    } catch (DisplayRequestException e) {
      log.info("context warm-up compile failed: " + e.getMessage());
      return false;
    }
  }

  /** A plain {@code --no-output} compile of the connected context through the server. */
  @NotNull
  private static DisplayResponse compileContext(@NotNull Connected connected) throws DisplayRequestException {
    List<String> compileArgs = new ArrayList<>(connected.args());
    compileArgs.add("--no-output");
    return HaxeDisplayTransport.request(HaxeCompilationServerManager.SERVER_HOST, connected.port(), compileArgs,
                                        CONTEXT_COMPILE_TIMEOUT_MS);
  }

  /** Forgets which contexts were compiled, so the next module lookup compiles again. */
  public void resetCompiledContexts() {
    compiledContexts.clear();
  }

  /** The file on disk behind an element; a completion copy maps back to its original. */
  @Nullable
  static VirtualFile physicalFileOf(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file != null ? file.getOriginalFile().getVirtualFile() : null;
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

  // --- build file resolution ---

  /**
   * The module's current build file: the project's active file when this
   * module owns it, else the module's Build command file.
   */
  @Nullable
  private VirtualFile currentBuildFile(@NotNull Module module) {
    VirtualFile active = fileIfOwned(HaxeKnownBuildFiles.effectiveActivePath(project), module);
    if (active != null) return active;
    HaxeEnvironmentStore.CompileCommand command =
      HaxeEnvironmentStore.getInstance(project).getCompileCommand(module.getName());
    return command != null ? fileIfOwned(command.buildFilePath(), module) : null;
  }

  @Nullable
  private VirtualFile fileIfOwned(@Nullable String path, @NotNull Module module) {
    if (path == null || path.isBlank()) return null;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    if (file == null || !file.isValid()) return null;
    return HaxeContainers.containerIdFor(project, file).equals(module.getName()) ? file : null;
  }
}
