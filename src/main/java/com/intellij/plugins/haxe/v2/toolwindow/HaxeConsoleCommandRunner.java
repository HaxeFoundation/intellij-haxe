package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCommandNotifications;
import com.intellij.plugins.haxe.v2.buildtools.HaxeProjectTrust;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolCommandLines;
import com.intellij.plugins.haxe.v2.buildtools.HaxeUnsavedDocuments;
import icons.HaxeIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Runs a tool command with its output attached to a console in the Run tool window,
 * so compile errors and long-running processes are fully visible (the console picks
 * up the plugin's error filters, giving clickable file:line links). Must be called
 * on the EDT. Used by the tool window's execute button, compile command and tool
 * rows, and the file tool actions.
 */
public final class HaxeConsoleCommandRunner {

  private HaxeConsoleCommandRunner() {
  }

  public static void run(@NotNull Project project,
                         @NotNull String presentableName,
                         @NotNull List<String> command,
                         @Nullable String workDirectory) {
    run(project, presentableName, command, workDirectory, null);
  }

  /** Like {@link #run(Project, String, List, String)}; {@code onTerminated} runs after process exit (any thread). */
  public static void run(@NotNull Project project,
                         @NotNull String presentableName,
                         @NotNull List<String> command,
                         @Nullable String workDirectory,
                         @Nullable Runnable onTerminated) {
    // the command comes from the project's build configuration - project code
    if (!HaxeProjectTrust.confirmForAction(project, HaxeBundle.message("haxe.trust.action.execute.command"))) {
      return;
    }
    HaxeUnsavedDocuments.saveAll();
    String effectiveWorkDirectory = workDirectory != null ? workDirectory : project.getBasePath();
    GeneralCommandLine commandLine = HaxeToolCommandLines.interactive(command, effectiveWorkDirectory);
    try {
      KillableColoredProcessHandler processHandler = new KillableColoredProcessHandler(commandLine);
      ProcessTerminatedListener.attach(processHandler);
      if (onTerminated != null) {
        processHandler.addProcessListener(new ProcessListener() {
          @Override
          public void processTerminated(@NotNull ProcessEvent event) {
            onTerminated.run();
          }
        });
      }

      // the process handler itself prints the command line on startNotify
      ConsoleView console = TextConsoleBuilderFactory.getInstance()
        .createBuilder(project)
        .getConsole();

      console.attachToProcess(processHandler);

      RunContentDescriptor descriptor =
        new RunContentDescriptor(console, processHandler, console.getComponent(), presentableName, HaxeIcons.HAXE_LOGO);
      RunContentManager.getInstance(project).showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);

      processHandler.startNotify();
    }
    catch (ExecutionException e) {
      String title = HaxeBundle.message("haxe.command.runner.failed", presentableName);
      HaxeCommandNotifications.notify(project, title, StringUtil.notNullize(e.getMessage()), NotificationType.ERROR);
    }
  }
}
