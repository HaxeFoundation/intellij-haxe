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
 * Tree context menu on a plain action row or a tool row: runs it (same as
 * double-click). A launching action row gets Run and Debug from
 * {@link HaxeRunProgramAction} instead.
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
    boolean plainAction = selected instanceof ActionNode actionNode && actionNode.launch() == null;
    e.getPresentation().setEnabledAndVisible(plainAction || selected instanceof ToolNode);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
