package com.intellij.plugins.haxe.debugger.hxcppserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

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

  /** (call line in the closurecall scenario, what the call goes through). */
  static final List<Arguments> CLOSURE_CALL_LINES = List.of(
    // step into enters a closure callee: `var fn = grab; fn()`
    arguments(FixtureSession.CLOSURE_CALL_LINE, "a closure local"),
    // step into enters a closure from array access: `functions[0]()`, where the
    // closure only exists mid-line (produced by the array access) - cpp's
    // line-based STEP_INTO enters it regardless
    arguments(FixtureSession.CLOSURE_ARRAY_CALL_LINE, "an array element"));

  @ParameterizedTest(name = "{1}")
  @FieldSource("CLOSURE_CALL_LINES")
  @DisplayName("step into enters the closure callee")
  public void stepIntoEntersTheClosureCallee(int callLine, String callee) throws Exception {
    try (FixtureSession session = FixtureSession.launchScenario("closurecall")) {
      session.initialize();
      session.setBreakpoints(FixtureSession.EX_SOURCE, new int[]{callLine}, null);
      session.configurationDone();
      int threadId = session.stoppedThread(session.awaitStopped());

      session.stepIn(threadId);
      int steppedThread = session.stoppedThread(session.awaitStopped());
      StackFrame top = session.topFrame(steppedThread);

      assertEquals(FixtureSession.CLOSURE_BODY_LINE, top.getLine(), "stepped into grab through " + callee);

      // the session stays healthy: run to a clean exit
      session.resume(steppedThread);
      assertEquals(0, session.awaitExit(), "clean exit");
    }
  }
}
