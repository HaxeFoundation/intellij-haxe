package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.haxelib.*;
import com.intellij.plugins.haxe.haxelib.HaxelibRepositoryLayout;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildtools.*;
import com.intellij.plugins.haxe.v2.buildtools.context.HaxeResolveContexts;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Library;
import com.intellij.plugins.haxe.v2.buildtools.classpaths.HaxeExternalClasspathStore;
import com.intellij.plugins.haxe.v2.buildtools.classpaths.HaxeExternalClasspaths;
import com.intellij.plugins.haxe.v2.buildtools.classpaths.HaxeExternalClasspaths.ExternalClasspath;
import java.nio.file.Path;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeEffectiveBuildFileInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibPathParser.LibrarySection;
import com.intellij.plugins.haxe.v2.migration.HaxeV1Migrator;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.config.HaxeTarget.FLAG_DEFINE_VALUE;

/**
 * Attaches each module's haxelib dependencies as module-level libraries, so they
 * appear under External Libraries and feed resolve scopes. The dependency set
 * is the union of the module's build files: every file resolves under its own
 * build ({@link HaxeResolveContexts}), so each build's libraries must be
 * attached, whichever file is active. The define context stays per build -
 * it counts only the libraries a file's build declares. Classpaths come from
 * `haxelib path`, which includes transitive dependencies; it runs from the
 * declaring build's work directory, so a build-local {@code .haxelib}
 * repository counts. The same run collects each module's build-file
 * classpaths outside its own sources ({@link HaxeExternalClasspaths}).
 * Managed entries carry a name prefix so the sync can remove stale ones
 * without touching user-defined libraries.
 */
// TODO: a file resolves against the libraries of every build of its module; a per-context library scope would limit it to its own build's
@CustomLog
public final class HaxeLibrarySync {

  public static final String MANAGED_PREFIX = "haxelib: ";

  /** The toolchain libraries hxp build scripts import from. */
  private static final List<String> HXP_TOOLCHAIN_LIBRARIES = List.of("hxp", "lime");

  /** A library a module's build declares, with the directory haxelib resolves it from: the declaring build's work directory. */
  public record DeclaredDependency(@NotNull HaxeLibDependency dependency, @Nullable Path workDirectory) {
  }

  private HaxeLibrarySync() {
  }

  @NotNull
  public static String managedLibraryName(@NotNull String libraryName) {
    return MANAGED_PREFIX + libraryName;
  }

  /**
   * Whether a synced library-table entry belongs to the given lib. Entries
   * carry the resolved version as a suffix ("haxelib: openfl 9.5.0"), so an
   * exact-name lookup would miss them.
   */
  public static boolean isManagedEntryFor(@Nullable String entryName, @NotNull String libraryName) {
    if (entryName == null) return false;
    String base = managedLibraryName(libraryName);
    return entryName.equals(base) || entryName.startsWith(base + " ");
  }

  /** The lib a synced entry name stands for ({@code haxelib: name} or {@code haxelib: name 1.2.3}); null for an unmanaged entry. */
  @Nullable
  public static String haxelibNameOf(@Nullable String entryName) {
    if (entryName == null || !entryName.startsWith(MANAGED_PREFIX)) return null;
    String rest = entryName.substring(MANAGED_PREFIX.length());
    int versionStart = rest.indexOf(' ');
    return versionStart < 0 ? rest : rest.substring(0, versionStart);
  }

  /**
   * One module's {@code haxelib path} results: the classpaths per External
   * Libraries entry, per dependency the {@code -D name=version} defines the
   * output carried (the dependency and its transitives), every library the
   * output listed with what its haxelib.json requires, and per dependency
   * the names the output listed in order (the closure).
   */
  private record Resolution(@NotNull Map<String, List<String>> classpaths,
                            @NotNull Map<String, Map<String, String>> definesByDependency,
                            @NotNull Map<String, Library> libraries,
                            @NotNull Map<String, List<String>> closureByDependency) {
  }

  /** Everything a sync run records besides the workspace libraries, keyed by module name where per container. */
  private record SyncResult(@NotNull Map<String, Map<String, Map<String, String>>> definesByModule,
                            @NotNull Map<String, Library> libraries,
                            @NotNull Map<String, Map<String, List<String>>> closuresByModule) {
  }

  /** One run's inputs, read in one read action: per module its declared libraries, and per module name its external classpaths. */
  record Collected(@NotNull Map<Module, List<DeclaredDependency>> dependencies,
                   @NotNull Map<String, List<ExternalClasspath>> externalClasspaths) {
  }

  /**
   * Requests a background sync of every module's libraries. Syncs run one at
   * a time and a request during a run gets one follow-up run
   * ({@link HaxeLibrarySyncQueue}); {@code onFinished} runs on the EDT once
   * a sync started after this request completed.
   */
  public static void sync(@NotNull Project project, @Nullable Runnable onFinished) {
    HaxeLibrarySyncQueue.getInstance(project).request(onFinished);
  }

  /** One sync run; {@code onDone} runs on the EDT exactly once, whether the run applied libraries or stopped short. */
  static void runOnce(@NotNull Project project, @NotNull Runnable onDone) {
    new Task.Backgroundable(project, HaxeBundle.message("haxe.library.sync.progress"), false) {
      // once the workspace apply is scheduled, it signals the completion itself
      private boolean applying;

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        Collected collected = HaxeReadActions.compute(() -> collect(project));
        if (project.isDisposed()) return;
        // they need no haxelib run: published before the slow lookups, so files resolve through them early
        HaxeExternalClasspathStore.getInstance(project).replaceAll(collected.externalClasspaths());

        Map<String, Map<String, List<String>>> byModuleName = new LinkedHashMap<>();
        SyncResult result = new SyncResult(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
        collected.dependencies().forEach((module, declared) -> {
          Resolution resolution = resolveClasspaths(project, module, declared, indicator);
          byModuleName.put(module.getName(), resolution.classpaths());
          result.definesByModule().put(module.getName(), resolution.definesByDependency());
          resolution.libraries().forEach(result.libraries()::putIfAbsent);
          result.closuresByModule().put(module.getName(), resolution.closureByDependency());
        });
        if (!project.isDisposed()) {
          applying = true;
          Runnable onApplied = () -> recordResolution(project, result, onDone);
          HaxeLibraryWorkspace.getInstance(project).applyAllAsync(byModuleName, onApplied);
        }
      }

      @Override
      public void onFinished() {
        // a run that never reached the apply (cancelled, failed) still completes
        if (!applying && !project.isDisposed()) onDone.run();
      }
    }.queue();
  }

  /** On the EDT after the libraries landed: the defines and the dependency graph are recorded, then the end of the run is announced. */
  private static void recordResolution(@NotNull Project project, @NotNull SyncResult result, @Nullable Runnable onFinished) {
    if (!project.isDisposed()) {
      HaxeLibraryDefinesStore.getInstance(project).replaceAll(result.definesByModule());
      HaxeLibraryGraph.getInstance(project).replaceAll(result.libraries(), result.closuresByModule());
      HaxeInstalledLibraries.getInstance(project).refreshAsync();
      HaxeBuildEventListener.publisher(project).librariesSynced();
    }
    if (onFinished != null) {
      onFinished.run();
    }
  }

  /** Every module's inputs; a module the legacy engine equips gets none ({@link #legacyEngineOwns}). Call in a read action. */
  @NotNull
  static Collected collect(@NotNull Project project) {
    Collected collected = new Collected(new LinkedHashMap<>(), new LinkedHashMap<>());
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (legacyEngineOwns(project, module)) {
        collected.dependencies().put(module, List.of());
        continue;
      }
      List<HaxeBuildFile> buildFiles = HaxeKnownBuildFiles.forModule(project, module);
      List<HaxeBuildFile> sources = dependencySources(project, module, buildFiles);
      collected.dependencies().put(module, declaredDependencies(project, module, buildFiles, sources));
      collected.externalClasspaths().put(module.getName(), HaxeExternalClasspaths.of(project, module, sources));
    }
    return collected;
  }

  /** The libraries the module's builds ({@code sources}) declare, plus the hxp toolchain when it holds hxp scripts. */
  @NotNull
  private static List<DeclaredDependency> declaredDependencies(@NotNull Project project,
                                                               @NotNull Module module,
                                                               @NotNull List<HaxeBuildFile> buildFiles,
                                                               @NotNull List<HaxeBuildFile> sources) {
    List<DeclaredDependency> declared = new ArrayList<>();
    for (HaxeBuildFile buildFile : sources) {
      Path workDirectory = haxelibWorkDirectory(project, module, buildFile);
      for (HaxeLibDependency dependency : effectiveLibraries(project, buildFile)) {
        addIfNameAbsent(declared, new DeclaredDependency(dependency, workDirectory));
      }
    }
    addHxpToolchainLibraries(project, module, buildFiles, declared);
    return declared;
  }

  /**
   * Where haxelib resolves a build's libraries: the build's work directory
   * (its override, else the sniffed folder), so a {@code .haxelib} repository
   * local to that build is the one its compile uses; the container root for a
   * build without a directory.
   */
  @Nullable
  private static Path haxelibWorkDirectory(@NotNull Project project, @NotNull Module module, @NotNull HaxeBuildFile buildFile) {
    String workDirectory = HaxeBuildWorkDirectories.workDirectory(project, buildFile.file());
    return workDirectory != null ? Path.of(workDirectory) : containerWorkDirectory(project, module);
  }

  /** The container root: where the libraries no build declares (the hxp toolchain) resolve from; null without a project directory. */
  @Nullable
  private static Path containerWorkDirectory(@NotNull Project project, @NotNull Module module) {
    String root = HaxeContainers.containerRootPath(project, module.getName());
    return root == null ? null : Path.of(root);
  }

  /**
   * Whether the legacy (V1) library engine equips the module: a V1-type module
   * in a project the legacy updater tracks, or one already carrying the
   * engine's "Haxelib: " entries. The sync then attaches nothing of its own
   * (and drops what it attached earlier), so no dependency appears twice;
   * the module gets the sync's libraries once it is converted.
   */
  private static boolean legacyEngineOwns(@NotNull Project project, @NotNull Module module) {
    if (!HaxeV1Migrator.isLegacy(module)) return false;
    if (HaxelibProjectUpdater.tracksProject(project)) return true;
    for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
      if (entry instanceof LibraryOrderEntry library && isLegacyManaged(library.getLibraryName())) return true;
    }
    return false;
  }

  private static boolean isLegacyManaged(@Nullable String libraryName) {
    return libraryName != null && HaxelibNameUtil.isManagedLibrary(libraryName);
  }

  /**
   * Modules holding .hxp build scripts get the hxp and lime haxelibs attached
   * as regular module libraries, so the scripts (importing hxp.* /
   * lime.tools.* from the toolchain) resolve with completion. Deliberate
   * trade-off: the module's own sources can reference these libs too, and
   * only the compiler will complain.
   */
  private static void addHxpToolchainLibraries(@NotNull Project project,
                                               @NotNull Module module,
                                               @NotNull List<HaxeBuildFile> buildFiles,
                                               @NotNull List<DeclaredDependency> dependencies) {
    boolean hasHxpScript = buildFiles.stream()
      .anyMatch(file -> file.type() == HaxeBuildFileType.HXP_PROJECT || file.type() == HaxeBuildFileType.HXP_SCRIPT);
    if (!hasHxpScript) return;
    Path workDirectory = containerWorkDirectory(project, module);
    for (String library : HXP_TOOLCHAIN_LIBRARIES) {
      addIfNameAbsent(dependencies, new DeclaredDependency(new HaxeLibDependency(library, null), workDirectory));
    }
  }

  private static void addIfNameAbsent(@NotNull List<DeclaredDependency> dependencies, @NotNull DeclaredDependency declared) {
    String key = declared.dependency().key();
    boolean known = dependencies.stream().anyMatch(existing -> existing.dependency().key().equals(key));
    if (!known) {
      dependencies.add(declared);
    }
  }

  /**
   * A build file's library dependencies with conditionals evaluated
   * ({@link HaxeEffectiveBuildFileInfo}); a landed evaluation asks for one
   * more sync through the queue's callback.
   */
  @NotNull
  private static List<HaxeLibDependency> effectiveLibraries(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    Runnable onEvaluated = HaxeLibrarySyncQueue.getInstance(project).resyncRequest();
    return HaxeEffectiveBuildFileInfo.of(project, buildFile, onEvaluated).libraries();
  }

  /**
   * The build files whose libraries and classpaths the module gets: all its
   * known build files, plus the container's tests build files it owns - the
   * module's source roots hold the test sources too, and a marked tests
   * build need not be a known build file.
   */
  @NotNull
  private static List<HaxeBuildFile> dependencySources(@NotNull Project project,
                                                       @NotNull Module module,
                                                       @NotNull List<HaxeBuildFile> buildFiles) {
    List<HaxeBuildFile> sources = new ArrayList<>(buildFiles);
    for (HaxeBuildFile tests : testsBuildFiles(project, module, buildFiles)) {
      boolean known = sources.stream()
        .anyMatch(source -> source.file().getPath().equals(tests.file().getPath()));
      if (!known) {
        sources.add(tests);
      }
    }
    return sources;
  }

  /** The container's marked (or convention-suggested) tests build files that this module owns (see {@link HaxeTestFrameworks#testsBuildPaths}). */
  @NotNull
  private static List<HaxeBuildFile> testsBuildFiles(@NotNull Project project,
                                                     @NotNull Module module,
                                                     @NotNull List<HaxeBuildFile> buildFiles) {
    Map<String, List<HaxeLibDependency>> librariesByPath = new LinkedHashMap<>();
    for (HaxeBuildFile buildFile : buildFiles) {
      librariesByPath.put(buildFile.file().getPath(), HaxeBuildSections.inspectSelected(project, buildFile).libraries());
    }
    List<String> testsPaths = HaxeTestFrameworks.testsBuildPaths(project, module.getName(), librariesByPath);
    List<HaxeBuildFile> owned = new ArrayList<>();
    for (String testsPath : testsPaths) {
      HaxeBuildFile tests = byPath(project, buildFiles, testsPath);
      if (tests != null && ownedBy(project, tests, module)) {
        owned.add(tests);
      }
    }
    return owned;
  }

  /** Finds the path among the scanned files, or loads it directly (manually added files live outside the scan). */
  @Nullable
  private static HaxeBuildFile byPath(@NotNull Project project, @NotNull List<HaxeBuildFile> scanned, @Nullable String path) {
    if (StringUtil.isEmptyOrSpaces(path)) return null;
    for (HaxeBuildFile buildFile : scanned) {
      if (buildFile.file().getPath().equals(path)) return buildFile;
    }
    return HaxeBuildFileScanner.findBuildFile(project, path);
  }

  private static boolean ownedBy(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull Module module) {
    return HaxeContainers.containerIdFor(project, buildFile.file()).equals(module.getName());
  }

  @NotNull
  private static Resolution resolveClasspaths(@NotNull Project project,
                                              @NotNull Module module,
                                              @NotNull List<DeclaredDependency> dependencies,
                                              @NotNull ProgressIndicator indicator) {
    Sdk sdk = HaxeToolPathResolver.resolveSdk(project, module.getName());
    Map<String, List<String>> libraries = new LinkedHashMap<>();
    Map<String, Map<String, String>> definesByDependency = new LinkedHashMap<>();
    Map<String, Library> resolvedLibraries = new LinkedHashMap<>();
    Map<String, List<String>> closureByDependency = new LinkedHashMap<>();
    if (sdk != null) {
      for (DeclaredDependency declared : dependencies) {
        HaxeLibDependency dependency = declared.dependency();
        indicator.setText2(dependency.name());
        List<String> output = haxelibPathOutput(project, module, declared);
        Map<String, String> defines = new LinkedHashMap<>();
        List<String> closure = new ArrayList<>();
        // one External Libraries entry PER library in the output (see HaxelibPathParser.parseSections)
        for (LibrarySection section : HaxelibPathParser.parseSections(dependency.name(), output)) {
          String version = sectionVersion(section, dependency);
          defines.putIfAbsent(section.name(), version != null ? version : FLAG_DEFINE_VALUE);
          for (HaxeDefine extraDefine : section.extraDefines()) {
            defines.putIfAbsent(extraDefine.name(), extraDefine.value() != null ? extraDefine.value() : FLAG_DEFINE_VALUE);
          }
          closure.add(section.name());
          resolvedLibraries.putIfAbsent(section.name(), resolvedLibrary(section, version));
          if (section.classpaths().isEmpty()) continue;
          // TODO: drop classpaths under the module's own content roots - a lib resolving to the open
          //  checkout (a tests project's <haxelib path="../.."/>) registers the module's source folder
          //  as a library root, and the platform then skips inspections and external annotators there

          String entryName = version == null ? section.name() : section.name() + " " + version;
          if (isScmCheckout(section)) entryName += " [git]";
          libraries.merge(managedLibraryName(entryName), section.classpaths(), HaxeLibrarySync::unionPreservingOrder);
        }
        definesByDependency.put(dependency.key(), defines);
        closureByDependency.put(dependency.key(), closure);
      }
    }
    return new Resolution(libraries, definesByDependency, resolvedLibraries, closureByDependency);
  }

  /** The graph node of a section: its resolved version, and the requirements and folder of the haxelib.json above its roots. */
  @NotNull
  private static Library resolvedLibrary(@NotNull LibrarySection section, @Nullable String version) {
    Path manifest = HaxelibManifest.find(section.classpaths());
    List<HaxeLibraryGraph.Requirement> requires = manifest == null ? List.of() : HaxelibManifest.requirements(manifest);
    return new Library(section.name(), version, requires, section.extraDefines(), libraryRoot(section, manifest));
  }

  /** The library's folder as lime takes it: the haxelib.json's, else the classpath haxelib prints right before the library's version define. */
  @Nullable
  private static String libraryRoot(@NotNull LibrarySection section, @Nullable Path manifest) {
    if (manifest != null) return FileUtil.toSystemIndependentName(manifest.getParent().toString());
    if (section.classpaths().isEmpty()) return null;
    return FileUtil.toSystemIndependentName(Path.of(section.classpaths().getLast()).toString());
  }

  /// The section carries its own version; the queried library itself may only know it from the dependency.
  @Nullable
  private static String sectionVersion(LibrarySection section,
                                       HaxeLibDependency dependency) {
    if (section.version() != null) return section.version();
    return section.name().equals(dependency.name()) ? dependency.version() : null;
  }

  /**
   * A git/hg checkout serves from {@code <repo>/<lib>/git/...}; its
   * {@code -D} marker still names the release in the checkout's
   * haxelib.json, so the entry says which it really is.
   */
  private static boolean isScmCheckout(LibrarySection section) {
    for (String classpath : section.classpaths()) {
      String[] segments = classpath.replace('\\', '/').split("/");
      for (int i = 1; i < segments.length; i++) {
        boolean checkoutSegment = HaxelibRepositoryLayout.isScmDirName(segments[i]);
        if (checkoutSegment && segments[i - 1].equalsIgnoreCase(section.name())) return true;
      }
    }
    return false;
  }

  @NotNull
  private static List<String> unionPreservingOrder(@NotNull List<String> first, @NotNull List<String> second) {
    List<String> union = new ArrayList<>(first);
    for (String path : second) {
      if (!union.contains(path)) {
        union.add(path);
      }
    }
    return union;
  }

  @NotNull
  private static List<String> haxelibPathOutput(@NotNull Project project, @NotNull Module module, @NotNull DeclaredDependency declared) {
    HaxeLibDependency dependency = declared.dependency();
    String spec = dependency.name();
    // a release version can be passed to `haxelib path`; git/path specs cannot
    if (HaxelibSemVer.isReleaseVersion(dependency.version())) {
      spec += ":" + dependency.version();
    }
    return HaxelibCommands.output(project, module.getName(), declared.workDirectory(), List.of("path", spec));
  }
}
