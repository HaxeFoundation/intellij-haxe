package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeModuleVariants;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.settings.*;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.SemVer;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression.FLAG_DEFINE_VALUE;

/**
 * The IDE's conditional-compilation define context, derived from the v2 build
 * configuration: the project's ACTIVE build file's defines (via `lime display`
 * for xml projects — conditionals evaluated, toolchain defines included)
 * overlaid with the owning container's IDE Define overrides. Feeds
 * {@code HaxeDefineDetectionManager.getAllDefinitions()}, i.e. the parsing and
 * indexing of every haxe file — which is why a context change re-indexes the
 * files with conditional compilation ({@link HaxeDefineContextInvalidator}).
 */
@Service(Service.Level.PROJECT)
@CustomLog
public final class HaxeDefineContextService implements Disposable, HaxeBuildSettingsListener {

  /**
   * Sentinel for {@link #lastComputed}: no consumer has been handed a define
   * context yet, so nothing was parsed against one and no reparse is owed.
   * Identity-compared, never handed out.
   */
  private static final Map<String, String> NEVER_HANDED_OUT = new LinkedHashMap<>();

  /** Every input of a computed context; a mismatch invalidates the cached one. */
  private record ContextKey(@NotNull String path, @NotNull HaxeBuildFileType type, long contentStamp,
                            @Nullable String targetId, @Nullable String sdkName, @Nullable String customTarget,
                            @NotNull String compilerVersion, @NotNull List<EnvironmentDefine> overrides) {
  }

  private record Snapshot(@NotNull ContextKey key, @NotNull Map<String, String> defines) {
  }

  /**
   * Lock-free identity of every input reachable without VFS or read-action
   * work: the active path and the file's content stamp. A match
   * short-circuits the whole computation - this runs per candidate inside
   * index lookups, so the fast path must not touch findFileByPath or take a
   * read action.
   */
  private record FastState(@NotNull String path, @NotNull VirtualFile file, long contentStamp,
                           @Nullable Map<String, String> defines) {
  }

  private final Project project;
  private volatile Snapshot snapshot;
  private volatile FastState fastState;
  /** The defines most recently handed to a consumer — what current PSI state was parsed against. */
  private volatile Map<String, String> lastComputed = NEVER_HANDED_OUT;
  private final AtomicBoolean refreshQueued = new AtomicBoolean();
  private final Object refreshLock = new Object();

  // the implicit single-file fallback behind HaxeKnownBuildFiles sweeps every
  // module under the read lock - far too heavy for getActiveDefines, which
  // runs per candidate inside index lookups. Resolved once per build-settings
  // change instead of per call (a module set change without a settings event
  // keeps the stale answer until the next one).
  private volatile String effectiveActivePath;
  private volatile boolean effectiveActivePathComputed;
  private volatile String activeContainerId;
  private volatile boolean activeContainerIdComputed;

  public HaxeDefineContextService(@NotNull Project project) {
    this.project = project;
    // config changes (language level, compiler settings) feed haxe_ver, so they invalidate the context too
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(HaxeBuildSettingsListener.TOPIC, this);
    connection.subscribe(HaxeBuildConfigListener.TOPIC, (HaxeBuildConfigListener)this::buildSettingsChanged);
  }

  public static HaxeDefineContextService getInstance(@NotNull Project project) {
    return project.getService(HaxeDefineContextService.class);
  }

  /**
   * Drops the derived context and recomputes in the background — a settings
   * mutation must reach parsing (re-index) and highlighting WITHOUT relying on
   * the tool window being open to call {@link #refreshAsync()}.
   */
  @Override
  public void buildSettingsChanged() {
    snapshot = null;
    fastState = null;
    effectiveActivePathComputed = false;
    activeContainerIdComputed = false;
    refreshAsync();
  }

  /**
   * The current define context, or null when the project has no v2 active build
   * file (legacy detection applies then). Cached; recomputed when the build
   * file, target selection or environment overrides change.
   */
  @Nullable
  public Map<String, String> getActiveDefines() {
    String path = effectiveActivePath();
    if (StringUtil.isEmptyOrSpaces(path)) {
      // record the null handout: files parsed now use the LEGACY define
      // context, and activating a build file later must trigger a reparse
      lastComputed = null;
      return null;
    }

    FastState fast = fastState;
    boolean fastHit = fast != null
                      && fast.path().equals(path)
                      && fast.file().isValid()
                      && contentStamp(fast.file()) == fast.contentStamp();
    if (fastHit) {
      return fast.defines();
    }

    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    if (file == null || !file.isValid()) {
      lastComputed = null;
      return null;
    }

    Map<String, String> result = HaxeReadActions.compute(() -> cachedOrComputedDefines(file));
    fastState = new FastState(path, file, contentStamp(file), result);
    lastComputed = result;
    return result;
  }

  /**
   * Recomputes the context in the background and, when it actually changed,
   * has {@link HaxeDefineContextInvalidator} re-index the conditional haxe
   * files. Cheap when nothing changed — safe to call from every tree
   * refresh; requests arriving while one is queued coalesce into it.
   */
  public void refreshAsync() {
    if (!refreshQueued.compareAndSet(false, true)) return;
    AppExecutorUtil.getAppExecutorService().execute(this::refreshNow);
  }

  /**
   * Whether the ACTIVE build context defines the name before environment
   * overrides apply — the define quickfix uses this to decide between merely
   * dropping its own override and masking a build-file define with a REMOVE
   * entry. False when no v2 active build file is configured.
   */
  public boolean isDefinedWithoutOverrides(@NotNull String name) {
    VirtualFile file = activeBuildFile();
    if (file == null) return false;
    return HaxeReadActions.compute(() -> {
      HaxeBuildFileType type = HaxeBuildFileScanner.detectType(project, file);
      if (type == null) return false;
      return baseDefines(file, type).containsKey(name);
    });
  }

  /**
   * The container owning the ACTIVE build file, or null without one — where
   * define overrides belong, and the container library/SDK elements resolve
   * their language level against. Cached (the language level lookup calls
   * this per annotated element); callers hold the read lock.
   */
  @Nullable
  public String activeContainerId() {
    if (!activeContainerIdComputed) {
      VirtualFile file = activeBuildFile();
      activeContainerId = file == null ? null : HaxeContainers.containerIdFor(project, file);
      activeContainerIdComputed = true;
    }
    return activeContainerId;
  }

  @Nullable
  private String effectiveActivePath() {
    if (!effectiveActivePathComputed) {
      effectiveActivePath = HaxeReadActions.compute(() -> HaxeKnownBuildFiles.effectiveActivePath(project));
      effectiveActivePathComputed = true;
    }
    return effectiveActivePath;
  }

  @Nullable
  private VirtualFile activeBuildFile() {
    String path = effectiveActivePath();
    if (StringUtil.isEmptyOrSpaces(path)) return null;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    return file != null && file.isValid() ? file : null;
  }

  private static long contentStamp(@NotNull VirtualFile file) {
    Document document = FileDocumentManager.getInstance().getCachedDocument(file);
    return document != null ? document.getModificationStamp() : file.getModificationStamp();
  }

  private void refreshNow() {
    if (project.isDisposed()) return;
    synchronized (refreshLock) {
      // cleared under the lock: requests arriving during this refresh fold into ONE follow-up
      refreshQueued.set(false);
      // compare against the defines last handed to consumers, NOT the snapshot
      // cache: the invalidation topic clears the snapshot synchronously before
      // this runs. The sentinel keeps project open cheap (nothing was handed
      // out, nothing to re-index) while a recorded null handout still pushes
      // on the null -> non-null transition (build file activated after files
      // were parsed against the legacy context).
      Map<String, String> before = lastComputed;
      snapshot = null;
      fastState = null;
      // one non-blocking read action around the recompute: this is a pooled
      // thread, and the inner blocking read forms then nest instead of freezing the UI
      Map<String, String> after = ReadAction.nonBlocking(this::getActiveDefines)
        .expireWith(this)
        .executeSynchronously();
      lastComputed = after;
      boolean changed = before != NEVER_HANDED_OUT && !Objects.equals(before, after);
      log.debug("define context refresh: changed=" + changed + ", defines=" + (after == null ? "legacy" : after.size()));
      if (changed) {
        HaxeDefineContextInvalidator.invalidateConditionalFiles(project);
      }
    }
  }

  /** The snapshot's defines while its inputs are unchanged, else a fresh computation. Call in a read action. */
  @Nullable
  private Map<String, String> cachedOrComputedDefines(@NotNull VirtualFile file) {
    HaxeBuildFileType type = HaxeBuildFileScanner.detectType(project, file);
    if (type == null) return null;

    ContextKey key = contextKey(file, type);
    Snapshot current = snapshot;
    if (current != null && current.key().equals(key)) {
      return current.defines();
    }
    Map<String, String> defines = compute(file, type);
    snapshot = new Snapshot(key, defines);
    return defines;
  }

  @NotNull
  private ContextKey contextKey(@NotNull VirtualFile file, @NotNull HaxeBuildFileType type) {
    String containerId = HaxeContainers.containerIdFor(project, file);
    HaxeEnvironmentStore environment = HaxeEnvironmentStore.getInstance(project);
    String targetId = HaxeTargetSelectionStore.getInstance(project).getSelectedTargetId(file);
    String sdkName = environment.getSdkName(containerId);
    String customTarget = environment.getCustomTarget(containerId);
    String compilerVersion = HaxeLanguageLevelUtil.getHaxeVersion(project, containerId);
    List<EnvironmentDefine> overrides = environment.getDefines(containerId);
    return new ContextKey(file.getPath(), type, contentStamp(file), targetId, sdkName, customTarget, compilerVersion, overrides);
  }

  @NotNull
  private Map<String, String> compute(@NotNull VirtualFile file, @NotNull HaxeBuildFileType type) {
    Map<String, String> defines = baseDefines(file, type);

    String containerId = HaxeContainers.containerIdFor(project, file);
    putCompilerIdentityDefines(defines, containerId);
    putHashlinkVersionDefine(defines, containerId);
    putCustomTargetDefines(defines, containerId);

    for (EnvironmentDefine override : HaxeEnvironmentStore.getInstance(project).getDefines(containerId)) {
      if (override.effect() == DefineEffect.REMOVE) {
        defines.remove(override.name());
      } else {
        defines.put(override.name(), override.value().isEmpty() ? FLAG_DEFINE_VALUE : override.value());
      }
    }
    return defines;
  }

  /** The build context's defines BEFORE the container's environment overrides apply. */
  @NotNull
  private Map<String, String> baseDefines(@NotNull VirtualFile file, @NotNull HaxeBuildFileType type) {
    HaxeBuildFile buildFile = new HaxeBuildFile(file, type);
    HaxeBuildFileInfo info = effectiveInfo(buildFile);

    Map<String, String> defines = new LinkedHashMap<>();
    for (HaxeBuildFileInfo.HaxeDefine define : info.defines()) {
      defines.put(define.name(), define.value() != null ? define.value() : FLAG_DEFINE_VALUE);
    }
    // the compiler implicitly defines the target (hl, js, sys, ...) - the std
    // library's per-target sources are gated on exactly these
    if (info.target() != null) {
      info.target().getDefinitions().forEach(definition -> defines.putIfAbsent(definition, FLAG_DEFINE_VALUE));
    }
    return defines;
  }

  /**
   * The compiler's identity defines ({@code haxe_ver}, {@code haxe} and the
   * major-version flags), mirrored so `#if (haxe_ver >= 4.1)` blocks activate
   * correctly. Sourced from the language level or the container's compiler
   * version per the compiler settings; wins over a build-tool-reported value
   * (the point of choosing the level), while the container's explicit Define
   * overrides still apply on top.
   */
  private void putCompilerIdentityDefines(@NotNull Map<String, String> defines, @NotNull String containerId) {
    String version = HaxeLanguageLevelUtil.getHaxeVersion(project, containerId);
    defines.put("haxe_ver", version);
    defines.put("haxe", version);
    SemVer semVer = SemVer.parseFromText(version);
    if (semVer != null) {
      if (semVer.getMajor() >= 3) defines.put("haxe3", version);
      if (semVer.getMajor() >= 4) defines.put("haxe4", version);
      if (semVer.getMajor() >= 5) defines.put("haxe5", version);
    }
  }

  /**
   * The container's Custom target setting, mirrored as the defines
   * {@code --custom-target} sets: {@code custom_target} and
   * {@code target.name=<name>} (never the bare name — a custom target does
   * NOT define itself the way stock targets do). Wins over an hxml-declared
   * custom target, which arrives through the build file's defines; the
   * container's explicit Define overrides still apply on top.
   */
  private void putCustomTargetDefines(@NotNull Map<String, String> defines, @NotNull String containerId) {
    String customTarget = HaxeEnvironmentStore.getInstance(project).getCustomTarget(containerId);
    if (customTarget == null) return;
    defines.put(HaxeModuleVariants.CUSTOM_TARGET_DEFINE, FLAG_DEFINE_VALUE);
    defines.put(HaxeModuleVariants.TARGET_NAME_DEFINE, customTarget);
  }

  /**
   * The compiler defaults {@code hl_ver} by reading {@code std/hl/hl_version}
   * from its standard library when the define is absent; mirror that so
   * {@code #if (hl_ver >= version("1.12.0"))} blocks activate correctly out
   * of the box. Synthesized for EVERY target - the value is a static property
   * of the configured toolchain, target membership is what {@code #if hl}
   * answers, and a target-dependent define would flicker with the selection.
   * A build-file {@code -D hl-ver} keeps precedence (absence-guarded), and
   * the container's environment overrides still apply on top.
   */
  private void putHashlinkVersionDefine(@NotNull Map<String, String> defines, @NotNull String containerId) {
    if (defines.containsKey("hl_ver") || defines.containsKey("hl-ver")) return;
    String version = readStdHlVersion(containerId);
    if (version != null) {
      defines.put("hl_ver", version);
    }
  }

  @Nullable
  private String readStdHlVersion(@NotNull String containerId) {
    Sdk sdk = HaxeToolPathResolver.resolveSdk(project, containerId);
    if (sdk == null) return null;
    for (VirtualFile root : sdk.getRootProvider().getFiles(OrderRootType.SOURCES)) {
      VirtualFile versionFile = root.findFileByRelativePath("hl/hl_version");
      if (versionFile == null) continue;
      try {
        String version = VfsUtilCore.loadText(versionFile).trim();
        return version.isEmpty() ? null : version;
      }
      catch (IOException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * The build file's parsed info; lime-family files use the selected target's
   * `lime display` result when available (raw xml defines as the fallback until
   * the background run lands, which then re-triggers a refresh).
   */
  @NotNull
  private HaxeBuildFileInfo effectiveInfo(@NotNull HaxeBuildFile buildFile) {
    HaxeBuildFileInfo raw = HaxeBuildSections.inspectSelected(project, buildFile);
    HaxeBuildFileType type = buildFile.type();
    if (!LimeProjects.isLimeFamily(type)) return raw;

    String targetFlag = LimeProjects.selectedTargetFlag(project, type, buildFile.file());
    String containerId = HaxeContainers.containerIdFor(project, buildFile.file());
    String environmentSdk = HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
    HaxeBuildFileInfo display = HaxeLimeProjectInfoService.getInstance(project)
      .getCachedOrSchedule(buildFile, targetFlag, environmentSdk, this::refreshAsync);
    return display != null ? display : raw;
  }

  @Override
  public void dispose() {
  }
}
