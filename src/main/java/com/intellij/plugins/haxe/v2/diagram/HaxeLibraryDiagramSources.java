package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLFileType;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlIncludeTree;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSections;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSystem;
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeEffectiveBuildFileInfo;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What the diagram reads about a build file: its include tree, or for a
 * non-hxml build file one entry with its inspected declarations; and for a
 * build that merges its libraries' include files, what its build tool
 * evaluated.
 */
final class HaxeLibraryDiagramSources {

  private HaxeLibraryDiagramSources() {
  }

  /** Null when the path is not a file any more. Read action. */
  @Nullable
  static HxmlIncludeTree treeOf(@NotNull Project project, @NotNull String path) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    if (file == null) return null;
    if (HXMLFileType.DEFAULT_EXTENSION.equalsIgnoreCase(file.getExtension())) return HxmlIncludeTree.of(file);
    HaxeBuildFile buildFile = knownBuildFile(project, path);
    HaxeBuildFileInfo info = buildFile == null ? HaxeBuildFileInfo.EMPTY : HaxeBuildSections.inspectSelected(project, buildFile);
    return HxmlIncludeTree.leaf(file, info);
  }

  /**
   * What the build uses for its selected target when its build tool merges
   * the libraries' include files (lime, nme): the tool's evaluation, the parse
   * until that lands. Null for any other build file. Read action.
   */
  @Nullable
  static HaxeBuildFileInfo mergingBuildInfo(@NotNull Project project, @NotNull String path) {
    HaxeBuildFile buildFile = knownBuildFile(project, path);
    if (buildFile == null || !HaxeBuildSystem.of(buildFile.type()).mergesLibraryIncludeFiles()) return null;
    return HaxeEffectiveBuildFileInfo.of(project, buildFile);
  }

  /** The folder of the library as the last sync resolved it; null when the sync knows no folder for it. */
  @Nullable
  static VirtualFile libraryRoot(@NotNull HaxeLibraryGraph graph, @NotNull String library) {
    HaxeLibraryGraph.Library resolved = graph.library(library);
    if (resolved == null || resolved.root() == null) return null;
    return LocalFileSystem.getInstance().findFileByPath(resolved.root());
  }

  @Nullable
  private static HaxeBuildFile knownBuildFile(@NotNull Project project, @NotNull String path) {
    for (HaxeBuildFile buildFile : HaxeKnownBuildFiles.all(project)) {
      if (path.equals(buildFile.file().getPath())) return buildFile;
    }
    return null;
  }
}
