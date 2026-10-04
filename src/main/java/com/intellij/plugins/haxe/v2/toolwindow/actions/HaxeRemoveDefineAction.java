package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowEditors;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvDefineNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on an environment define: removes it after the same
 * confirmation the Delete key shows.
 */
public final class HaxeRemoveDefineAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeRemoveDefineAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.remove.define"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null && panel.getSelectedUserObject() instanceof EnvDefineNode define) {
      HaxeToolWindowEditors.confirmAndRemoveDefine(project, define);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(panel.getSelectedUserObject() instanceof EnvDefineNode);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
