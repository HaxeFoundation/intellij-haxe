package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestRunConfigurations;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ModuleNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProjectNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.TestRunNode;
import org.jetbrains.annotations.NotNull;

/**
 * Tree context menu: debugs the selection's unit tests, shown beside Run
 * wherever it applies and enabled when the tests build's target has a debug
 * lane; an unsupported target's row explains which targets do.
 */
public final class HaxeDebugUnitTestsAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeDebugUnitTestsAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.node.debug.unit.tests"), null, AllIcons.Actions.StartDebugger);
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    String buildFilePath = panel.resolveTestsPath(panel.getSelectedUserObject());
    if (buildFilePath != null) {
      HaxeTestRunConfigurations.debug(project, buildFilePath);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    Object selection = panel.getSelectedUserObject();
    boolean relevant = selection instanceof TestRunNode
      || selection instanceof ModuleNode
      || selection instanceof ProjectNode;
    String buildFilePath = relevant ? panel.resolveTestsPath(selection) : null;
    if (project == null || buildFilePath == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    boolean debuggable = HaxeTestRunConfigurations.isDebugSupported(project, buildFilePath);
    e.getPresentation().setVisible(true);
    e.getPresentation().setEnabled(debuggable);
    // set in BOTH branches: the presentation is reused across selections, so
    // a hint set once would stick to every later, enabled selection
    e.getPresentation().setDescription(
      debuggable ? null : HaxeBundle.message("haxe.test.debug.unsupported.target"));
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
