package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowLaunches;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvCompileCommandNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on a configured Compile command row: runs it.
 */
public final class HaxeRunCompileCommandAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeRunCompileCommandAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.run.compile.command"), null, AllIcons.Actions.Compile);
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    if (panel.getSelectedUserObject() instanceof EnvCompileCommandNode compileCommand) {
      HaxeToolWindowLaunches.runCompileCommand(project, compileCommand);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean runnable = panel.getSelectedUserObject() instanceof EnvCompileCommandNode compileCommand
                       && compileCommand.command() != null;
    e.getPresentation().setEnabledAndVisible(runnable);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
