package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Re-sending setBreakpoints for a file while stopped on a breakpoint in it (what
 * run-to-cursor, or toggling a breakpoint while paused, does) must not make the
 * next continue immediately re-hit the breakpoint the session is parked on. setForSource
 * re-arms the INT3 at the current instruction pointer; the session must keep it
 * suspended so continue single-steps the real instruction and makes progress.
 */
@DisplayName("HashLink debugger: breakpoint reflush (integration)")
public class BreakpointReflushIntegrationTest extends DapIntegrationTestBase {

  @Test
  @DisplayName("reflush while stopped does not re hit the current breakpoint")
  public void reflushWhileStoppedDoesNotReHitTheCurrentBreakpoint() throws Exception {
    // stop at the loop line on the first iteration (i == 0)
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertEquals("0", localsInTopFrame(stopped.getBody().getThreadId()).get("i"), "first iteration");

    // re-send this file's breakpoints WHILE stopped on one — re-arms the INT3 at the
    // current instruction pointer, the exact situation that used to cause a re-hit
    assertTrue(setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE).isSuccess(), "reflush while stopped succeeds");

    // continue: the loop body must actually run (i advances to 1), proving the session
    // stepped over the current breakpoint instead of re-hitting it at i == 0
    StoppedEvent next = continueToNextStop();
    assertEquals("1", localsInTopFrame(next.getBody().getThreadId()).get("i"), "continue made progress past the current breakpoint");

    request(new DisconnectRequest());
  }

  /**
   * Removing a file's breakpoints WHILE stopped on one of them must fully
   * retire that breakpoint: the next continue used to re-arm the removed
   * breakpoint's INT3 from the stale stopped-on reference, and the orphan trap
   * (no table entry) was then "resumed past silently" with Eip beyond the
   * 0xCC — executing the original instruction minus its first byte and
   * faulting the VM at the removed breakpoint's line.
   */
  @Test
  @DisplayName("removing breakpoints while stopped on one runs cleanly after continue")
  public void removingBreakpointsWhileStoppedOnOneRunsCleanlyAfterContinue() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    int threadId = stopped.getBody().getThreadId();

    // remove every breakpoint in the file while standing on one of them
    assertTrue(setBreakpoints(fixtureSrcDir.resolve(FIXTURE_MAIN).toString()).isSuccess(), "breakpoint removal succeeds");

    // the loop line executes twice more; with the stale INT3 re-armed this
    // faulted ("Low-level runtime error") instead of running to a clean exit
    assertTrue(request(continueRequest(threadId)).isSuccess(), "continue succeeds");
    boolean exited = false;
    Integer exitCode = null;
    while (!exited) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected a debug event (run-to-exit after breakpoint removal)");
      if (event instanceof StoppedEvent unexpected) {
        fail("no further stop expected after removing all breakpoints, got: "
             + unexpected.getBody().getReason() + " / " + unexpected.getBody().getDescription());
      }
      else if (event instanceof ExitedEvent exit) {
        exited = true;
        exitCode = exit.getBody().getExitCode();
      }
    }
    assertEquals(Integer.valueOf(0), exitCode, "clean exit");
  }
}
