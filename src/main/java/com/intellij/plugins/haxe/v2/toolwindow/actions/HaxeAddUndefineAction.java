package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Context menu on the IDE environment or Define overrides row: adds a REMOVE
 * entry, which hides a define the build file, target or libraries set.
 */
public final class HaxeAddUndefineAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeAddUndefineAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.add.undefine"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    String containerId = HaxeAddDefineAction.selectedEnvironmentContainerId(panel);
    if (project == null || containerId == null) return;

    String name = DefinePrompt.showUndefineName(project);
    if (name != null) {
      HaxeEnvironmentStore.getInstance(project).putUndefine(containerId, name);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(HaxeAddDefineAction.selectedEnvironmentContainerId(panel) != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
