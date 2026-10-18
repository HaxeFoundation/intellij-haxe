package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildCaches;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tree context menu on a build file (or any of its child rows): drops the file's
 * cached evaluation and re-runs everything derived from it - tree info, library
 * sync and, through the sync's end, the parse-context defines.
 */
public final class HaxeReloadBuildFileAction extends HaxeSelectedNodeAction<BuildFileRow> {

  public HaxeReloadBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, BuildFileRow.class, HaxeBundle.message("haxe.toolwindow.reload.build.file"), AllIcons.Actions.Refresh);
  }

  @Override
  protected @Nullable Object selection() {
    return panel.getSelectedBuildFileRowAncestor();
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull BuildFileRow row) {
    HaxeBuildCaches.clearEvaluations(project, row.buildFile().file().getPath());
    panel.refreshTree();
    HaxeLibrarySync.sync(project, null);
  }
}
