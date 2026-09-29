package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Uncaught haxe.Exception INSTANCES (as opposed to bare string throws) put
 * the eval VM in a state it can never leave forward: at the exceptionStop,
 * every continue/step RE-EXECUTES the whole throw expression — constructor,
 * stack collection, toString, then the same exceptionStop again, forever
 * (the debugger stepped in circles through the constructor and
 * could not let the program die). The adapter's way out, pinned here: clear
 * the VM's exception options, then resume — the program dies NATURALLY with
 * its own uncaught-exception stderr and exit code 1. Also pinned: a session
 * whose exception breakpoints are disabled (empty filters) never stops on
 * the uncaught throw at all — the VM's DEFAULT would.
 */
@DisplayName("Eval debugger: object throw (live)")
public class EvalObjectThrowLiveTest extends EvalLiveTestBase {

  @Test
  @Timeout(60)
  @DisplayName("resume at the uncaught object stop lets the program die naturally")
  public void resumeAtTheUncaughtObjectStopLetsTheProgramDieNaturally() throws Exception {
    startSession(List.of("uncaught"));
    StoppedEvent stopped = awaitExceptionStop();
    String description = stopped.getBody().getDescription();
    assertTrue(description != null && description.contains("uncaught-object"), "stop carries the thrown text");

    ContinueRequest resume = continueRequest(stopped.getBody().getThreadId());
    assertTrue(request(resume).isSuccess(), "resume at the uncaught stop");
    awaitNaturalDeath();
  }

  @Test
  @Timeout(60)
  @DisplayName("stepping at the uncaught object stop lets the program die naturally")
  public void steppingAtTheUncaughtObjectStopLetsTheProgramDieNaturally() throws Exception {
    startSession(List.of("uncaught"));
    StoppedEvent stopped = awaitExceptionStop();
    StepInRequest stepIn = stepInRequest(stopped.getBody().getThreadId());
    assertTrue(request(stepIn).isSuccess(), "stepIn at the uncaught stop");
    awaitNaturalDeath();
  }

  @Test
  @Timeout(60)
  @DisplayName("with exception breakpoints disabled an uncaught throw never stops")
  public void withExceptionBreakpointsDisabledAnUncaughtThrowNeverStops() throws Exception {
    // the IDE sends EMPTY filters when every exception breakpoint is disabled;
    // that must override the VM's stop-on-uncaught DEFAULT: no stop, just the
    // program's natural death
    startSession(List.of());
    awaitNaturalDeath();
  }

  @Override
  protected String fixtureMain() {
    return "EvalThrowObj";
  }

  private StoppedEvent awaitExceptionStop() throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event instanceof StoppedEvent stopped) {
        assertEquals("exception", stopped.getBody().getReason(), "the stop is the exception");
        return stopped;
      }
      if (event instanceof TerminatedEvent) {
        throw new AssertionError("terminated before the exception stop");
      }
    }
    throw new AssertionError("no exception stop within " + TIMEOUT + "ms");
  }

  private void awaitNaturalDeath() throws Exception {
    long deadline = System.currentTimeMillis() + TIMEOUT;
    while (System.currentTimeMillis() < deadline) {
      Event event = dapClient.pollEvent(250);
      if (event instanceof TerminatedEvent) {
        assertTrue(haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "haxe exited");
        assertNotEquals(0, haxe.exitValue(), "died from the uncaught exception (non-zero exit)");
        return;
      }
      if (event instanceof StoppedEvent stopped) {
        throw new AssertionError("stopped again (" + stopped.getBody().getReason()
                                 + ") instead of dying — the uncaught re-execution loop is back");
      }
    }
    throw new AssertionError("program never died within " + TIMEOUT + "ms");
  }
}
