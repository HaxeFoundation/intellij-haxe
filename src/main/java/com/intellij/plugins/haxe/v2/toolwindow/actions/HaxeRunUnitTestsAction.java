package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestRunConfigurations;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ModuleNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProjectNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.TestRunNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: runs the selection's unit tests - directly on the "Run Unit
 * Tests" row, or on a container row through its marked (or convention-suggested)
 * tests build file. Disabled with a hint while the container has none.
 */
public final class HaxeRunUnitTestsAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeRunUnitTestsAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.node.run.unit.tests"), null, AllIcons.RunConfigurations.TestState.Run);
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    String buildFilePath = panel.resolveTestsPath(panel.getSelectedUserObject());
    if (buildFilePath != null) {
      HaxeTestRunConfigurations.run(project, buildFilePath);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Object selection = panel.getSelectedUserObject();
    if (selection instanceof TestRunNode) {
      e.getPresentation().setEnabledAndVisible(true);
      return;
    }
    if (!(selection instanceof ModuleNode) && !(selection instanceof ProjectNode)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    boolean hasTestsFile = panel.resolveTestsPath(selection) != null;
    e.getPresentation().setVisible(true);
    e.getPresentation().setEnabled(hasTestsFile);
    // set in BOTH branches: the presentation is reused across selections, so
    // a hint set once would stick to every later, enabled selection
    e.getPresentation().setDescription(
      hasTestsFile ? null : HaxeBundle.message("haxe.toolwindow.run.unit.tests.no.tests.file"));
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
