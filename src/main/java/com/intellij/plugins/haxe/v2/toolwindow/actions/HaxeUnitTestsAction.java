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
import org.jetbrains.annotations.Nullable;

/**
 * Tree context menu: runs or debugs the selection's unit tests - directly on the
 * "Run Unit Tests" row, or on a container row through its marked (or
 * convention-suggested) tests build file. Disabled with a hint while the
 * container has no tests file and, for Debug, while the tests build's target
 * has no debug lane.
 */
public final class HaxeUnitTestsAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;
  private final boolean debug;

  public HaxeUnitTestsAction(@NotNull HaxeToolWindowPanel panel, boolean debug) {
    super(HaxeBundle.message(debug ? "haxe.toolwindow.node.debug.unit.tests" : "haxe.toolwindow.node.run.unit.tests"),
          null,
          debug ? AllIcons.Actions.StartDebugger : AllIcons.RunConfigurations.TestState.Run);
    this.panel = panel;
    this.debug = debug;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    String buildFilePath = panel.resolveTestsPath(panel.getSelectedUserObject());
    if (buildFilePath == null) return;
    if (debug) {
      HaxeTestRunConfigurations.debug(project, buildFilePath);
    }
    else {
      HaxeTestRunConfigurations.run(project, buildFilePath);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    Object selection = panel.getSelectedUserObject();
    boolean relevant = selection instanceof TestRunNode || selection instanceof ModuleNode || selection instanceof ProjectNode;
    if (project == null || !relevant) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    String hint = disabledHint(project, panel.resolveTestsPath(selection));
    e.getPresentation().setVisible(true);
    e.getPresentation().setEnabled(hint == null);
    // set in BOTH branches: the presentation is reused across selections, so
    // a hint set once would stick to every later, enabled selection
    e.getPresentation().setDescription(hint);
  }

  /** Why the action cannot run: no tests file, or (Debug) a target without a debug lane; null when it can. */
  @Nullable
  private String disabledHint(@NotNull Project project, @Nullable String buildFilePath) {
    if (buildFilePath == null) return HaxeBundle.message("haxe.toolwindow.run.unit.tests.no.tests.file");
    if (debug && !HaxeTestRunConfigurations.isDebugSupported(project, buildFilePath)) {
      return HaxeBundle.message("haxe.test.debug.unsupported.target");
    }
    return null;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
