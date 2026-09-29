package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowLaunches;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on an action row: runs it (same as double-click).
 */
public final class HaxeRunActionNodeAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeRunActionNodeAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.run.action"), null, AllIcons.Actions.Execute);
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    switch (panel.getSelectedUserObject()) {
      case ToolNode toolNode -> HaxeToolWindowLaunches.runTool(project, toolNode);
      case ActionNode actionNode -> HaxeToolWindowLaunches.runAction(project, actionNode);
      case null, default -> { }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Object selected = panel.getSelectedUserObject();
    e.getPresentation().setEnabledAndVisible(selected instanceof ActionNode || selected instanceof ToolNode);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
