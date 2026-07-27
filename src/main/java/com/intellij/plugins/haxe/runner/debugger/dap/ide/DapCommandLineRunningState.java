package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Plain (non-debug) execution of a prepared command line, with its output in
 * the console — the Run half of every DAP-debugger configuration.
 */
public class DapCommandLineRunningState extends CommandLineState {
  private final Project project;
  private final GeneralCommandLine commandLine;

  public DapCommandLineRunningState(ExecutionEnvironment env, Project project, GeneralCommandLine commandLine) {
    super(env);
    this.project = project;
    this.commandLine = commandLine;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(project));
    return new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
  }
}
