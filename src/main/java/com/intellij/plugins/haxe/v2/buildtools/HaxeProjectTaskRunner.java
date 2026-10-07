package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.task.ModuleBuildTask;
import com.intellij.task.ProjectTask;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskRunner;
import com.intellij.task.TaskRunnerResults;
import icons.HaxeIcons;
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
 * sequentially in one "Haxe Build" console, streaming while they run; the first
 * failure aborts the build, and so does the tab's Stop.
 */
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
      HaxeBuildChain chain = new HaxeBuildChain(project, List.copyOf(containerIds), promise);
      showBuildConsole(project, chain);
      // a task promise cancelled from outside ends the chain the way Stop does
      promise.onError(error -> chain.destroyProcess());
      chain.start();
    });
    return promise;
  }

  /** Opens the "Haxe Build" tab on the chain: its console streams the output, its Stop ends the chain. */
  private static void showBuildConsole(@NotNull Project project, @NotNull HaxeBuildChain chain) {
    ConsoleView console = TextConsoleBuilderFactory.getInstance()
      .createBuilder(project)
      .getConsole();
    console.attachToProcess(chain);
    String title = HaxeBundle.message("haxe.build.console.title");
    RunContentDescriptor descriptor = new RunContentDescriptor(console, chain, console.getComponent(), title, HaxeIcons.HAXE_LOGO);
    RunContentManager.getInstance(project)
      .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
    chain.startNotify();
  }
}
