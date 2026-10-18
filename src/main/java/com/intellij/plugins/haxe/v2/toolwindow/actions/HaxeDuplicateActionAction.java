package com.intellij.plugins.haxe.v2.toolwindow.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSystem;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCustomCommands;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowPanel;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.BuildFileRow;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProgramNode;
import com.intellij.plugins.haxe.v2.toolwindow.ui.HaxeCustomActionDialog;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Tree context menu on any action row or the Build &amp; run row: opens the
 * Add Action dialog pre-filled with a copy of the row - a generated action's
 * resolved command and work directory, a custom action's stored values, the
 * Build &amp; run row's default build command with its launch - so a built-in
 * row becomes a custom one to change. A copied command names the SDK's haxe
 * or haxelib as {@code ${haxe}} or {@code ${haxelib}} and the selected target
 * as {@code ${target}}.
 */
public final class HaxeDuplicateActionAction extends DumbAwareAction {

  private final HaxeToolWindowPanel panel;

  public HaxeDuplicateActionAction(@NotNull HaxeToolWindowPanel panel) {
    super(HaxeBundle.message("haxe.toolwindow.duplicate.action"));
    this.panel = panel;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    BuildFileRow row = panel.getSelectedBuildFileRowAncestor();
    if (project == null || row == null) return;
    CustomAction source = templateOf(project, row.buildFile(), panel.getSelectedUserObject());
    if (source == null) return;
    Set<String> taken = HaxeCustomActionDialog.takenNames(project, row.buildFile(), null);
    CustomAction template = source.withName(copyName(source.name(), taken));

    String launchOutput = launchOutputOf(panel.getSelectedUserObject());
    HaxeCustomActionDialog dialog = HaxeCustomActionDialog.forDuplicate(project, row.buildFile(), launchOutput, template);
    if (dialog.showAndGet()) {
      HaxeCustomActionsStore.getInstance(project).addAction(row.buildFile().file().getPath(), dialog.getAction());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Object selected = panel.getSelectedUserObject();
    e.getPresentation().setEnabledAndVisible(selected instanceof ActionNode || selected instanceof ProgramNode);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  @Nullable
  private static CustomAction templateOf(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @Nullable Object selected) {
    return switch (selected) {
      case ActionNode actionNode when actionNode.custom() -> actionNode.storedAction(project);
      case ActionNode actionNode -> copyOfGenerated(project, buildFile, actionNode);
      case ProgramNode programNode -> copyOfBuildAndRun(project, programNode);
      case null, default -> null;
    };
  }

  @Nullable
  private static String launchOutputOf(@Nullable Object selected) {
    return switch (selected) {
      case ActionNode actionNode -> actionNode.launchOutput();
      case ProgramNode programNode -> programNode.targetOutput();
      case null, default -> null;
    };
  }

  /** The resolved command line as the row runs it; the work directory stays blank when it is the build file's own. */
  @NotNull
  private static CustomAction copyOfGenerated(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull ActionNode actionNode) {
    String command = asTypedCommand(project, buildFile, actionNode.command());
    return new CustomAction(actionNode.name(), command, workDirectoryOverride(project, buildFile, actionNode.workDirectory()), null);
  }

  /**
   * The default build action's command with a launch of the row's kind, every
   * parameter at its default. The interpreter row's command runs the program
   * itself, so its copy is a plain action.
   */
  @Nullable
  private static CustomAction copyOfBuildAndRun(@NotNull Project project, @NotNull ProgramNode programNode) {
    List<String> command = ReadAction.computeBlocking(() -> defaultBuildCommand(project, programNode.buildFile()));
    if (command == null) return null;

    HaxeLaunchKind kind = programNode.kind();
    Launch launch = kind.compilesBeforeLaunch() ? new Launch.Create(kind.name(), Map.of(), true) : null;
    String name = HaxeBundle.message("haxe.toolwindow.node.program");
    return new CustomAction(name, asTypedCommand(project, programNode.buildFile(), command), "", launch);
  }

  /** The file's default build action as its Build row runs it; resolving it reads the module index and the file. */
  @Nullable
  private static List<String> defaultBuildCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    HaxeBuildSystem buildSystem = HaxeBuildSystem.of(buildFile.type());
    return buildSystem.actionCommand(project, environmentSdkOf(project, buildFile), buildFile, buildSystem.defaultBuildActionName());
  }

  /**
   * The command as typed text: its leading executable as {@code ${haxe}} or
   * {@code ${haxelib}} when it is the SDK's, the selected target as {@code ${target}}.
   */
  @NotNull
  private static String asTypedCommand(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @NotNull List<String> command) {
    if (command.isEmpty()) return "";
    String environmentSdk = ReadAction.computeBlocking(() -> environmentSdkOf(project, buildFile));
    String targetFlag = ReadAction.computeBlocking(() -> HaxeBuildSystem.of(buildFile.type()).selectedTargetFlag(project, buildFile));
    List<String> arguments = new ArrayList<>(HaxeCustomCommands.withTargetVariable(command, targetFlag));
    arguments.set(0, HaxeCustomCommands.toolVariableFor(project, environmentSdk, arguments.getFirst()));
    return ParametersListUtil.join(arguments);
  }

  /** The build file's container SDK; finding the container reads the module index. */
  @Nullable
  private static String environmentSdkOf(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    String containerId = HaxeContainers.containerIdFor(project, buildFile.file());
    return HaxeEnvironmentStore.getInstance(project).getSdkName(containerId);
  }

  @NotNull
  private static String workDirectoryOverride(@NotNull Project project, @NotNull HaxeBuildFile buildFile, @Nullable String workDirectory) {
    String fileWorkDirectory = HaxeBuildWorkDirectories.workDirectory(project, buildFile.file());
    return workDirectory == null || Objects.equals(workDirectory, fileWorkDirectory) ? "" : workDirectory;
  }

  /** "{@code name} (copy)", numbered "(copy 2)", "(copy 3)"... while the build file already uses the name. */
  @NotNull
  static String copyName(@NotNull String name, @NotNull Set<String> taken) {
    String candidate = HaxeBundle.message("haxe.toolwindow.duplicate.action.name", name);
    for (int number = 2; taken.contains(candidate); number++) {
      candidate = HaxeBundle.message("haxe.toolwindow.duplicate.action.name.numbered", name, number);
    }
    return candidate;
  }
}
