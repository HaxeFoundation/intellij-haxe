package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import org.junit.Test;

/**
 * Re-sending setBreakpoints for a file while stopped on a breakpoint in it (what
 * run-to-cursor, or toggling a breakpoint while paused, does) must not make the
 * next continue immediately re-hit the breakpoint we are standing on. setForSource
 * re-arms the INT3 at the current instruction pointer; the session must keep it
 * suspended so continue single-steps the real instruction and makes progress.
 */
public class BreakpointReflushIntegrationTest extends DapIntegrationTestBase {

  @Test
  public void reflushWhileStoppedDoesNotReHitTheCurrentBreakpoint() throws Exception {
    // stop at the loop line on the first iteration (i == 0)
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertEquals("first iteration", "0", localsInTopFrame(stopped.getBody().getThreadId()).get("i"));

    // re-send this file's breakpoints WHILE stopped on one — re-arms the INT3 at the
    // current instruction pointer, the exact situation that used to cause a re-hit
    assertTrue("reflush while stopped succeeds", setBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE).isSuccess());

    // continue: the loop body must actually run (i advances to 1), proving we stepped
    // over the current breakpoint instead of re-hitting it at i == 0
    StoppedEvent next = continueToNextStop();
    assertEquals("continue made progress past the current breakpoint",
                 "1", localsInTopFrame(next.getBody().getThreadId()).get("i"));

    request(new DisconnectRequest());
  }
}
