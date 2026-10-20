package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The IDE's per-breakpoint Condition reaches the eval VM: the adapter sends
 * it with the breakpoint and the VM evaluates it at each hit, so a breakpoint
 * in a loop stops only on the iterations its condition selects. A condition
 * that fails to evaluate counts as false. A condition the VM cannot PARSE is
 * not covered here: the VM answers such a registration by dying on its
 * socket thread (see EvalProtocol.setBreakpoints), so the client checks the
 * syntax before sending.
 */
@DisplayName("Eval debugger: conditional breakpoint (live)")
public class EvalConditionalBreakpointLiveTest extends EvalLiveTestBase {
  private static final String FIXTURE = "EvalCondition.hx";
  // EvalCondition.hx load-bearing lines
  private static final int LOOP_BODY_LINE = 13;
  private static final int AFTER_LOOP_LINE = 15;

  @Test
  @Timeout(60)
  @DisplayName("stops only on the iteration the condition selects")
  public void stopsOnlyOnTheIterationTheConditionSelects() throws Exception {
    startWithBreakpoints(conditionalBreakpoint(LOOP_BODY_LINE, "i == 3"), lineBreakpoint(AFTER_LOOP_LINE));

    StoppedEvent stopped = awaitStopped();
    StackFrame top = topFrame(stopped.getBody().getThreadId());
    assertEquals("breakpoint", stopped.getBody().getReason(), "stopped by the conditional breakpoint");
    assertEquals(LOOP_BODY_LINE, top.getLine(), "stopped on the loop body");
    assertEquals("3", findLocal(top.getId(), "i").variable().getValue(), "the first stop is the iteration the condition selects");

    assertTrue(request(continueRequest(stopped.getBody().getThreadId())).isSuccess(), "continue");
    StoppedEvent afterLoop = awaitStopped();
    assertEquals(AFTER_LOOP_LINE, topFrame(afterLoop.getBody().getThreadId()).getLine(),
                 "no further iteration stops; the next stop is the unconditional breakpoint after the loop");

    assertTrue(request(continueRequest(afterLoop.getBody().getThreadId())).isSuccess(), "continue to the end");
    awaitTerminated();
    assertTrue(haxe.waitFor(TIMEOUT, TimeUnit.MILLISECONDS), "haxe exited");
    assertEquals(0, haxe.exitValue(), "clean exit");
  }

  @Test
  @Timeout(60)
  @DisplayName("a trailing semicolon in the condition is tolerated")
  public void aTrailingSemicolonInTheConditionIsTolerated() throws Exception {
    startWithBreakpoints(conditionalBreakpoint(LOOP_BODY_LINE, "i == 4;"));

    StoppedEvent stopped = awaitStopped();
    StackFrame top = topFrame(stopped.getBody().getThreadId());
    assertEquals("4", findLocal(top.getId(), "i").variable().getValue(), "the condition minus its semicolon selected the stop");
  }

  @Test
  @Timeout(60)
  @DisplayName("a condition that cannot be evaluated never stops")
  public void aConditionThatCannotBeEvaluatedNeverStops() throws Exception {
    startWithBreakpoints(conditionalBreakpoint(LOOP_BODY_LINE, "noSuchName == 3"), lineBreakpoint(AFTER_LOOP_LINE));

    StoppedEvent stopped = awaitStopped();
    assertEquals(AFTER_LOOP_LINE, topFrame(stopped.getBody().getThreadId()).getLine(),
                 "the VM counts a failing condition as false: the first stop is the unconditional breakpoint after the loop");
  }

  @Override
  protected String fixtureMain() {
    return "EvalCondition";
  }

  /** The opening dance with the given breakpoints on the fixture, up to configurationDone. */
  private void startWithBreakpoints(SourceBreakpoint... breakpoints) throws Exception {
    initialize();
    launch();
    assertTrue(request(setBreakpointsRequest(FIXTURE, List.of(breakpoints))).isSuccess(), "setBreakpoints");
    configurationDone();
  }
}
