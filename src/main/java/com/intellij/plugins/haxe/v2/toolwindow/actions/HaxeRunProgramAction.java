package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowLaunches;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch;
import com.intellij.plugins.haxe.v2.runconfig.HaxeDebugSupport;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProgramNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Run / Debug on the Build &amp; run tree row and on a launching action row:
 * starts the row's visible run configuration, with the compile attached as a
 * before-launch step. A plain action row is untouched — compiling and running
 * the output are separate concepts.
 */
public final class HaxeRunProgramAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;
  private final boolean debug;

  public HaxeRunProgramAction(@NotNull HaxeToolWindowPanel panel, boolean debug) {
    super(HaxeBundle.message(debug ? "haxe.toolwindow.debug.action" : "haxe.toolwindow.run.action"),
          null,
          debug ? AllIcons.Actions.StartDebugger : AllIcons.Actions.Execute);
    this.panel = panel;
    this.debug = debug;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    Executor executor = debug ? DefaultDebugExecutor.getDebugExecutorInstance()
                              : DefaultRunExecutor.getRunExecutorInstance();
    switch (panel.getSelectedUserObject()) {
      case ProgramNode programNode -> HaxeToolWindowLaunches.runProgram(project, programNode, executor);
      case ActionNode actionNode when actionNode.launch() != null -> HaxeToolWindowLaunches.runAction(project, actionNode, executor);
      case null, default -> { }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    // the Debug variant shows only for targets a debugger lane exists for -
    // neko, for one, runs but cannot be debugged
    e.getPresentation().setEnabledAndVisible(applicable(e.getProject(), panel.getSelectedUserObject()));
  }

  private boolean applicable(@Nullable Project project, @Nullable Object selected) {
    if (project == null) return false;
    if (selected instanceof ActionNode actionNode && actionNode.launch() instanceof Launch.Existing existing) {
      return !debug || HaxeToolWindowLaunches.canDebug(project, existing.configurationName());
    }
    HaxeLaunchKind kind = launchKindOf(selected);
    return kind != null && (!debug || HaxeDebugSupport.supportsProgramDebug(kind.target()));
  }

  /** The kind a Build & run row or a created-configuration action row launches through; null for every other row. */
  @Nullable
  static HaxeLaunchKind launchKindOf(@Nullable Object selected) {
    return switch (selected) {
      case ProgramNode programNode -> programNode.kind();
      case ActionNode actionNode -> actionNode.launchKind();
      case null, default -> null;
    };
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
