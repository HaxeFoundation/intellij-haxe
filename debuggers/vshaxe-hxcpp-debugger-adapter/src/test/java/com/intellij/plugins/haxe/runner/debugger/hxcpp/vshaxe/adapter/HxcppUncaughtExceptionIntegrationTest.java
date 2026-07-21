package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

/**
 * Uncaught-exception behaviour of the real server, pinned empirically: the
 * debuggee STOPS AT THE THROW LINE (observed with reason "pause" — the
 * server classifies the critical error only later in the unwind), and after
 * continuing, the thrown text surfaces (as an exception-reason stop and/or
 * the process's Critical Error output) before termination. There is nothing
 * to configure: setExceptionBreakpoints is an honest no-op.
 */
public class HxcppUncaughtExceptionIntegrationTest extends HxcppIntegrationTestBase {

  @Before
  public void setUp() throws Exception {
    launchFixture("hxcpp.fixture.uncaught.exe", "Uncaught.hx");
  }

  @Test
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
        assertTrue("exception stop should carry the thrown text, got: " + description,
                   description != null && description.contains("kaboom"));
        // releasing the final stop races the process's death — a failed
        // continue IS the expected outcome here
        continueQuietly(following.getBody().getThreadId());
      }
    }

    debuggee.waitFor(TIMEOUT, TimeUnit.MILLISECONDS);
    assertTrue("the thrown text should surface somewhere (exception stop or Critical Error output); "
               + "sawExceptionStop=" + sawExceptionStop + ", output:\n" + output(),
               sawExceptionStop || output().contains("kaboom"));
  }

  private void continueQuietly(int threadId) {
    ContinueArguments arguments = new ContinueArguments();
    arguments.setThreadId(threadId);
    ContinueRequest request = new ContinueRequest();
    request.setArguments(arguments);
    try {
      dapClient.sendRequest(request, TIMEOUT);
    } catch (Exception ignored) {
      // the dying debuggee may close the connection first
    }
  }
}
