package com.intellij.plugins.haxe.runner.nodejs;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.plugins.haxe.runner.nodejs.NodeDebugBackend;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Debug runner for the Node.js configuration: starts the script under
 * {@code node --inspect-brk=<port>}, which holds it before the first line,
 * and {@link NodeDebugBackend} attaches the js-debug adapter to that port and
 * releases the hold once breakpoints are installed. Breakpoints bind in the
 * .hx sources through the source maps a debug build writes.
 */
public class NodeDebugRunner extends DapDebugRunnerBase<NodeRunConfiguration, NodeDebugBackend> {
  public static final String RUNNER_ID = "HaxeNodeDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  protected Class<NodeRunConfiguration> configurationClass() {
    return NodeRunConfiguration.class;
  }

  @Override
  protected void validate(NodeRunConfiguration configuration) throws ExecutionException {
    configuration.requireModule();
    configuration.resolveExecutable();
  }

  @Override
  protected NodeDebugBackend createBackend(NodeRunConfiguration configuration) throws ExecutionException {
    Path node = configuration.resolveExecutable();
    Path workDirectory = configuration.resolveWorkingDirectory();
    String workDirectoryPath = workDirectory == null ? null : workDirectory.toString();
    return new NodeDebugBackend(node, findFreePort(), workDirectoryPath);
  }

  /** The plain run's command line with the inspector hold ahead of the script. */
  @Override
  protected GeneralCommandLine createCommandLine(NodeRunConfiguration configuration, NodeDebugBackend backend)
    throws ExecutionException {
    GeneralCommandLine commandLine = configuration.createCommandLine();
    commandLine.getParametersList().addAt(0, backend.inspectorArgument());
    return commandLine;
  }
}
