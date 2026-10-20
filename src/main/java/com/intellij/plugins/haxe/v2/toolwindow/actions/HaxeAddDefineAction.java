package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.IdeDefinesNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.IdeOverridesNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tree context menu on a build file's IDE Overrides (or its Defines) row: adds a define entry.
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
    String buildFilePath = selectedOverridesBuildFilePath(panel);
    if (project == null || buildFilePath == null) return;

    DefinePrompt.DefineInput input = DefinePrompt.show(project, null);
    if (input != null) {
      HaxeIdeOverridesStore.getInstance(project).putDefine(buildFilePath, input.name(), input.value());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(selectedOverridesBuildFilePath(panel) != null);
  }

  /** The build file of the selected IDE Overrides or Defines row; null for any other selection. */
  @Nullable
  static String selectedOverridesBuildFilePath(@NotNull HaxeToolWindowPanel panel) {
    return switch (panel.getSelectedUserObject()) {
      case IdeOverridesNode overridesNode -> overridesNode.buildFile().file().getPath();
      case IdeDefinesNode definesNode -> definesNode.buildFile().file().getPath();
      case null, default -> null;
    };
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
