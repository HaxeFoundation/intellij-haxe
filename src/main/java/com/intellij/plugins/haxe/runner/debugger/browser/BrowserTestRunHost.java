package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.client.DapEndpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.HostedTestRunSentinel;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.InitializedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.InitializeRequest;
import java.io.IOException;
import java.io.OutputStream;
import org.jetbrains.annotations.Nullable;

/**
 * The synthetic process behind a plain (non-debug) BROWSER-hosted test run:
 * there is no debuggee process — the tests run in a page the backend serves
 * and launches — so this handler orchestrates the backend, drives the child
 * session's minimal DAP handshake (initialize, fire-and-forget launch,
 * configurationDone on the initialized event; no breakpoints), and replays
 * the page's console — arriving as DAP output events — into its own output
 * stream, where the attached SM test console parses the protocol. The run
 * ends when the injected reporter's completion sentinel arrives (a page has
 * no exit code); Stop tears the browser and adapter down the same way.
 */
public final class BrowserTestRunHost extends ProcessHandler {

  private static final long REQUEST_TIMEOUT_MILLIS = 15_000;
  private static final long EVENT_POLL_MILLIS = 100;
  // a page that 404s or throws before the reporter loads produces neither
  // the sentinel nor a terminated event, and the run would spin until Stop;
  // output refreshes the deadline, so only sustained silence trips it, not
  // a long suite
  private static final long OUTPUT_STALL_MILLIS = 120_000;

  private final BrowserDebugBackend backend;
  private volatile boolean finished;

  BrowserTestRunHost(BrowserDebugBackend backend) {
    this.backend = backend;
  }

  @Override
  public void startNotify() {
    super.startNotify();
    Thread runner = new Thread(this::orchestrate, "Browser test run");
    runner.setDaemon(true);
    runner.start();
  }

  private void orchestrate() {
    try {
      DapEndpoint endpoint = backend.connect();
      backend.startBackgroundOutput(line -> notifyTextAvailable(line + "\n", ProcessOutputTypes.SYSTEM));

      InitializeRequest initialize = InitializeRequest.standard("chrome", true);
      initialize.getArguments().setClientName("IntelliJ Haxe");
      Response initialized = endpoint.sendRequest(initialize, REQUEST_TIMEOUT_MILLIS);
      if (!initialized.isSuccess()) {
        throw new IOException("the adapter rejected initialize: " + initialized.getMessage());
      }
      // js-debug answers launch only after configurationDone - fire and forget
      endpoint.sendRequestNoWait(backend.launchRequest());
      pumpEvents(endpoint);
    } catch (IOException e) {
      failRun(HaxeDebuggerBundle.message("browser.test.run.start.failed", e.getMessage()));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      failRun(HaxeDebuggerBundle.message("browser.test.run.interrupted"));
    }
  }

  private void pumpEvents(DapEndpoint endpoint) throws IOException, InterruptedException {
    long deadline = System.currentTimeMillis() + OUTPUT_STALL_MILLIS;
    while (!finished) {
      if (System.currentTimeMillis() > deadline) {
        failRun(HaxeDebuggerBundle.message("browser.test.run.stalled", String.valueOf(OUTPUT_STALL_MILLIS / 1000)));
        return;
      }
      Event event = endpoint.pollEvent(EVENT_POLL_MILLIS);
      switch (event) {
        case null -> { /* poll again */ }
        case InitializedEvent ignored -> endpoint.sendRequest(new ConfigurationDoneRequest(), REQUEST_TIMEOUT_MILLIS);
        case OutputEvent output -> {
          deadline = System.currentTimeMillis() + OUTPUT_STALL_MILLIS;
          handleOutput(output);
        }
        // the browser closed underneath the run: without the sentinel the
        // tests never finished - report failure, not success
        case TerminatedEvent ignored -> finish(null);
        default -> { /* thread starts/exits etc. - nothing to do without views */ }
      }
    }
  }

  private void handleOutput(OutputEvent output) {
    String text = output.getBody().getOutput();
    if (text == null) {
      return;
    }
    Integer exitCode = HostedTestRunSentinel.exitCode(text);
    String replayed = HostedTestRunSentinel.strip(text);
    if (!replayed.isEmpty()) {
      Key<?> type = "stderr".equals(output.getBody().getCategory())
                    ? ProcessOutputTypes.STDERR
                    : ProcessOutputTypes.STDOUT;
      notifyTextAvailable(replayed, type);
    }
    if (exitCode != null) {
      finish(exitCode);
    }
  }

  private void failRun(String message) {
    // Stop tears the backend down mid-poll and the resulting IOException
    // lands here; that is not a startup failure and must not be printed
    // after the run already ended
    if (finished) {
      return;
    }
    notifyTextAvailable(message + "\n", ProcessOutputTypes.STDERR);
    finish(null);
  }

  /** Ends the run: browser, adapter and server torn down; null exit = the run never completed (reported as 1). */
  private void finish(@Nullable Integer exitCode) {
    terminate(exitCode != null ? exitCode : 1);
  }

  private synchronized void terminate(int exitCode) {
    if (finished) {
      return;
    }
    finished = true;
    backend.close();
    notifyProcessTerminated(exitCode);
  }

  @Override
  protected void destroyProcessImpl() {
    // the user's Stop: no tests outcome to report - a neutral 0 keeps the
    // "Terminated" banner quiet (the SM tree state is the real verdict)
    terminate(0);
  }

  @Override
  protected void detachProcessImpl() {
    destroyProcessImpl();
  }

  @Override
  public boolean detachIsDefault() {
    return false;
  }

  @Override
  public @Nullable OutputStream getProcessInput() {
    return null;
  }
}
