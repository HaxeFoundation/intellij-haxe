package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.runconfig.HaxeConfigurationLaunches;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionConfigurationFactory;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionRunConfiguration;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvCompileCommandNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProgramNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolNode;
import com.intellij.util.PathUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Launches from tree rows. Action and program rows run through run
 * configurations (created on first use, reused after), so they land in the
 * run configuration dropdown and can be rerun or debugged from the main UI -
 * the Gradle tool window pattern. The compile command and tool rows run in
 * a console.
 */
public final class HaxeToolWindowLaunches {

  private HaxeToolWindowLaunches() {
  }

  /**
   * Runs the container's configured compile command (no-op while unconfigured),
   * routed through the compilation server when enabled and applicable.
   */
  public static void runCompileCommand(@NotNull Project project, @NotNull EnvCompileCommandNode compileCommand) {
    if (compileCommand.command() == null) return;
    // connect injection may spawn the compilation server - keep process creation off the EDT
    AppExecutorUtil.getAppExecutorService().execute(() -> {
      List<String> command = HaxeCompileCommands.connectIfEnabled(
        project, compileCommand.containerId(), compileCommand.connectEligible(), compileCommand.command());
      ApplicationManager.getApplication().invokeLater(() -> {
        if (!project.isDisposed()) {
          HaxeConsoleCommandRunner.run(project, compileCommand.display(), command, compileCommand.workDirectory());
        }
      });
    });
  }

  public static void runAction(@NotNull Project project, @NotNull ActionNode actionNode) {
    if (actionNode.command().isEmpty()) return;
    HaxeConfigurationLaunches.runSelected(project, actionConfiguration(project, actionNode), DefaultRunExecutor.getRunExecutorInstance());
  }

  // TODO: run tools through a run configuration like action rows, so they land in the run dropdown
  public static void runTool(@NotNull Project project, @NotNull ToolNode toolNode) {
    if (toolNode.command().isEmpty()) return;
    // a raw tree double-click dispatches on the EDT without the write-intent
    // lock the runner's save-all needs; invokeLater re-enters with it
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!project.isDisposed()) {
        HaxeConsoleCommandRunner.run(project, toolNode.name(), toolNode.command(), toolNode.workDirectory());
      }
    });
  }

  /**
   * The "compile &amp; run" row: launches the program a build file produces
   * under the given executor (Run, Debug or a profiler entry), through the
   * target's visible run configuration (HashLink, Browser, …) whose
   * before-launch step compiles the file.
   */
  public static void runProgram(@NotNull Project project, @NotNull ProgramNode programNode, @NotNull Executor executor) {
    VirtualFile file = programNode.buildFile().file();
    // the file index needs a read action - the EDT has no implicit read access
    Module module = ReadAction.computeBlocking(() -> ProjectFileIndex.getInstance(project).getModuleForFile(file));
    if (module == null) {
      notifyUser(project, HaxeBundle.message("haxe.toolwindow.program.no.module", file.getName()));
      return;
    }

    RunnerAndConfigurationSettings settings = HaxeProgramLaunches.findOrCreate(
      project, module, programNode.buildFile(), programNode.target(), programNode.targetOutput());
    if (settings == null) {
      notifyUser(project, HaxeBundle.message("haxe.toolwindow.program.unsupported", file.getName()));
      return;
    }
    HaxeConfigurationLaunches.runSelected(project, settings, executor);
  }

  /** The action row's run configuration: the registered one, else a new one added to the RunManager. */
  @NotNull
  private static RunnerAndConfigurationSettings actionConfiguration(@NotNull Project project, @NotNull ActionNode actionNode) {
    RunManager runManager = RunManager.getInstance(project);
    RunnerAndConfigurationSettings existing = runManager.getAllSettings().stream()
      .filter(candidate -> isActionConfiguration(candidate, actionNode))
      .findFirst()
      .orElse(null);
    if (existing != null) return existing;

    String name = PathUtil.getFileName(actionNode.ownerId()) + " " + actionNode.name();
    ConfigurationFactory factory = HaxeRunConfigurationType.getInstance().getFactory(HaxeActionConfigurationFactory.class);
    RunnerAndConfigurationSettings settings = runManager.createConfiguration(name, factory);
    HaxeActionRunConfiguration configuration = (HaxeActionRunConfiguration)settings.getConfiguration();
    configuration.setBuildFilePath(actionNode.ownerId());
    configuration.setActionName(actionNode.name());
    runManager.addConfiguration(settings);
    return settings;
  }

  private static boolean isActionConfiguration(@NotNull RunnerAndConfigurationSettings candidate, @NotNull ActionNode actionNode) {
    return candidate.getConfiguration() instanceof HaxeActionRunConfiguration configuration
           && configuration.getBuildFilePath().equals(actionNode.ownerId())
           && configuration.getActionName().equals(actionNode.name());
  }

  private static void notifyUser(@NotNull Project project, @NotNull String message) {
    HaxeCommandNotifications.notify(project, message, NotificationType.WARNING);
  }
}
