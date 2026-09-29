package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ColoredProcessHandler} for the launched app/debuggee: such a
 * process runs long and prints sparsely, and the default output reader
 * busy-polls it (the platform logs a warning naming this override).
 */
public class MostlySilentColoredProcessHandler extends ColoredProcessHandler {

  public MostlySilentColoredProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
    super(commandLine.createProcess(), commandLine.getCommandLineString());
  }

  @Override
  protected @NotNull BaseOutputReader.Options readerOptions() {
    return BaseOutputReader.Options.forMostlySilentProcess();
  }
}
