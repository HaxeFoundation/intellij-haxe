package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Drives step over / into / out against a real HashLink debug session, one step
 * kind per test. Every test starts stopped at the loop breakpoint in main, where
 * the next statement is the call to add().
 *
 * Closure step-in has two resolution paths, exercised as the two rows of the
 * parameterized closure tests. STATIC: `var fn = grab; fn()`
 * (FIXTURE_CLOSURE_CALL_LINE) — the callee exists only at runtime (OCallClosure
 * has no static findex), so step-into resolves the closure register's vclosure
 * `fun` pointer at the stop and plants the entry temp there; without that, the
 * step degraded to a step-over (user-reported). DEFERRED: `callbacks[idx]()`
 * (FIXTURE_CLOSURE_REAL_ARRAY_LINE) — a REAL array (runtime index, not
 * analyzer-folded): the closure register is loaded BY the array access on the
 * same line, so it is EMPTY at the stop and cannot be resolved up front; the
 * step traps the call op itself, resolves the operand there, and runs on into
 * the callee (user-reported after the plain closure fix).
 */
@DisplayName("HashLink debugger: stepping (integration)")
public class SteppingIntegrationTest extends DapIntegrationTestBase {
  @Test
  @DisplayName("step into enters callee")
  public void stepIntoEntersCallee() throws Exception {
    StoppedEvent atLoop = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertEquals("breakpoint", atLoop.getBody().getReason());
    assertTrue(topFrameName(atLoop.getBody().getThreadId()).endsWith("main"), "stopped in main");

    assertTrue(request(stepInRequest(atLoop.getBody().getThreadId())).isSuccess(), "stepIn accepted");
    StoppedEvent inAdd = awaitStopped();

    assertEquals("step", inAdd.getBody().getReason());
    String frame = topFrameName(inAdd.getBody().getThreadId());
    assertTrue(frame.endsWith("add"), "stepped into add (was " + frame + ")");

    request(new DisconnectRequest());
  }

  @ParameterizedTest(name = "call line {0}")
  @ValueSource(ints = {FIXTURE_CLOSURE_CALL_LINE, FIXTURE_CLOSURE_REAL_ARRAY_LINE})
  @DisplayName("step into enters the closure callee")
  public void stepIntoEntersTheClosureCallee(int callLine) throws Exception {
    StoppedEvent atCall = runToBreakpoint(FIXTURE_CLOSURE, callLine);
    int atCallLine = stackTrace(atCall.getBody().getThreadId()).getBody().getStackFrames().get(0).getLine();
    assertEquals(callLine, atCallLine, "stopped on the closure call line");

    assertTrue(request(stepInRequest(atCall.getBody().getThreadId())).isSuccess(), "stepIn accepted");
    StoppedEvent inGrab = awaitStopped();

    assertEquals("step", inGrab.getBody().getReason());
    String frame = topFrameName(inGrab.getBody().getThreadId());
    assertTrue(frame.endsWith("grab"), "stepped into the closure's target grab (was " + frame + ")");

    request(new DisconnectRequest());
  }

  @ParameterizedTest(name = "call line {0}")
  @ValueSource(ints = {FIXTURE_CLOSURE_CALL_LINE, FIXTURE_CLOSURE_REAL_ARRAY_LINE})
  @DisplayName("stepping inside the closure entered callee stays inside")
  public void steppingInsideTheClosureEnteredCalleeStaysInside(int callLine) throws Exception {
    // user-reported: after ENTERING a closure-called function, the very next
    // step jumped back out to the caller (and the caller position skipped a
    // line) instead of advancing to the callee's second line
    StoppedEvent atCall = runToBreakpoint(FIXTURE_CLOSURE, callLine);
    int threadId = atCall.getBody().getThreadId();
    assertTrue(request(stepInRequest(threadId)).isSuccess(), "stepIn accepted");
    StoppedEvent inGrab = awaitStopped();
    assertEquals(FIXTURE_CLOSURE_BODY_LINE, stackTrace(inGrab.getBody().getThreadId()).getBody().getStackFrames().get(0).getLine(), "landed on grab's first line");

    assertTrue(request(nextRequest(inGrab.getBody().getThreadId())).isSuccess(), "next accepted");
    StoppedEvent second = awaitStopped();
    var frame = stackTrace(second.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertTrue(frame.getName().endsWith("grab"), "still inside grab (was " + frame.getName() + " line " + frame.getLine() + ")");
    assertEquals(FIXTURE_CLOSURE_BODY_LINE2, frame.getLine(), "advanced to grab's second line");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("step out returns to caller")
  public void stepOutReturnsToCaller() throws Exception {
    StoppedEvent atLoop = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertTrue(request(stepInRequest(atLoop.getBody().getThreadId())).isSuccess(), "stepIn accepted");
    StoppedEvent inAdd = awaitStopped();

    assertTrue(request(stepOutRequest(inAdd.getBody().getThreadId())).isSuccess(), "stepOut accepted");
    StoppedEvent backInMain = awaitStopped();

    assertEquals("step", backInMain.getBody().getReason());
    String frame = topFrameName(backInMain.getBody().getThreadId());
    assertTrue(frame.endsWith("main"), "stepped out to main (was " + frame + ")");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("step over stays in caller")
  public void stepOverStaysInCaller() throws Exception {
    StoppedEvent atLoop = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    assertTrue(request(nextRequest(atLoop.getBody().getThreadId())).isSuccess(), "next accepted");
    StoppedEvent afterNext = awaitStopped();

    assertEquals("step", afterNext.getBody().getReason());
    String frame = topFrameName(afterNext.getBody().getThreadId());
    assertTrue(frame.endsWith("main"), "still in main after step over (was " + frame + ")");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("step over a long running call waits for the landing")
  public void stepOverALongRunningCallWaitsForTheLanding() throws Exception {
    // Regression: a step over a slow call (Sys.sleep(3)) produces no debug
    // events for seconds. The step must WAIT for its landing — an earlier
    // "step watchdog" wrongly downgraded such steps to a resume after 2s,
    // losing the stop entirely (user-reported design flaw, verified here).
    StoppedEvent atSleep = runToBreakpoint(FIXTURE_MAIN, FIXTURE_SLOW_LINE);
    long start = System.currentTimeMillis();
    assertTrue(request(nextRequest(atSleep.getBody().getThreadId())).isSuccess(), "next accepted");
    StoppedEvent landed = awaitStopped();
    long elapsed = System.currentTimeMillis() - start;

    assertEquals("step", landed.getBody().getReason());
    var frame = stackTrace(landed.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertEquals(FIXTURE_SLOW_AFTER_LINE, frame.getLine(), "landed on the line after the sleep");
    assertTrue(elapsed >= 2500, "the landing took the sleep's duration (" + elapsed + "ms), not a watchdog shortcut");

    request(new DisconnectRequest());
  }

  @Test
  @DisplayName("step over a caught throw lands in the catch block")
  public void stepOverACaughtThrowLandsInTheCatchBlock() throws Exception {
    // Regression (user-reported): stepping over `throw` inside a try left the
    // function entirely instead of landing in the catch. The CFG treated OThrow
    // as terminal; the VM actually longjmps to the enclosing OTrap's handler,
    // so the step planted no temp at the catch and ran through it to the caller.
    // Uncaught.hx: line 11 `throw "caught-one"` inside try, catch body prints
    // on line 13.
    // haxe 4.1 emits bogus "line 1" debug info for catch-handler ops, so the
    // landing cannot be identified — not supported by the current adapter
    assumeFixtureHaxe43Plus();
    Assumptions.assumeTrue(uncaughtFixtureHl != null, "uncaught fixture not built - skipping");

    initialize();
    assertTrue(launch(uncaughtFixtureHl.toString()).isSuccess(), "launch");
    assertTrue(setBreakpoints(fixtureSrcDir.resolve("Uncaught.hx").toString(), 11).isSuccess(), "breakpoint on the caught throw");
    assertTrue(request(new ConfigurationDoneRequest()).isSuccess(), "configurationDone");

    StoppedEvent atThrow = awaitStopped();
    int threadId = atThrow.getBody().getThreadId();

    assertTrue(request(nextRequest(threadId)).isSuccess(), "next accepted");
    StoppedEvent landed = awaitStopped();

    assertEquals("step", landed.getBody().getReason());
    var frame = stackTrace(landed.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertTrue(frame.getName().endsWith("main"), "still in Uncaught.main (was " + frame.getName() + ")");
    assertTrue(frame.getLine() >= 12 && frame.getLine() <= 14, "landed in the catch block, lines 12-14 (was line " + frame.getLine() + ")");

    request(new DisconnectRequest());
  }

  // stepOverALongRunningCallWaitsForTheLanding needs the fixture's slow call
  // at its full 3s (it asserts elapsed >= 2500ms - which also fails loudly if
  // this plumbing ever breaks). Everywhere else the sleep is near-instant.
  @Override
  protected Map<String, String> adapterEnv() {
    return Map.of("FIXTURE_SLOW", "1");
  }
}
