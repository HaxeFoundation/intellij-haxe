package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ExitedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SetBreakpointsResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackTraceResponse;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ThreadsRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Drives the adapter through whole debug sessions and asserts the EVENT SEQUENCE:
 * launch, breakpoint stops, stack trace, continue, output, exit, disconnect.
 * These are deliberately flow tests — the subject under test is the ordering and
 * completeness of the lifecycle, not a single feature.
 */
public class DebugLifecycleIntegrationTest extends DapIntegrationTestBase {

  @Test
  public void fullBreakpointLifecycle() throws Exception {
    initialize();
    Response launch = launch();
    assertTrue("launch succeeds: " + launch.getMessage(), launch.isSuccess());

    Response setBreakpoints = setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertTrue("setBreakpoints succeeds", setBreakpoints.isSuccess());
    assertAllVerified(setBreakpoints);

    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    // The loop body runs three times, so we expect three breakpoint stops.
    List<String> output = new ArrayList<>();
    int stops = 0;
    boolean exited = false;
    boolean terminated = false;
    Integer exitCode = null;

    while (!terminated) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a debug event", event);
      if (event instanceof StoppedEvent stopped) {
        stops++;
        assertEquals("breakpoint", stopped.getBody().getReason());
        int threadId = stopped.getBody().getThreadId();

        if (stops == 1) {
          // verify threads + stack trace on the first stop
          assertTrue(request(new ThreadsRequest()).isSuccess());
          StackTraceResponse frames = stackTrace(threadId);
          assertEquals("top frame at breakpoint line", FIXTURE_LOOP_LINE,
                       frames.getBody().getStackFrames().get(0).getLine());
          String topPath = frames.getBody().getStackFrames().get(0).getSource().getPath().replace('\\', '/');
          assertTrue("top frame in Main.hx (" + topPath + ")", topPath.endsWith("Main.hx"));
        }

        assertTrue("continue succeeds", request(continueRequest(threadId)).isSuccess());
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

    assertEquals("breakpoint hit once per loop iteration", 3, stops);
    assertTrue("exited event received", exited);
    assertEquals("clean exit", Integer.valueOf(0), exitCode);
    String allOutput = String.join("", output);
    assertTrue("fixture start printed (" + allOutput + ")", allOutput.contains("fixture-start"));
    assertTrue("fixture total printed", allOutput.contains("fixture-total:3"));

    assertTrue("disconnect succeeds", request(new DisconnectRequest()).isSuccess());
  }

  @Test
  public void runsToCompletionWithoutBreakpoints() throws Exception {
    initialize();
    assertTrue(launch().isSuccess());
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess());

    boolean terminated = false;
    boolean sawOutput = false;
    while (!terminated) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected an event before termination", event);
      if (event instanceof OutputEvent out && out.getBody().getOutput().contains("fixture-start")) {
        sawOutput = true;
      }
      else if (event instanceof TerminatedEvent) {
        terminated = true;
      }
    }
    assertTrue("debuggee produced output", sawOutput);
  }

  @Test
  public void nonexistentProgramFailsButAdapterStillServesDisconnect() throws Exception {
    initialize();
    Response launch = launch("does-not-exist.hl");
    assertTrue("launch of a missing program fails", !launch.isSuccess());

    Response disconnect = request(new DisconnectRequest());
    assertTrue("adapter still answers disconnect after a failed launch", disconnect.isSuccess());
  }

  private static void assertAllVerified(Response response) {
    var body = ((SetBreakpointsResponse)response).getBody();
    assertTrue("at least one breakpoint returned", body.getBreakpoints().size() >= 1);
    for (Breakpoint breakpoint : body.getBreakpoints()) {
      assertTrue("breakpoint verified", breakpoint.isVerified());
    }
  }
}
