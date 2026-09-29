package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvironmentNode;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeEnvironmentDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on any environment row: opens the Configure Environment dialog
 * (SDK selector + defines table).
 */
public final class HaxeConfigureEnvironmentAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeConfigureEnvironmentAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.configure.environment"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    EnvironmentNode environment = panel.getSelectedEnvironmentNode();
    if (project == null || environment == null) return;

    HaxeEnvironmentDialog dialog = new HaxeEnvironmentDialog(project, environment.containerId(),
                                                             environment.displayName(),
                                                             environment.activeBuildFileDefines());
    dialog.show();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(panel.getSelectedEnvironmentNode() != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
