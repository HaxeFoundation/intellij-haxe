package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowLaunches;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.HaxeProfilerExecutorSupport;
import com.intellij.plugins.haxe.profiler.HaxeProfilerExecutorSupport.ProfilerEntry;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProgramNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

/**
 * Profile on the "compile &amp; run" tree row: launches the build file's
 * program through its lane's IU profiler entry (compile attached as a
 * before-launch step, with the profiler's compile additions where the lane
 * needs them). One registered entry runs directly and names itself on the
 * action; several open a chooser; none — the profiler module missing, the
 * target without a lane, or every entry deleted in settings — disables it.
 */
public final class HaxeProfileProgramAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeProfileProgramAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.profile.action"), null, AllIcons.Actions.Profile);
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null || !(panel.getSelectedUserObject() instanceof ProgramNode programNode)) return;
    List<ProfilerEntry> entries = profilerEntriesFor(programNode);
    if (entries.isEmpty()) return; // raced a settings change - the action was disabled a moment ago
    if (entries.size() == 1) {
      HaxeToolWindowLaunches.runProgram(project, programNode, entries.getFirst().executor());
      return;
    }
    BaseListPopupStep<ProfilerEntry> step =
      new BaseListPopupStep<>(HaxeBundle.message("haxe.toolwindow.profile.action"), entries) {
        @Override
        public @NotNull String getTextFor(ProfilerEntry entry) {
          return entry.displayName();
        }

        @Override
        public Icon getIconFor(ProfilerEntry entry) {
          return AllIcons.Actions.Profile;
        }

        @Override
        public @Nullable PopupStep<?> onChosen(ProfilerEntry entry, boolean finalChoice) {
          return doFinalStep(() -> HaxeToolWindowLaunches.runProgram(project, programNode, entry.executor()));
        }
      };
    JBPopupFactory.getInstance()
      .createListPopup(step)
      .showInBestPositionFor(e.getDataContext());
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    List<ProfilerEntry> entries = panel.getSelectedUserObject() instanceof ProgramNode programNode
                                  ? profilerEntriesFor(programNode)
                                  : List.of();
    e.getPresentation().setEnabledAndVisible(!entries.isEmpty());
    e.getPresentation().setText(entries.size() == 1
                                ? HaxeBundle.message("haxe.toolwindow.profile.action.with", entries.getFirst().displayName())
                                : HaxeBundle.message("haxe.toolwindow.profile.action"));
  }

  private static List<ProfilerEntry> profilerEntriesFor(ProgramNode programNode) {
    HaxeProfilableRunConfiguration.Lane lane =
      HaxeProgramLaunches.profilingLaneFor(programNode.target(), programNode.targetOutput());
    return lane == null ? List.of() : HaxeProfilerExecutorSupport.profilerExecutors(lane);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
