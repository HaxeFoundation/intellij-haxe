package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvDefinesNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvironmentNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tree context menu on an Environment (or its Defines) row: adds a user define.
 */
public final class HaxeAddDefineAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeAddDefineAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.add.define"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    String containerId = selectedContainerId();
    if (project == null || containerId == null) return;

    DefinePrompt.DefineInput input = DefinePrompt.show(project, null);
    if (input != null) {
      HaxeEnvironmentStore.getInstance(project).putDefine(containerId, input.name(), input.value());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(selectedContainerId() != null);
  }

  @Nullable
  private String selectedContainerId() {
    return switch (panel.getSelectedUserObject()) {
      case EnvironmentNode environmentNode -> environmentNode.containerId();
      case EnvDefinesNode definesNode -> definesNode.containerId();
      case null, default -> null;
    };
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
