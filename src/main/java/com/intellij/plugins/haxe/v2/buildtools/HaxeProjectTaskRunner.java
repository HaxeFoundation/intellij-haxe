package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.task.ModuleBuildTask;
import com.intellij.task.ProjectTask;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskRunner;
import com.intellij.task.TaskRunnerResults;
import com.intellij.util.concurrency.AppExecutorUtil;
import icons.HaxeIcons;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Makes the IDE's Build Project / Build Module actions run the modules' configured
 * build commands (Compilation | Build command in the Haxe tool window). Modules
 * without a build command are left to the platform's default runner. Commands run
 * sequentially in one "Haxe Build" console; the first failure aborts the build.
 */
@CustomLog
public final class HaxeProjectTaskRunner extends ProjectTaskRunner {

  @Override
  public boolean canRun(@NotNull Project project, @NotNull ProjectTask projectTask, @Nullable ProjectTaskContext context) {
    if (!(projectTask instanceof ModuleBuildTask moduleTask)) return false;
    Module module = moduleTask.getModule();
    if (module.isDisposed()) return false;
    return HaxeEnvironmentStore.getInstance(project).getCompileCommand(module.getName()) != null;
  }

  @Override
  public @NotNull Promise<Result> run(@NotNull Project project,
                                      @NotNull ProjectTaskContext context,
                                      ProjectTask @NotNull ... tasks) {
    AsyncPromise<Result> promise = new AsyncPromise<>();

    Set<String> containerIds = new LinkedHashSet<>();
    for (ProjectTask task : tasks) {
      if (task instanceof ModuleBuildTask moduleTask) {
        containerIds.add(moduleTask.getModule().getName());
      }
    }
    if (containerIds.isEmpty()) {
      promise.setResult(TaskRunnerResults.SUCCESS);
      return promise;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) {
        promise.setResult(TaskRunnerResults.ABORTED);
        return;
      }
      // the build commands come from the project's configuration and the
      // compile runs macros - project code
      if (!HaxeProjectTrust.confirmForAction(project, HaxeBundle.message("haxe.trust.action.build"))) {
        promise.setResult(TaskRunnerResults.ABORTED);
        return;
      }
      ConsoleView console = TextConsoleBuilderFactory.getInstance()
        .createBuilder(project)
        .getConsole();
      String title = HaxeBundle.message("haxe.build.console.title");
      RunContentDescriptor descriptor = new RunContentDescriptor(console, null, console.getComponent(), title, HaxeIcons.HAXE_LOGO);
      RunContentManager.getInstance(project)
        .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);

      AppExecutorUtil.getAppExecutorService().execute(() -> promise.setResult(buildAll(project, containerIds, console)));
    });
    return promise;
  }

  @NotNull
  private static Result buildAll(@NotNull Project project, @NotNull Set<String> containerIds, @NotNull ConsoleView console) {
    HaxeUnsavedDocuments.saveAll();
    for (String containerId : containerIds) {
      if (!buildContainer(project, containerId, console)) {
        return TaskRunnerResults.FAILURE;
      }
    }
    console.print(HaxeBundle.message("haxe.build.console.done") + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    return TaskRunnerResults.SUCCESS;
  }

  /** Runs the container's build command with its output in the console; false when it failed. */
  private static boolean buildContainer(@NotNull Project project, @NotNull String containerId, @NotNull ConsoleView console) {
    HaxeCompileCommands.Resolved resolved = HaxeReadActions.compute(() -> HaxeCompileCommands.resolve(project, containerId));
    if (resolved == null) {
      // canRun saw a command, but it may have gone stale since - not an error
      return true;
    }
    List<String> command = HaxeCompileCommands.connectIfEnabled(project, containerId, resolved.connectEligible(), resolved.command());
    console.print("[" + containerId + "] " + String.join(" ", command) + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    try {
      GeneralCommandLine commandLine = new GeneralCommandLine(command)
        .withWorkDirectory(resolved.workDirectory())
        .withEnvironment(LimeProjects.commandEnvironment(command));
      ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess();
      if (!output.getStdout().isEmpty()) {
        console.print(output.getStdout(), ConsoleViewContentType.NORMAL_OUTPUT);
      }
      if (!output.getStderr().isEmpty()) {
        console.print(output.getStderr(), ConsoleViewContentType.ERROR_OUTPUT);
      }
      if (output.getExitCode() == 0) return true;

      String failure = HaxeBundle.message("haxe.build.console.failed", containerId, output.getExitCode());
      console.print(failure + "\n", ConsoleViewContentType.ERROR_OUTPUT);
      return false;
    }
    catch (ExecutionException e) {
      String failure = HaxeBundle.message("haxe.build.console.start.failed", containerId, e.getMessage());
      console.print(failure + "\n", ConsoleViewContentType.ERROR_OUTPUT);
      return false;
    }
  }
}
