package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibrarySync;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeLimeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeNmeProjectInfoService;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on a build file (or any of its child rows): drops the file's
 * cached evaluation and re-runs everything derived from it - tree info, library
 * sync and (via the refresh funnel) the parse-context defines.
 */
public final class HaxeReloadBuildFileAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeReloadBuildFileAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.reload.build.file"), null, AllIcons.Actions.Refresh);
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    BuildFileRow row = panel.getSelectedBuildFileRowAncestor();
    if (project == null || row == null) return;

    HaxeLimeProjectInfoService.getInstance(project).invalidate(row.buildFile().file().getPath());
    HaxeNmeProjectInfoService.getInstance(project).invalidate(row.buildFile().file().getPath());
    panel.refreshTree();
    HaxeLibrarySync.sync(project, panel::refreshTree);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(panel.getSelectedBuildFileRowAncestor() != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
