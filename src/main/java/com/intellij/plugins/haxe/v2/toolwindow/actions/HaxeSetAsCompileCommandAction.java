package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore.CompileCommand;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu on an action row: makes it the container's compile command
 * (what a project build runs), keeping any previously configured extra arguments.
 */
public final class HaxeSetAsCompileCommandAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeSetAsCompileCommandAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.set.as.compile.command"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    BuildFileRow fileRow = panel.getSelectedBuildFileRowAncestor();
    if (project == null || fileRow == null
        || !(panel.getSelectedUserObject() instanceof ActionNode actionNode)) {
      return;
    }

    HaxeEnvironmentStore store = HaxeEnvironmentStore.getInstance(project);
    CompileCommand previous = store.getCompileCommand(fileRow.containerId());
    String arguments = previous == null ? "" : StringUtil.notNullize(previous.arguments());
    CompileCommand compileCommand = new CompileCommand(actionNode.ownerId(), actionNode.name(), arguments);
    store.setCompileCommand(fileRow.containerId(), compileCommand);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean applicable = panel.getSelectedUserObject() instanceof ActionNode
                         && panel.getSelectedBuildFileRowAncestor() != null;
    e.getPresentation().setEnabledAndVisible(applicable);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
