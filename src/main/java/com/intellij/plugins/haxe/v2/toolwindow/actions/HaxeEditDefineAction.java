package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvDefineNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on an environment define: edits its name/value.
 */
public final class HaxeEditDefineAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeEditDefineAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.edit.define"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null || !(panel.getSelectedUserObject() instanceof EnvDefineNode define)) return;

    String initial = define.value().isEmpty() ? define.name() : define.name() + "=" + define.value();
    DefinePrompt.DefineInput input = DefinePrompt.show(project, initial);
    if (input == null) return;

    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);
    if (!input.name().equals(define.name())) {
      store.removeDefine(define.containerId(), define.name());
    }
    store.putDefine(define.containerId(), input.name(), input.value());
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
