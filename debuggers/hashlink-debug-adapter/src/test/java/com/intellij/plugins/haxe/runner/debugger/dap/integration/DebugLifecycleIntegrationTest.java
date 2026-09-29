package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import com.intellij.plugins.haxe.runner.debugger.dap.DapPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Drives the adapter through whole debug sessions and asserts the EVENT SEQUENCE:
 * launch, breakpoint stops, stack trace, continue, output, exit, disconnect.
 * These are deliberately flow tests — the subject under test is the ordering and
 * completeness of the lifecycle, not a single feature.
 */
@DisplayName("HashLink debugger: debug lifecycle (integration)")
public class DebugLifecycleIntegrationTest extends DapIntegrationTestBase {

  @Test
  @DisplayName("full breakpoint lifecycle")
  public void fullBreakpointLifecycle() throws Exception {
    initialize();
    Response launch = launch();
    assertTrue(launch.isSuccess(), "launch succeeds: " + launch.getMessage());

    Response setBreakpoints = setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertTrue(setBreakpoints.isSuccess(), "setBreakpoints succeeds");
    assertAllVerified(setBreakpoints);

    configurationDone();

    // The loop body runs three times, so three breakpoint stops are expected.
    List<String> output = new ArrayList<>();
    int stops = 0;
    boolean exited = false;
    boolean terminated = false;
    Integer exitCode = null;

    while (!terminated) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected a debug event");
      if (event instanceof StoppedEvent stopped) {
        stops++;
        assertEquals("breakpoint", stopped.getBody().getReason());
        int threadId = stopped.getBody().getThreadId();

        if (stops == 1) {
          // verify threads + stack trace on the first stop
          assertTrue(request(new ThreadsRequest()).isSuccess());
          StackTraceResponse frames = stackTrace(threadId);
          assertEquals(FIXTURE_LOOP_LINE, frames.getBody().getStackFrames().get(0).getLine(), "top frame at breakpoint line");
          String topPath = DapPaths.toForwardSlashes(frames.getBody().getStackFrames().get(0).getSource().getPath());
          assertTrue(topPath.endsWith("Main.hx"), "top frame in Main.hx (" + topPath + ")");
        }

        assertTrue(request(continueRequest(threadId)).isSuccess(), "continue succeeds");
      }
      else if (event instanceof OutputEvent out) {
        output.add(out.getBody().getOutput());
      }
      else if (event instanceof ExitedEvent exit) {
        exited = true;
        exitCode = exit.getBody().getExitCode();
      }
      else if (event instanceof TerminatedEvent) {
        terminated = true;
      }
    }

    assertEquals(3, stops, "breakpoint hit once per loop iteration");
    assertTrue(exited, "exited event received");
    assertEquals(Integer.valueOf(0), exitCode, "clean exit");
    String allOutput = String.join("", output);
    assertTrue(allOutput.contains("fixture-start"), "fixture start printed (" + allOutput + ")");
    assertTrue(allOutput.contains("fixture-total:3"), "fixture total printed");

    assertTrue(request(new DisconnectRequest()).isSuccess(), "disconnect succeeds");
  }

  @Test
  @DisplayName("runs to completion without breakpoints")
  public void runsToCompletionWithoutBreakpoints() throws Exception {
    initialize();
    assertTrue(launch().isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    awaitOutputContaining("fixture-start");
    awaitEvent(TerminatedEvent.class);
  }

  @Test
  @DisplayName("nonexistent program fails but adapter still serves disconnect")
  public void nonexistentProgramFailsButAdapterStillServesDisconnect() throws Exception {
    initialize();
    Response launch = launch("does-not-exist.hl");
    assertTrue(!launch.isSuccess(), "launch of a missing program fails");

    Response disconnect = request(new DisconnectRequest());
    assertTrue(disconnect.isSuccess(), "adapter still answers disconnect after a failed launch");
  }

  private static void assertAllVerified(Response response) {
    var body = ((SetBreakpointsResponse)response).getBody();
    assertTrue(body.getBreakpoints().size() >= 1, "at least one breakpoint returned");
    for (Breakpoint breakpoint : body.getBreakpoints()) {
      assertTrue(breakpoint.isVerified(), "breakpoint verified");
    }
  }
}
