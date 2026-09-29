package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.profiler.HaxeJsProfilerCapture;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.MostlySilentColoredProcessHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;

/**
 * A profiling browser run: content served like plain Run, but the browser
 * is a Chromium CHILD the IDE launches itself — fresh profile directory,
 * DevTools port open — so the V8 sampling profiler can be driven over CDP
 * (an already-running browser instance would ignore the port). The
 * capture collects in one-second segments streamed into the session file,
 * so the live view follows the run; STOP collects the final segment before
 * killing the browser, and a window closed by hand keeps every segment
 * already collected.
 */
public class BrowserProfilingState implements RunProfileState {

  /** The streamed V8 session, persisted beside the served content. */
  public static final String PROFILE_FILE_NAME = "jsprofile.hxtsession";

  private final BrowserRunConfiguration configuration;
  private final int samplingIntervalUs;

  public BrowserProfilingState(@NotNull BrowserRunConfiguration configuration, int samplingIntervalUs) {
    this.configuration = configuration;
    this.samplingIntervalUs = samplingIntervalUs;
  }

  @Override
  public @Nullable ExecutionResult execute(Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
    ContentHttpServer server = null;
    boolean handedOver = false;
    try {
      String url;
      Path contentRoot = configuration.resolveContentRootOrNull();
      if (configuration.isServeContent()) {
        if (contentRoot == null) {
          throw new ExecutionException(HaxeDebuggerBundle.message("browser.runner.no.content.root"));
        }
        server = createContentServer(contentRoot);
        url = server.getBaseUrl();
      }
      else {
        url = configuration.getUrl();
      }

      int debugPort = freePort();
      GeneralCommandLine commandLine = chromiumCommandLine(url, debugPort);
      HaxeJsProfilerCapture.Handle capture =
        HaxeJsProfilerCapture.startCapture(configuration.getProject(), configuration.getName(), sessionPath(contentRoot),
                                           debugPort, samplingIntervalUs, contentRoot, url);
      ProcessHandler handler = profiledBrowserHandler(commandLine, capture, server);

      ConsoleView console = TextConsoleBuilderFactory.getInstance()
        .createBuilder(configuration.getProject())
        .getConsole();
      console.attachToProcess(handler);
      handedOver = true;
      return new DefaultExecutionResult(console, handler);
    }
    finally {
      if (server != null && !handedOver) {
        server.close();
      }
    }
  }

  /** The streamed session file: beside the served content, else in the project directory. */
  @NotNull
  private Path sessionPath(@Nullable Path contentRoot) {
    return contentRoot != null
           ? contentRoot.resolve(PROFILE_FILE_NAME)
           : Path.of(configuration.getProject().getBasePath(), PROFILE_FILE_NAME);
  }

  /** The browser process, ending the capture on Stop and releasing the content server when the browser exits. */
  @NotNull
  private static ProcessHandler profiledBrowserHandler(@NotNull GeneralCommandLine commandLine,
                                                       @Nullable HaxeJsProfilerCapture.Handle capture,
                                                       @Nullable ContentHttpServer server) throws ExecutionException {
    ProcessHandler handler = new MostlySilentColoredProcessHandler(commandLine) {
      @Override
      protected void destroyProcessImpl() {
        // Stop = end of the capture: collect while the browser still lives
        if (capture != null) capture.finishCapture();
        super.destroyProcessImpl();
      }
    };
    handler.addProcessListener(new ProcessListener() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        // no-op after a Stop's collection; a browser closed by hand
        // finalizes the session with the segments already streamed
        if (capture != null) capture.connectionLost();
        if (server != null) server.close();
      }
    });
    return handler;
  }

  /**
   * A fresh Chromium child: its own profile directory (an existing instance
   * would swallow the launch and ignore the DevTools port) and no
   * first-run interruptions.
   */
  private GeneralCommandLine chromiumCommandLine(String url, int debugPort) throws ExecutionException {
    WebBrowser browser = DebugBrowser.resolve(configuration.getBrowserId());
    if (browser == null || DebugBrowser.familyOf(browser) != BrowserRunConfiguration.BrowserFamily.CHROMIUM) {
      throw new ExecutionException(HaxeDebuggerBundle.message("browser.runner.profiling.needs.chromium"));
    }
    Path executable = DebugBrowser.executableOf(browser);
    if (executable == null) {
      throw new ExecutionException(HaxeDebuggerBundle.message("browser.runner.browser.no.exe", browser.getName()));
    }
    Path profileDirectory;
    try {
      profileDirectory = FileUtil.createTempDirectory("haxe-js-profile", null, true).toPath();
    }
    catch (IOException e) {
      throw new ExecutionException(HaxeDebuggerBundle.message("browser.runner.server.failed", e.getMessage()), e);
    }
    return new GeneralCommandLine()
      .withExePath(executable.toString())
      .withParameters("--remote-debugging-port=" + debugPort,
                      "--user-data-dir=" + profileDirectory,
                      "--no-first-run",
                      "--no-default-browser-check",
                      url);
  }

  private ContentHttpServer createContentServer(Path contentRoot) throws ExecutionException {
    try {
      return new ContentHttpServer(contentRoot);
    }
    catch (IOException e) {
      throw new ExecutionException(HaxeDebuggerBundle.message("browser.runner.server.failed", e.getMessage()), e);
    }
  }

  private static int freePort() throws ExecutionException {
    try (ServerSocket probe = new ServerSocket(0)) {
      return probe.getLocalPort();
    }
    catch (IOException e) {
      throw new ExecutionException(HaxeDebuggerBundle.message("browser.runner.server.failed", e.getMessage()), e);
    }
  }
}
