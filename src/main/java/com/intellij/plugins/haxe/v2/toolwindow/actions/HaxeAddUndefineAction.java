package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Context menu on a build file's IDE Overrides or Defines row: adds a REMOVE
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
    String buildFilePath = HaxeAddDefineAction.selectedOverridesBuildFilePath(panel);
    if (project == null || buildFilePath == null) return;

    String name = DefinePrompt.showUndefineName(project);
    if (name != null) {
      HaxeIdeOverridesStore.getInstance(project).putUndefine(buildFilePath, name);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(HaxeAddDefineAction.selectedOverridesBuildFilePath(panel) != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
