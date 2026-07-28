package com.intellij.plugins.haxe.debugger.hxcppserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Step into a CLOSURE call (`var fn = grab; fn()`), where the callee is only
 * knowable at runtime. cpp.vm.Debugger's STEP_INTO is line-based and
 * depth-agnostic — the runtime instruments every Haxe line, so entering the
 * closure's target needs no server-side callee resolution; this pins that it
 * actually lands in the body (the HashLink adapter needed a real fix for the
 * same shape: its step-in plants entry breakpoints, and a closure call has
 * no static entry).
 */
@DisplayName("HXCPP debugger: closure step (integration)")
public class ClosureStepIT {

  @Test
  @DisplayName("step into enters a closure callee")
  public void stepIntoEntersAClosureCallee() throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("closurecall")) {
      session.initialize();
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.CLOSURE_CALL_LINE}, null);
      session.configurationDone();
      int threadId = session.stoppedThread(session.awaitStopped());

      session.stepIn(threadId);
      int steppedThread = session.stoppedThread(session.awaitStopped());
      StackFrame top = session.topFrame(steppedThread);

      assertEquals(FixtureSession.CLOSURE_BODY_LINE, top.getLine(), "stepped into the closure's target grab");

      // the session stays healthy: run to a clean exit
      session.resume(steppedThread);
      assertEquals(0, session.awaitExit(), "clean exit");
    }
  }

  @Test
  @DisplayName("step into enters a closure from array access")
  public void stepIntoEntersAClosureFromArrayAccess() throws Exception {
    // `functions[0]()`: the closure only exists mid-line (produced by the
    // array access) — cpp's line-based STEP_INTO enters it regardless
    try (FixtureSession session = FixtureSession.launchScenario("closurecall")) {
      session.initialize();
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{FixtureSession.CLOSURE_ARRAY_CALL_LINE}, null);
      session.configurationDone();
      int threadId = session.stoppedThread(session.awaitStopped());

      session.stepIn(threadId);
      int steppedThread = session.stoppedThread(session.awaitStopped());
      StackFrame top = session.topFrame(steppedThread);

      assertEquals(FixtureSession.CLOSURE_BODY_LINE, top.getLine(), "stepped into the array element's target grab");

      session.resume(steppedThread);
      assertEquals(0, session.awaitExit(), "clean exit");
    }
  }
}
