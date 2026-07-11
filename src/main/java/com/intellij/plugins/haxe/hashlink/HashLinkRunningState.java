package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Plain (non-debug) execution of a compiled HashLink program:
 * {@code hl <program.hl>}. Default working directory is the program's
 * directory so relative resource loading behaves like a manual launch.
 */
public class HashLinkRunningState extends CommandLineState {
  private final Module module;
  private final Path hlExecutable;
  private final Path hlProgram;
  private final @Nullable Path workingDirectory;

  public HashLinkRunningState(ExecutionEnvironment env, Module module,
                              Path hlExecutable, Path hlProgram, @Nullable Path workingDirectory) {
    super(env);
    this.module = module;
    this.hlExecutable = hlExecutable;
    this.hlProgram = hlProgram;
    this.workingDirectory = workingDirectory;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    Path workDir = workingDirectory != null ? workingDirectory : hlProgram.getParent();
    GeneralCommandLine commandLine = new GeneralCommandLine()
      .withExePath(hlExecutable.toString())
      .withParameters(hlProgram.toString())
      .withWorkDirectory(workDir != null ? workDir.toString() : null);

    setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(module.getProject()));
    return new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
  }
}
