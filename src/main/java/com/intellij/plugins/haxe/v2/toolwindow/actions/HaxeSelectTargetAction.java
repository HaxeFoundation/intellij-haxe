package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowEditors;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.TargetNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: opens the target dropdown for a selectable target row
 * (XML/HXP based projects; HXML targets are fixed by the file).
 */
public final class HaxeSelectTargetAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeSelectTargetAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.select.target.title"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    if (panel.getSelectedUserObject() instanceof TargetNode targetNode && targetNode.selectable()) {
      HaxeToolWindowEditors.showTargetPopup(project, targetNode, panel.getSelectionPopupPoint());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean selectable = panel.getSelectedUserObject() instanceof TargetNode targetNode && targetNode.selectable();
    e.getPresentation().setEnabledAndVisible(selectable);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
