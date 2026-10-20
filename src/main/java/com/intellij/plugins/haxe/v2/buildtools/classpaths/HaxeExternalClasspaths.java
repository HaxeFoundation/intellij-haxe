package com.intellij.plugins.haxe.v2.buildtools.classpaths;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildClasspaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build-file classpaths outside the module's own sources, such as
 * {@code -cp ../shared/src} or {@code <source path="../shared"/>}. The
 * compiler reads them like any other classpath, so the module's files resolve
 * against them: they join its resolve scope and its Haxe roots, and a folder
 * outside every module is indexed as a synthetic library
 * ({@link HaxeExternalClasspathStore}). The module setup itself stays
 * untouched. Only the declared entries count: the implicit work directory is
 * the module's own folder, and haxelib paths are the library sync's entries.
 */
public final class HaxeExternalClasspaths {

  /** A classpath folder outside the module's sources; {@code outsideModules} when no module's content holds it. */
  public record ExternalClasspath(@NotNull VirtualFile directory, boolean outsideModules) {
  }

  private HaxeExternalClasspaths() {
  }

  /** The build files' declared classpaths that lie outside the module's own sources. Call in a read action. */
  @NotNull
  public static List<ExternalClasspath> of(@NotNull Project project, @NotNull Module module, @NotNull List<HaxeBuildFile> buildFiles) {
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
    Map<VirtualFile, ExternalClasspath> external = new LinkedHashMap<>();
    for (HaxeBuildFile buildFile : buildFiles) {
      for (VirtualFile directory : HaxeBuildClasspaths.declaredDirectories(project, buildFile)) {
        Module owner = fileIndex.getModuleForFile(directory);
        boolean skipped = isModulesOwn(directory, module, owner) || holdsContent(directory, contentRoots);
        if (!skipped) {
          external.putIfAbsent(directory, new ExternalClasspath(directory, owner == null));
        }
      }
    }
    return List.copyOf(external.values());
  }

  /**
   * Whether the folder belongs to the module itself: its own content, or one
   * of its own source roots that holds none of the owner's content. Two
   * modules can mark the same folder as a source root (a root module
   * re-marking a nested module's sources); the platform hands its files to
   * one of them, and the other still owns the folder.
   */
  private static boolean isModulesOwn(@NotNull VirtualFile directory, @NotNull Module module, @Nullable Module owner) {
    if (owner == module) return true;
    VirtualFile[] ownerContentRoots = owner == null ? VirtualFile.EMPTY_ARRAY : ModuleRootManager.getInstance(owner).getContentRoots();
    for (VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots()) {
      if (!VfsUtilCore.isAncestor(sourceRoot, directory, false)) continue;
      boolean holdsOwner = Arrays.stream(ownerContentRoots).anyMatch(root -> VfsUtilCore.isAncestor(sourceRoot, root, false));
      if (!holdsOwner) return true;
    }
    return false;
  }

  /**
   * Whether the folder holds a module's content root below it ({@code -cp ..}):
   * as a classpath it would re-root that module's packages, and as a library
   * turn its files into library code.
   */
  private static boolean holdsContent(@NotNull VirtualFile directory, @NotNull VirtualFile[] contentRoots) {
    for (VirtualFile contentRoot : contentRoots) {
      if (VfsUtilCore.isAncestor(directory, contentRoot, true)) return true;
    }
    return false;
  }
}
