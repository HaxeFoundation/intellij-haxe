package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeSourceRootsOffer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The build files a container actually has, as the tool window presents them:
 * top-level auto-detected files plus manual additions, minus hidden ones. Every
 * consumer of "the container's build files" or "the active build file" must go
 * through here — the scanner alone misses manually registered files (the only
 * kind a project with build files in subfolders has), and the active-file store
 * alone misses the implicit single-file case the tree shows as (Active).
 */
public final class HaxeKnownBuildFiles {

  private HaxeKnownBuildFiles() {
  }

  /** A module's known build files (detected + manual − hidden); none for a disposed module. Call inside a read action. */
  @NotNull
  public static List<HaxeBuildFile> forModule(@NotNull Project project, @NotNull Module module) {
    if (module.isDisposed()) return List.of();
    return mergeWithStore(project, module.getName(), HaxeBuildFileScanner.scan(module));
  }

  /** A container's known build files: a module's, or the project root's. Call inside a read action. */
  @NotNull
  public static List<HaxeBuildFile> forContainer(@NotNull Project project, @NotNull String containerId) {
    if (HaxeContainers.PROJECT_ROOT_CONTAINER.equals(containerId)) {
      return mergeWithStore(project, containerId, HaxeBuildFileScanner.scanProjectRoot(project));
    }
    Module module = ModuleManager.getInstance(project).findModuleByName(containerId);
    return module == null ? List.of() : forModule(project, module);
  }

  /** Every known build file in the project: all module containers plus the project root. Call inside a read action. */
  @NotNull
  public static List<HaxeBuildFile> all(@NotNull Project project) {
    Map<String, HaxeBuildFile> byPath = new LinkedHashMap<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      for (HaxeBuildFile buildFile : forModule(project, module)) {
        byPath.putIfAbsent(buildFile.file().getPath(), buildFile);
      }
    }
    List<HaxeBuildFile> projectRootFiles = forContainer(project, HaxeContainers.PROJECT_ROOT_CONTAINER);
    for (HaxeBuildFile buildFile : projectRootFiles) {
      byPath.putIfAbsent(buildFile.file().getPath(), buildFile);
    }
    return List.copyOf(byPath.values());
  }

  /**
   * The project's EFFECTIVE active build file: the stored choice while it is
   * a known build file (a deleted or hidden one no longer is), else the sole
   * known build file (the rule behind the tree's (Active) badge). Runs the
   * full known-files sweep (module scans, VFS lookups) — call inside a read
   * action, and never from a per-candidate hot path.
   */
  @Nullable
  public static String effectiveActivePath(@NotNull Project project) {
    return HaxeActiveBuildFileStore.getInstance(project).resolveActivePath(knownPaths(project));
  }

  /** Whether the path names a known build file of any container. Call inside a read action. */
  public static boolean isKnown(@NotNull Project project, @NotNull String path) {
    return knownPaths(project).contains(path);
  }

  @NotNull
  private static List<String> knownPaths(@NotNull Project project) {
    return all(project).stream().map(buildFile -> buildFile.file().getPath()).toList();
  }

  /**
   * Registers a build file as a manual addition to the container and runs the
   * full follow-up: source-roots offer, then a project sync - a newly known
   * build file can change the module's library set (it may even become the
   * implicit active file). {@code onFinished} runs when the sync completes.
   */
  public static void registerBuildFile(@NotNull Project project,
                                       @NotNull String containerId,
                                       @NotNull VirtualFile file,
                                       @Nullable Runnable onFinished) {
    HaxeBuildFilesStore.getInstance(project).addFile(containerId, file.getPath());
    HaxeSourceRootsOffer.offerFor(project, containerId, file);
    HaxeProjectSync.sync(project, onFinished);
  }

  /** Applies the manual-files store to a detected set: additions resolved and typed, hidden paths dropped. */
  @NotNull
  public static List<HaxeBuildFile> mergeWithStore(@NotNull Project project,
                                                   @NotNull String containerId,
                                                   @NotNull List<HaxeBuildFile> detected) {
    HaxeBuildFilesStore store = HaxeBuildFilesStore.getInstance(project);
    Map<String, HaxeBuildFile> byPath = new LinkedHashMap<>();
    for (HaxeBuildFile buildFile : detected) {
      byPath.putIfAbsent(buildFile.file().getPath(), buildFile);
    }
    for (String path : store.getAddedPaths(containerId)) {
      if (byPath.containsKey(path)) continue;
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
      if (file == null || !file.isValid()) continue;
      HaxeBuildFileType type = HaxeBuildFileScanner.detectType(project, file);
      if (type != null) {
        byPath.put(path, new HaxeBuildFile(file, type));
      }
    }
    store.getHiddenPaths(containerId).forEach(byPath::remove);
    return List.copyOf(byPath.values());
  }
}
