package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import java.util.Map;
import org.junit.Test;

/**
 * Drives step over / into / out against a real HashLink debug session, one step
 * kind per test. Every test starts stopped at the loop breakpoint in main, where
 * the next statement is the call to add().
 */
public class SteppingIntegrationTest extends DapIntegrationTestBase {

  // stepOverALongRunningCallWaitsForTheLanding needs the fixture's slow call
  // at its full 3s (it asserts elapsed >= 2500ms - which also fails loudly if
  // this plumbing ever breaks). Everywhere else the sleep is near-instant.
  @Override
  protected Map<String, String> adapterEnv() {
    return Map.of("FIXTURE_SLOW", "1");
  }

  @Test
  public void stepIntoEntersCallee() throws Exception {
    StoppedEvent atLoop = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertEquals("breakpoint", atLoop.getBody().getReason());
    assertTrue("stopped in main", topFrameName(atLoop.getBody().getThreadId()).endsWith("main"));

    assertTrue("stepIn accepted", request(stepInRequest(atLoop.getBody().getThreadId())).isSuccess());
    StoppedEvent inAdd = awaitStopped();

    assertEquals("step", inAdd.getBody().getReason());
    String frame = topFrameName(inAdd.getBody().getThreadId());
    assertTrue("stepped into add (was " + frame + ")", frame.endsWith("add"));

    request(new DisconnectRequest());
  }

  @Test
  public void stepIntoEntersAClosureCallee() throws Exception {
    // `var fn = grab; fn()` — the callee exists only at RUNTIME (OCallClosure
    // has no static findex). Step-into resolves the closure register's
    // vclosure `fun` pointer at the stop and plants the entry temp there;
    // without that, the step degraded to a step-over (user-reported).
    StoppedEvent atCall = runToBreakpoint(FIXTURE_CLOSURE, FIXTURE_CLOSURE_CALL_LINE);
    assertTrue("stopped in the constructor", topFrameName(atCall.getBody().getThreadId()).endsWith("Holder.new"));

    assertTrue("stepIn accepted", request(stepInRequest(atCall.getBody().getThreadId())).isSuccess());
    StoppedEvent inGrab = awaitStopped();

    assertEquals("step", inGrab.getBody().getReason());
    String frame = topFrameName(inGrab.getBody().getThreadId());
    assertTrue("stepped into the closure's target grab (was " + frame + ")", frame.endsWith("grab"));

    request(new DisconnectRequest());
  }

  @Test
  public void stepIntoEntersAClosureFromArrayAccess() throws Exception {
    // `callbacks[idx]()` — a REAL array (runtime index, not analyzer-folded):
    // the closure register is loaded BY the array access on the same line, so
    // it is EMPTY at the stop and cannot be resolved up front. The step traps
    // the call op itself, resolves the operand there, and runs on into the
    // callee (user-reported after the plain closure fix).
    StoppedEvent atCall = runToBreakpoint(FIXTURE_CLOSURE, FIXTURE_CLOSURE_REAL_ARRAY_LINE);
    int atCallLine = stackTrace(atCall.getBody().getThreadId()).getBody().getStackFrames().get(0).getLine();
    assertEquals("stopped on the array-access call line", FIXTURE_CLOSURE_REAL_ARRAY_LINE, atCallLine);

    assertTrue("stepIn accepted", request(stepInRequest(atCall.getBody().getThreadId())).isSuccess());
    StoppedEvent inGrab = awaitStopped();

    assertEquals("step", inGrab.getBody().getReason());
    String frame = topFrameName(inGrab.getBody().getThreadId());
    assertTrue("stepped into the array element's target grab (was " + frame + ")", frame.endsWith("grab"));

    request(new DisconnectRequest());
  }

  @Test
  public void steppingInsideAClosureEnteredCalleeStaysInside() throws Exception {
    // user-reported: after ENTERING a closure-called function, the very next
    // step jumped back out to the caller (and the caller position skipped a
    // line) instead of advancing to the callee's second line
    StoppedEvent atCall = runToBreakpoint(FIXTURE_CLOSURE, FIXTURE_CLOSURE_CALL_LINE);
    int threadId = atCall.getBody().getThreadId();
    assertTrue("stepIn accepted", request(stepInRequest(threadId)).isSuccess());
    StoppedEvent inGrab = awaitStopped();
    assertEquals("landed on grab's first line", FIXTURE_CLOSURE_BODY_LINE,
                 stackTrace(inGrab.getBody().getThreadId()).getBody().getStackFrames().get(0).getLine());

    assertTrue("next accepted", request(nextRequest(inGrab.getBody().getThreadId())).isSuccess());
    StoppedEvent second = awaitStopped();
    var frame = stackTrace(second.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertTrue("still inside grab (was " + frame.getName() + " line " + frame.getLine() + ")",
               frame.getName().endsWith("grab"));
    assertEquals("advanced to grab's second line", FIXTURE_CLOSURE_BODY_LINE2, frame.getLine());

    request(new DisconnectRequest());
  }

  @Test
  public void steppingInsideAnArrayClosureEnteredCalleeStaysInside() throws Exception {
    // same, entered through the DEFERRED path (callbacks[idx]())
    StoppedEvent atCall = runToBreakpoint(FIXTURE_CLOSURE, FIXTURE_CLOSURE_REAL_ARRAY_LINE);
    int threadId = atCall.getBody().getThreadId();
    assertTrue("stepIn accepted", request(stepInRequest(threadId)).isSuccess());
    StoppedEvent inGrab = awaitStopped();
    assertEquals("landed on grab's first line", FIXTURE_CLOSURE_BODY_LINE,
                 stackTrace(inGrab.getBody().getThreadId()).getBody().getStackFrames().get(0).getLine());

    assertTrue("next accepted", request(nextRequest(inGrab.getBody().getThreadId())).isSuccess());
    StoppedEvent second = awaitStopped();
    var frame = stackTrace(second.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertTrue("still inside grab (was " + frame.getName() + " line " + frame.getLine() + ")",
               frame.getName().endsWith("grab"));
    assertEquals("advanced to grab's second line", FIXTURE_CLOSURE_BODY_LINE2, frame.getLine());

    request(new DisconnectRequest());
  }

  @Test
  public void stepOutReturnsToCaller() throws Exception {
    StoppedEvent atLoop = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);
    assertTrue("stepIn accepted", request(stepInRequest(atLoop.getBody().getThreadId())).isSuccess());
    StoppedEvent inAdd = awaitStopped();

    assertTrue("stepOut accepted", request(stepOutRequest(inAdd.getBody().getThreadId())).isSuccess());
    StoppedEvent backInMain = awaitStopped();

    assertEquals("step", backInMain.getBody().getReason());
    String frame = topFrameName(backInMain.getBody().getThreadId());
    assertTrue("stepped out to main (was " + frame + ")", frame.endsWith("main"));

    request(new DisconnectRequest());
  }

  @Test
  public void stepOverStaysInCaller() throws Exception {
    StoppedEvent atLoop = runToBreakpoint(FIXTURE_MAIN, FIXTURE_LOOP_LINE);

    assertTrue("next accepted", request(nextRequest(atLoop.getBody().getThreadId())).isSuccess());
    StoppedEvent afterNext = awaitStopped();

    assertEquals("step", afterNext.getBody().getReason());
    String frame = topFrameName(afterNext.getBody().getThreadId());
    assertTrue("still in main after step over (was " + frame + ")", frame.endsWith("main"));

    request(new DisconnectRequest());
  }

  @Test
  public void stepOverALongRunningCallWaitsForTheLanding() throws Exception {
    // Regression: a step over a slow call (Sys.sleep(3)) produces no debug
    // events for seconds. The step must WAIT for its landing — an earlier
    // "step watchdog" wrongly downgraded such steps to a resume after 2s,
    // losing the stop entirely (user-reported design flaw, verified here).
    StoppedEvent atSleep = runToBreakpoint(FIXTURE_MAIN, FIXTURE_SLOW_LINE);

    long start = System.currentTimeMillis();
    assertTrue("next accepted", request(nextRequest(atSleep.getBody().getThreadId())).isSuccess());
    StoppedEvent landed = awaitStopped();
    long elapsed = System.currentTimeMillis() - start;

    assertEquals("step", landed.getBody().getReason());
    var frame = stackTrace(landed.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertEquals("landed on the line after the sleep", FIXTURE_SLOW_AFTER_LINE, frame.getLine());
    assertTrue("the landing took the sleep's duration (" + elapsed + "ms), not a watchdog shortcut",
               elapsed >= 2500);

    request(new DisconnectRequest());
  }

  @Test
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
    org.junit.Assume.assumeTrue("uncaught fixture not built - skipping", uncaughtFixtureHl != null);
    initialize();
    assertTrue("launch", launch(uncaughtFixtureHl.toString()).isSuccess());
    assertTrue("breakpoint on the caught throw",
               setBreakpoints(fixtureSrcDir.resolve("Uncaught.hx").toString(), 11).isSuccess());
    assertTrue("configurationDone",
               request(new com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest()).isSuccess());
    StoppedEvent atThrow = awaitStopped();
    int threadId = atThrow.getBody().getThreadId();

    assertTrue("next accepted", request(nextRequest(threadId)).isSuccess());
    StoppedEvent landed = awaitStopped();

    assertEquals("step", landed.getBody().getReason());
    var frame = stackTrace(landed.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertTrue("still in Uncaught.main (was " + frame.getName() + ")", frame.getName().endsWith("main"));
    assertTrue("landed in the catch block, lines 12-14 (was line " + frame.getLine() + ")",
               frame.getLine() >= 12 && frame.getLine() <= 14);

    request(new DisconnectRequest());
  }
}
