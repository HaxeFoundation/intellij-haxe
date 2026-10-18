package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowEditors;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on a build file row: removes a manually added file, or hides
 * an auto-detected one (re-adding it via Add Build File un-hides it), after the
 * same confirmation the Delete key and the toolbar show.
 */
public final class HaxeRemoveBuildFileAction extends HaxeSelectedNodeAction<BuildFileRow> {

  public HaxeRemoveBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(panel, BuildFileRow.class, HaxeBundle.message("haxe.toolwindow.remove.build.file"));
  }

  @Override
  protected void perform(@NotNull Project project, @NotNull BuildFileRow row) {
    HaxeToolWindowEditors.confirmAndRemoveBuildFile(project, row);
  }

  /** "Remove" for a manually added file, "Hide" for a detected one. */
  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    BuildFileRow row = selectedNode();
    if (row != null) {
      String key = row.manual() ? "haxe.toolwindow.remove.build.file" : "haxe.toolwindow.hide.build.file";
      e.getPresentation().setText(HaxeBundle.message(key));
    }
  }
}
