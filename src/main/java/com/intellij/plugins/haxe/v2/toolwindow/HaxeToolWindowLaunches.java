package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.runconfig.HaxeConfigurationLaunches;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSystem;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionConfigurationFactory;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionRunConfiguration;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileScanner;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.Launch;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind.Parameter;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ActionNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.EnvCompileCommandNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ProgramNode;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.ToolNode;
import com.intellij.util.PathUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
          HaxeConsoleCommandRunner.run(project, compileCommand.containerId(), compileCommand.display(), command,
                                       compileCommand.workDirectory());
        }
      });
    });
  }

  public static void runAction(@NotNull Project project, @NotNull ActionNode actionNode) {
    runAction(project, actionNode, DefaultRunExecutor.getRunExecutorInstance());
  }

  /**
   * Runs an action row under the given executor: a plain action runs its
   * command in the Run console (callers pass only the Run executor for it); a
   * launching action starts its run configuration, created or existing, with
   * the command as the build step.
   */
  public static void runAction(@NotNull Project project, @NotNull ActionNode actionNode, @NotNull Executor executor) {
    // a bare trigger of an existing configuration and a chain of other actions need no command of their own
    boolean runsSomething = !actionNode.command().isEmpty()
                            || !actionNode.runBefore().isEmpty()
                            || actionNode.launch() instanceof Launch.Existing;
    if (!runsSomething) return;
    RunnerAndConfigurationSettings settings = actionNode.launch() != null
                                              ? launchingActionConfiguration(project, actionNode)
                                              : actionConfiguration(project, actionNode);
    if (settings != null) {
      HaxeConfigurationLaunches.runSelected(project, settings, executor);
    }
  }

  /** A launching action row under a profiler entry's executor; see {@link #profileProgram}. */
  public static void profileAction(@NotNull Project project, @NotNull ActionNode actionNode, @NotNull Executor executor) {
    RunnerAndConfigurationSettings settings = launchingActionConfiguration(project, actionNode);
    if (settings != null) {
      profile(project, settings, executor);
    }
  }

  // TODO: run tools through a run configuration like action rows, so they land in the run dropdown
  public static void runTool(@NotNull Project project, @NotNull ToolNode toolNode) {
    if (toolNode.command().isEmpty()) return;
    // a raw tree double-click dispatches on the EDT without the write-intent
    // lock the runner's save-all needs; invokeLater re-enters with it
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!project.isDisposed()) {
        HaxeConsoleCommandRunner.run(project, toolNode.containerId(), toolNode.name(), toolNode.command(), toolNode.workDirectory());
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
    RunnerAndConfigurationSettings settings = programConfiguration(project, programNode);
    if (settings != null) {
      HaxeConfigurationLaunches.runSelected(project, settings, executor);
    }
  }

  /**
   * The "compile &amp; run" row under a profiler entry's executor. A
   * configuration that is not ready to profile (a non-Chromium browser, no
   * compiled executable, no AIR descriptor) has no runner, and launching it
   * anyway would only earn the platform's generic "Cannot find runner"
   * balloon — so its editor opens instead, and the launch follows once the
   * configuration is completed.
   */
  public static void profileProgram(@NotNull Project project, @NotNull ProgramNode programNode, @NotNull Executor executor) {
    RunnerAndConfigurationSettings settings = programConfiguration(project, programNode);
    if (settings != null) {
      profile(project, settings, executor);
    }
  }

  private static void profile(@NotNull Project project, @NotNull RunnerAndConfigurationSettings settings, @NotNull Executor executor) {
    if (DumbService.isDumb(project)) {
      String message = HaxeBundle.message("haxe.toolwindow.profile.indexing");
      DumbService.getInstance(project).showDumbModeNotificationForAction(message, null);
      return;
    }
    if (!isProfilingReady(settings)) {
      String title = profileEditorTitle(settings);
      boolean completed = RunDialog.editConfiguration(project, settings, title, executor);
      if (!completed || !isProfilingReady(settings)) return;
    }
    HaxeConfigurationLaunches.runSelected(project, settings, executor);
  }

  private static boolean isProfilingReady(@NotNull RunnerAndConfigurationSettings settings) {
    return settings.getConfiguration() instanceof HaxeProfilableRunConfiguration configuration
           && configuration.isProfilingReady();
  }

  /** The editor's title names what the lane's readiness check is missing. */
  private static String profileEditorTitle(@NotNull RunnerAndConfigurationSettings settings) {
    if (!(settings.getConfiguration() instanceof HaxeProfilableRunConfiguration configuration)) {
      return HaxeBundle.message("haxe.toolwindow.profile.edit.title");
    }
    return switch (configuration.profilingLane()) {
      case JS -> HaxeBundle.message("haxe.toolwindow.profile.edit.title.js");
      case HXCPP -> HaxeBundle.message("haxe.toolwindow.profile.edit.title.hxcpp");
      case FLASH -> HaxeBundle.message("haxe.toolwindow.profile.edit.title.flash");
      case HASHLINK -> HaxeBundle.message("haxe.toolwindow.profile.edit.title");
    };
  }

  /**
   * The program row's run configuration (created on first use), or null —
   * with the reason notified — when the build file belongs to no module.
   */
  @Nullable
  private static RunnerAndConfigurationSettings programConfiguration(@NotNull Project project, @NotNull ProgramNode programNode) {
    Module module = moduleOf(project, programNode.buildFile().file());
    if (module == null) return null;

    String buildAction = HaxeBuildSystem.of(programNode.buildFile().type()).defaultBuildActionName();
    return HaxeProgramLaunches.findOrCreate(project, module, programNode.buildFile(), programNode.kind(),
                                            programNode.targetOutput(), buildAction, Map.of(), true);
  }

  /**
   * A launching action's run configuration (created on first use, or the
   * existing one it names), or null - with the reason notified - when the file
   * belongs to no module, produces no launchable output, or the action names an
   * unknown kind or a configuration that no longer exists.
   */
  @Nullable
  private static RunnerAndConfigurationSettings launchingActionConfiguration(@NotNull Project project, @NotNull ActionNode actionNode) {
    HaxeBuildFile buildFile = ReadAction.computeBlocking(() -> HaxeBuildFileScanner.findBuildFile(project, actionNode.ownerId()));
    if (buildFile == null) return null;
    return switch (actionNode.launch()) {
      case Launch.Create create -> createdConfiguration(project, actionNode, buildFile, create);
      case Launch.Existing existing -> existingConfiguration(project, actionNode, buildFile, existing);
      case null -> null;
    };
  }

  @Nullable
  private static RunnerAndConfigurationSettings createdConfiguration(@NotNull Project project,
                                                                    @NotNull ActionNode actionNode,
                                                                    @NotNull HaxeBuildFile buildFile,
                                                                    @NotNull Launch.Create launch) {
    HaxeLaunchKind kind = actionNode.launchKind();
    if (kind == null) {
      notifyUser(project, HaxeBundle.message("haxe.toolwindow.action.launch.unknown.kind", actionNode.name(), launch.kindId()));
      return null;
    }
    if (actionNode.launchOutput() == null) {
      notifyUser(project, HaxeBundle.message("haxe.toolwindow.program.unsupported", buildFile.file().getName()));
      return null;
    }
    Module module = moduleOf(project, buildFile.file());
    if (module == null) return null;

    Map<Parameter, String> overrides = Parameter.fromKeys(launch.parameters());
    return HaxeProgramLaunches.findOrCreate(project, module, buildFile, kind, actionNode.launchOutput(), actionNode.name(),
                                            overrides, launch.injectDebugArguments());
  }

  /** The named configuration, given the action's command as its build step when the action has one. */
  @Nullable
  private static RunnerAndConfigurationSettings existingConfiguration(@NotNull Project project,
                                                                     @NotNull ActionNode actionNode,
                                                                     @NotNull HaxeBuildFile buildFile,
                                                                     @NotNull Launch.Existing launch) {
    RunnerAndConfigurationSettings settings = RunManager.getInstance(project).findConfigurationByName(launch.configurationName());
    if (settings == null) {
      String message = HaxeBundle.message("haxe.toolwindow.action.launch.missing.configuration", actionNode.name(), launch.configurationName());
      notifyUser(project, message);
      return null;
    }
    if (!actionNode.command().isEmpty()) {
      HaxeProgramLaunches.ensureBuildStep(settings.getConfiguration(), buildFile, actionNode.name(), launch.injectDebugArguments());
    }
    return settings;
  }

  /** Whether the named existing configuration has a debug runner; false when it does not exist. */
  public static boolean canDebug(@NotNull Project project, @NotNull String configurationName) {
    RunnerAndConfigurationSettings settings = RunManager.getInstance(project).findConfigurationByName(configurationName);
    return settings != null && ProgramRunner.getRunner(DefaultDebugExecutor.EXECUTOR_ID, settings.getConfiguration()) != null;
  }

  /** The module owning the build file, or null with the reason notified. */
  @Nullable
  private static Module moduleOf(@NotNull Project project, @NotNull VirtualFile file) {
    // the file index needs a read action - the EDT has no implicit read access
    Module module = ReadAction.computeBlocking(() -> ProjectFileIndex.getInstance(project).getModuleForFile(file));
    if (module == null) {
      notifyUser(project, HaxeBundle.message("haxe.toolwindow.program.no.module", file.getName()));
    }
    return module;
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
