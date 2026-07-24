package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
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

  /**
   * Removing a file's breakpoints WHILE stopped on one of them must fully
   * retire that breakpoint: the next continue used to re-arm the removed
   * breakpoint's INT3 from the stale stopped-on reference, and the orphan trap
   * (no table entry) was then "resumed past silently" with Eip beyond the
   * 0xCC — executing the original instruction minus its first byte and
   * faulting the VM at the removed breakpoint's line.
   */
  @Test
  public void removingBreakpointsWhileStoppedOnOneRunsCleanlyAfterContinue() throws Exception {
    StoppedEvent stopped = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    int threadId = stopped.getBody().getThreadId();

    // remove every breakpoint in the file while standing on one of them
    assertTrue("breakpoint removal succeeds",
               setBreakpoints(fixtureSrcDir.resolve(FIXTURE_MAIN).toString()).isSuccess());

    // the loop line executes twice more; with the stale INT3 re-armed this
    // faulted ("Low-level runtime error") instead of running to a clean exit
    assertTrue("continue succeeds", request(continueRequest(threadId)).isSuccess());
    boolean exited = false;
    Integer exitCode = null;
    while (!exited) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected a debug event (run-to-exit after breakpoint removal)", event);
      if (event instanceof StoppedEvent unexpected) {
        fail("no further stop expected after removing all breakpoints, got: "
             + unexpected.getBody().getReason() + " / " + unexpected.getBody().getDescription());
      }
      else if (event instanceof ExitedEvent exit) {
        exited = true;
        exitCode = exit.getBody().getExitCode();
      }
    }
    assertEquals("clean exit", Integer.valueOf(0), exitCode);
  }
}
