package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.display.protocol.DisplayMethods;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.server.TypeBlueprint;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.ide.hint.types.HaxeTypeHintAnchors;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalFunctionDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.util.HaxeExternalPaths;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlArguments;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.Connected;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerDisplayService.DisplayContext;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerHoverTypes.Scope;
import com.intellij.psi.PsiElement;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * The compilation server as the only source of type inlays. Field, parameter
 * and return types of class members come from one {@code typer/compiledTypes}
 * answer per build context, which lists every member of the program with its
 * position ({@link HaxeCompiledMembers}); everything declared inside a
 * function body - variables, captures, extracted values, loop variables,
 * local functions and their parameters - comes from
 * {@link HaxeCompilerHoverTypes}, one hover per declaration. The IDE's own
 * type inference is never consulted: a declaration the compiler has not
 * answered for shows nothing.
 * <p>
 * Reads are cache-only ({@link #typeTextOf}, read action); a miss queues the
 * work. A member answer describes what is on disk, because a compile reads
 * the files, so it is refreshed per context after a Haxe source was saved or
 * a build input changed, and a file with unsaved edits takes every one of its
 * declarations by hover with the editor text until it is saved. A server
 * without {@code typer/compiledTypes} (Haxe 3) hands every member to the
 * hovers as well. Each landed
 * answer restarts highlighting once, when the inlay pass re-collects. A
 * compiler error suspends the context until the next save or input change; a
 * transport failure is retried by the next lookup. The server-wide clear
 * ({@link HaxeCompilerCaches}) drops everything.
 * <p>
 * The provider set follows the Haxe Compiler settings: while type inlays are
 * a compiler feature there, {@link HaxeCompilerTypeInlayProvider} runs and
 * the IDE's type-hint providers stay silent.
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeCompilerInlayService implements Disposable {

  /** A context's members and the disk and build state they describe. */
  private record ContextMembers(long buildStamp, long savedStamp, @NotNull HaxeCompiledMembers members) {
  }

  /** The disk and build state under which a context's compile failed. */
  private record FailedState(long buildStamp, long savedStamp) {
  }

  private final Project project;
  private final Map<String, ContextMembers> membersByContext = new ConcurrentHashMap<>();
  private final Map<String, FailedState> failedContexts = new ConcurrentHashMap<>();
  private final Set<String> queuedContexts = ConcurrentHashMap.newKeySet();
  private final ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Haxe compiler inlays", 1);
  @Nullable private volatile Map<String, String> testTypeTexts;

  public HaxeCompilerInlayService(@NotNull Project project) {
    this.project = project;
  }

  public static HaxeCompilerInlayService getInstance(@NotNull Project project) {
    return project.getService(HaxeCompilerInlayService.class);
  }

  @Override
  public void dispose() {
    executor.shutdownNow();
  }

  /** Whether the compiler provides the type inlays: the settings say so and the compilation server is enabled. */
  public static boolean isActive(@NotNull Project project) {
    return HaxeCompilerSettings.getInstance(project).usesCompilerInlays()
           && HaxeBuildToolSettings.getInstance(project).isCompilationServerEnabled();
  }

  /** Whether {@link HaxeCompilerTypeInlayProvider} has anything to show: the compiler is the source, or a test seeded the texts. */
  boolean providesInlays() {
    return testTypeTexts != null || isActive(project);
  }

  /**
   * The inlay text for a declaration - the type of a variable, parameter or
   * field, the return type of a method or local function - or null while the
   * compiler has not answered for it. Read action; never waits.
   */
  @Nullable
  HaxeInlayTypeText typeTextOf(@NotNull PsiElement declaration) {
    if (!HaxeCompilerHoverTypes.covers(declaration)) return null;
    Map<String, String> seeded = testTypeTexts;
    if (seeded != null) {
      String text = seeded.get(nameOf(declaration));
      return text != null ? HaxeInlayTypeText.plain(text) : null;
    }
    if (!isActive(project)) return null;
    VirtualFile file = HaxeCompilerDisplayService.physicalFileOf(declaration);
    if (file == null || !HaxeCompilerFeatureAvailability.isAvailable(project, file)) return null;

    boolean membersByHover = isUnsaved(file) || lacksCompiledTypes(file);
    Scope hoverScope = membersByHover ? Scope.ALL : Scope.LOCALS;
    JsonTypeRef type = HaxeCompilerHoverTypes.isLocal(declaration) || membersByHover
                       ? HaxeCompilerHoverTypes.getInstance(project).wireTypeOf(declaration, hoverScope)
                       : memberTypeOf(file, declaration);
    return HaxeInlayTypeText.of(isFunction(declaration) ? returnTypeOf(type) : type);
  }

  private static boolean isFunction(@NotNull PsiElement declaration) {
    return declaration instanceof HaxeMethodDeclaration || declaration instanceof HaxeLocalFunctionDeclaration;
  }

  @Nullable
  private static JsonTypeRef returnTypeOf(@Nullable JsonTypeRef functionType) {
    return functionType == null ? null : functionType.returnType();
  }

  /** Whether the file's server cannot list the compiled members (Haxe 3); its members are hovered then. */
  private boolean lacksCompiledTypes(@NotNull VirtualFile file) {
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    DisplayContext context = displayService.contextFor(file);
    return context != null && displayService.lacksMethod(context, DisplayMethods.COMPILED_TYPES);
  }

  /** Whether the editor holds text for the file that is not on disk yet; a compile would read the older file. */
  private static boolean isUnsaved(@NotNull VirtualFile file) {
    return HaxeCompilerDisplayService.unsavedContents(file) != null;
  }

  // --- members from typer/compiledTypes ---

  /** The member's type from its context's answer, queuing a refresh when there is none or it is stale; a parameter's comes from its method. */
  @Nullable
  private JsonTypeRef memberTypeOf(@NotNull VirtualFile file, @NotNull PsiElement declaration) {
    DisplayContext context = HaxeCompilerDisplayService.getInstance(project).contextFor(file);
    if (context == null) return null;
    String key = HaxeCompilerDisplayService.contextKey(context);
    ContextMembers known = membersByContext.get(key);
    if (known == null || isStale(known)) {
      queueRefresh(key, context);
    }
    if (known == null) return null;

    HaxeComponentName name = HaxeTypeHintAnchors.componentNameOf(declaration);
    if (name == null) return null;
    String path = HaxeCompiledMembers.normalizePath(file.getPath(), "", SystemInfo.isFileSystemCaseSensitive);
    HaxeCompiledMembers.Member member = known.members().memberAt(path, name.getTextRange().getStartOffset());
    if (member == null) return null;
    return declaration instanceof HaxeParameter parameter ? member.type().parameterType(name.getText()) : member.type();
  }

  private boolean isStale(@NotNull ContextMembers known) {
    return known.buildStamp() != buildInputsStamp() || known.savedStamp() != savedSourcesStamp();
  }

  private void queueRefresh(@NotNull String key, @NotNull DisplayContext context) {
    // fixture tests have no server to ask; never spawn the attempt
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    FailedState failed = failedContexts.get(key);
    if (failed != null && failed.buildStamp() == buildInputsStamp() && failed.savedStamp() == savedSourcesStamp()) return;
    if (!queuedContexts.add(key)) return;
    executor.execute(() -> runRefresh(key, context));
  }

  private void runRefresh(@NotNull String key, @NotNull DisplayContext context) {
    boolean landed;
    try {
      landed = !project.isDisposed() && refresh(key, context);
    } finally {
      queuedContexts.remove(key);
    }
    if (landed) {
      HaxeCompilerCaches.dropPsiCachesAndRestartLater(project, "haxe: compiler member types landed");
    }
  }

  /**
   * One compiledTypes request for the context; whether the members' lookup
   * changed: an answer was stored, or the server turned out to have no such
   * request and the members go to the hovers. Background thread, no read lock.
   */
  private boolean refresh(@NotNull String key, @NotNull DisplayContext context) {
    // read BEFORE the request: a change during it leaves the answer stale-stamped
    long buildStamp = buildInputsStamp();
    long saved = savedSourcesStamp();
    HaxeCompilerDisplayService displayService = HaxeCompilerDisplayService.getInstance(project);
    Connected connected = displayService.connectFor(context, DisplayMethods.COMPILED_TYPES);
    if (connected == null) return displayService.lacksMethod(context, DisplayMethods.COMPILED_TYPES);
    List<String> args = new ArrayList<>(connected.args());
    args.add(HxmlFileParser.NO_OUTPUT_FLAG);
    long started = System.currentTimeMillis();
    try {
      List<TypeBlueprint> types = connected.client().compiledTypes(args, HaxeCompilerDisplayService.CONTEXT_COMPILE_TIMEOUT_MS);
      HaxeCompiledMembers members =
        HaxeCompiledMembers.of(types, workDirectoryOf(connected.args()), SystemInfo.isFileSystemCaseSensitive, HaxeCompilerInlayService::fileBytes);
      membersByContext.put(key, new ContextMembers(buildStamp, saved, members));
      failedContexts.remove(key);
      if (log.isDebugEnabled()) {
        log.debug("compiler inlays: " + members.memberCount() + " members in " + members.fileCount() + " files, "
                  + (System.currentTimeMillis() - started) + " ms, context " + StringUtil.first(key, 80, true));
      }
      return true;
    } catch (DisplayRequestException e) {
      log.info("typer/compiledTypes failed: " + e.getMessage());
      if (e.isCompilerAnswered()) failedContexts.put(key, new FailedState(buildStamp, saved));
      return false;
    }
  }

  private long buildInputsStamp() {
    return HaxeCompilerDisplayService.getInstance(project).buildInputsStamp();
  }

  /** Moves when a Haxe source on disk changes: the member answers describe older files then. */
  private long savedSourcesStamp() {
    return HaxeSavedSourcesStamp.getInstance(project).get();
  }

  /** The directory the compile runs in (every {@code --cwd} composed), which relative file positions are from; the project directory without one. */
  @NotNull
  private String workDirectoryOf(@NotNull List<String> args) {
    Path workDirectory = HxmlArguments.workDirectory(args);
    return workDirectory != null ? workDirectory.toString() : StringUtil.notNullize(project.getBasePath());
  }

  @Nullable
  private static byte[] fileBytes(@NotNull String path) {
    if (HaxeExternalPaths.absolutePathOf(path) == null) return null;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    if (file == null) return null;
    try {
      return file.contentsToByteArray();
    } catch (IOException e) {
      return null;
    }
  }

  /** The declaration's name text, or null when it has none. */
  @Nullable
  private static String nameOf(@NotNull PsiElement declaration) {
    HaxeComponentName name = HaxeTypeHintAnchors.componentNameOf(declaration);
    return name != null ? name.getText() : null;
  }

  public void clearCaches() {
    membersByContext.clear();
    failedContexts.clear();
  }

  /** Replaces every lookup with a declaration-name-to-text table, so provider tests run without a server; null restores the compiler. */
  @TestOnly
  public void useTestTypeTexts(@Nullable Map<String, String> byDeclarationName) {
    testTypeTexts = byDeclarationName;
  }
}
