package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Uncaught-exception behaviour of the real server, pinned empirically: the
 * debuggee STOPS AT THE THROW LINE (observed with reason "pause" — the
 * server classifies the critical error only later in the unwind), and after
 * continuing, the thrown text surfaces (as an exception-reason stop and/or
 * the process's Critical Error output) before termination. There is nothing
 * to configure: setExceptionBreakpoints is an honest no-op.
 */
@DisplayName("HXCPP debugger (vshaxe): uncaught exception (integration)")
public class HxcppUncaughtExceptionIntegrationTest extends HxcppIntegrationTestBase {

  @BeforeEach
  public void setUp() throws Exception {
    launchFixture("hxcpp.fixture.uncaught.exe", "Uncaught.hx");
  }

  @Test
  @DisplayName("uncaught throw stops at the throw line and surfaces the text")
  public void uncaughtThrowStopsAtTheThrowLineAndSurfacesTheText() throws Exception {
    initializeAndLaunch();
    // must not fail even though the server has no handler for it
    require(new SetExceptionBreakpointsRequest());
    require(new ConfigurationDoneRequest());

    // the debugger stops AT the uncaught throw, whatever the reason label
    Stop stop = awaitStopAtLine(lineOfMarker("throw"));

    // releasing it lets the throw unwind: an exception-reason stop may follow
    // (continue past it), then the process dies printing the Critical Error
    sendContinue(stop.threadId());
    boolean sawExceptionStop = false;
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event instanceof TerminatedEvent) {
        break;
      }
      if (event instanceof StoppedEvent following && "exception".equals(following.getBody().getReason())) {
        sawExceptionStop = true;
        String description = following.getBody().getDescription();
        assertTrue(description != null && description.contains("kaboom"), "exception stop should carry the thrown text, got: " + description);
        // releasing the final stop races the process's death — a failed
        // continue IS the expected outcome here
        continueQuietly(following.getBody().getThreadId());
      }
    }

    debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS);

    boolean thrownTextSurfaced = sawExceptionStop || output().contains("kaboom");
    assertTrue(thrownTextSurfaced, "the thrown text should surface somewhere (exception stop or Critical Error output); "
               + "sawExceptionStop=" + sawExceptionStop + ", output:\n" + output());
  }

  private void continueQuietly(int threadId) {
    try {
      dapClient.sendRequest(continueRequest(threadId), TIMEOUT);
    } catch (Exception ignored) {
      // the dying debuggee may close the connection first
    }
  }
}
