package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryDiagramOpener;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.HaxeToolWindowNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.LibraryNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tree context menu on a build file or on one of its library rows: opens the
 * build file's project diagram (its includes, libraries and their
 * dependencies). Shown only while the Diagrams plugin provides an opener;
 * without it the entry does not exist.
 */
public final class HaxeShowLibraryDependenciesAction extends HaxeSelectedNodeAction<HaxeToolWindowNode> {

  public HaxeShowLibraryDependenciesAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, HaxeToolWindowNode.class, HaxeBundle.message("haxe.toolwindow.show.project.diagram"));
  }

  @Override
  protected boolean applies(@NotNull HaxeToolWindowNode node) {
    return buildFileOf(node) != null && HaxeLibraryDiagramOpener.available() != null;
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull HaxeToolWindowNode node) {
    HaxeBuildFile buildFile = buildFileOf(node);
    HaxeLibraryDiagramOpener opener = HaxeLibraryDiagramOpener.available();
    if (buildFile != null && opener != null) opener.open(project, buildFile);
  }

  /** The build file a row belongs to: the row itself or the owner of a library row; null for any other row. */
  @Nullable
  private static HaxeBuildFile buildFileOf(@NotNull HaxeToolWindowNode node) {
    return switch (node) {
      case BuildFileRow row -> row.buildFile();
      case LibraryNode library -> library.owner();
      default -> null;
    };
  }
}
