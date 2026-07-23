package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the browser configuration (experimental). Keys on
 * {@link BrowserRunConfiguration} only. Unlike the sibling runners it spawns
 * NO debuggee — the adapter launches the browser itself during the launch
 * request — so the command line is null and the session runs on a no-op
 * process handler. All heavy lifting (adapter download, node checks, content
 * server) happens inside {@link BrowserDebugBackend#connect()} off the EDT.
 */
public class BrowserDebugRunner extends DapDebugRunnerBase<BrowserRunConfiguration, BrowserDebugBackend> {
  public static final String RUNNER_ID = "HaxeBrowserDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  protected Class<BrowserRunConfiguration> configurationClass() {
    return BrowserRunConfiguration.class;
  }

  @Override
  protected void validate(BrowserRunConfiguration configuration) throws ExecutionException {
    // same checks as the editor's red banner, surfaced at run time too
    try {
      configuration.checkConfiguration();
    } catch (RuntimeConfigurationException e) {
      throw new ExecutionException(e.getMessageHtml().toString());
    }
  }

  @Override
  protected BrowserDebugBackend createBackend(BrowserRunConfiguration configuration) {
    Path contentRoot = configuration.resolveContentRootOrNull();
    return new BrowserDebugBackend(
      configuration.getBrowserFamily(),
      configuration.getNodePath(),
      configuration.getBrowserExecutablePath(),
      configuration.isServeContent(),
      contentRoot,
      configuration.getUrl());
  }

  @Override
  protected GeneralCommandLine createCommandLine(BrowserRunConfiguration configuration, BrowserDebugBackend backend) {
    return null; // the adapter launches the browser; nothing to spawn here
  }
}
