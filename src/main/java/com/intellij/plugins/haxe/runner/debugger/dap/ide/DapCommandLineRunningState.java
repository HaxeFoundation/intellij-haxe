package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Plain (non-debug) execution of a command line, with its output in the
 * console — the Run half of every DAP-debugger configuration.
 *
 * The command line is built lazily in {@link #startProcess()}: the platform
 * calls {@code getState()} BEFORE before-launch tasks run, so resolving the
 * program artifact there aborts a run whose compile step would have produced
 * it. Resolution must wait until the process actually starts.
 */
public class DapCommandLineRunningState extends CommandLineState {

  /** Builds the command line at process start; may fail with a user-readable error. */
  @FunctionalInterface
  public interface CommandLineSupplier {
    GeneralCommandLine get() throws ExecutionException;
  }

  private final Project project;
  private final CommandLineSupplier commandLineSupplier;

  public DapCommandLineRunningState(ExecutionEnvironment env, Project project, CommandLineSupplier commandLineSupplier) {
    super(env);
    this.project = project;
    this.commandLineSupplier = commandLineSupplier;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    GeneralCommandLine commandLine = commandLineSupplier.get();
    setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(project));
    return createProcessHandler(commandLine);
  }

  /** The handler the run tab attaches to; overridable for launches that need a different start (an elevated one). */
  @NotNull
  protected ProcessHandler createProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
    return new MostlySilentColoredProcessHandler(commandLine);
  }
}
