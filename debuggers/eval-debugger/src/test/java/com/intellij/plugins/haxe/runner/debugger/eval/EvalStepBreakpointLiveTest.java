package com.intellij.plugins.haxe.runner.debugger.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Breakpoints WIN over in-flight steps — the behaviour every sibling backend
 * has (hxcpp's HandleBreakpoints checks user breakpoints in every step mode;
 * the hashlink adapter resumes with user INT3s armed): a step-out must stop at
 * a breakpoint further down the function, and a step-over must stop at a
 * breakpoint inside the function being stepped over. The eval VM itself honours
 * breakpoints mid-verb (response + breakpointStop notification);
 * these tests pin the adapter's step-emulation loops to the same contract.
 */
@DisplayName("Eval debugger: step breakpoint (live)")
public class EvalStepBreakpointLiveTest extends EvalLiveTestBase {
  // EvalStepBp.hx load-bearing lines
  private static final int HELPER_BP_LINE = 11;
  private static final int WORK_START_LINE = 16;
  private static final int HELPER_CALL_LINE = 18;
  private static final int WORK_LATER_BP_LINE = 19;

  @Test
  @Timeout(60)
  @DisplayName("step out stops at a breakpoint further down the function")
  public void stepOutStopsAtABreakpointFurtherDownTheFunction() throws Exception {
    int threadId = runToFirstBreakpoint(WORK_START_LINE, WORK_LATER_BP_LINE);
    assertEquals(WORK_START_LINE, topFrame(threadId).getLine(), "parked at the top of work");

    StepOutRequest stepOut = stepOutRequest(threadId);
    assertTrue(request(stepOut).isSuccess(), "stepOut");

    StoppedEvent stopped = awaitStopped();
    assertEquals("breakpoint", stopped.getBody().getReason(), "step-out yields to the breakpoint below");
    assertEquals(WORK_LATER_BP_LINE, topFrame(stopped.getBody().getThreadId()).getLine(), "stopped ON that breakpoint's line");
  }

  @Test
  @Timeout(60)
  @DisplayName("step over stops at a breakpoint inside the stepped over call")
  public void stepOverStopsAtABreakpointInsideTheSteppedOverCall() throws Exception {
    int threadId = runToFirstBreakpoint(HELPER_CALL_LINE, HELPER_BP_LINE);
    assertEquals(HELPER_CALL_LINE, topFrame(threadId).getLine(), "parked on the helper() call");

    NextRequest next = nextRequest(threadId);
    assertTrue(request(next).isSuccess(), "next");

    StoppedEvent stopped = awaitStopped();
    assertEquals("breakpoint", stopped.getBody().getReason(), "step-over yields to the breakpoint inside the callee");
    assertEquals(HELPER_BP_LINE, topFrame(stopped.getBody().getThreadId()).getLine(), "stopped ON the callee's breakpoint line");
  }

  @Test
  @Timeout(60)
  @DisplayName("stepping off a breakpoint line does not insta stop on its own breakpoint")
  public void steppingOffABreakpointLineDoesNotInstaStopOnItsOwnBreakpoint() throws Exception {
    int threadId = runToFirstBreakpoint(WORK_START_LINE);
    assertEquals(WORK_START_LINE, topFrame(threadId).getLine(), "parked at the top of work");

    NextRequest next = nextRequest(threadId);
    assertTrue(request(next).isSuccess(), "next");

    StoppedEvent stopped = awaitStopped();
    assertEquals("step", stopped.getBody().getReason(), "a normal step off the parked breakpoint line");
    // the exact landing line wobbles by one (the VM sometimes reports a
    // trailing same-line sub-position first); the contract under test is only
    // that the step LEFT the line instead of insta-stopping on its own bp
    int landedLine = topFrame(stopped.getBody().getThreadId()).getLine();
    assertTrue(landedLine > WORK_START_LINE, "landed past the breakpoint line (was " + landedLine + ")");
  }

  @Override
  protected String fixtureMain() {
    return "EvalStepBp";
  }

  /** Sets the given breakpoints, runs to the first stop, returns the thread id. */
  private int runToFirstBreakpoint(int... lines) throws Exception {
    return runToBreakpoint("EvalStepBp.hx", lines).getBody().getThreadId();
  }
}
