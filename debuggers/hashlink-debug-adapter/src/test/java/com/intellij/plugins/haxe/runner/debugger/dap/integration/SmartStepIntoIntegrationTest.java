package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StepInTarget;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import java.util.List;
import org.junit.Test;

/**
 * Smart step into: on a line with several calls, stepInTargets lists them (in
 * execution order) and stepIn with a targetId enters exactly the chosen one —
 * running through the calls before it without stopping.
 *
 * Uses Main.hx line 20, the demo line with eight calls:
 * throwDemo(); inspectDemo(); Rich.demo(); Shadowed.demo(); ...
 */
public class SmartStepIntoIntegrationTest extends DapIntegrationTestBase {

  private static final int FIXTURE_DEMO_LINE = 20;

  @Test
  public void stepInTargetsListsTheCallsOnTheLineInExecutionOrder() throws Exception {
    StoppedEvent atDemo = runToBreakpoint(FIXTURE_MAIN, FIXTURE_DEMO_LINE);
    int frameId = topFrameId(atDemo.getBody().getThreadId());

    List<StepInTarget> targets = requestStepInTargets(frameId);

    assertTrue("several call targets on the demo line (got " + targets + ")", targets.size() >= 4);
    assertTrue("first target is the first call on the line (was " + targets.get(0).getLabel() + ")",
               targets.get(0).getLabel().endsWith("Main.throwDemo"));
    assertTrue("second target follows source order (was " + targets.get(1).getLabel() + ")",
               targets.get(1).getLabel().endsWith("Main.inspectDemo"));
    assertTrue("Rich.demo is offered (targets: " + targets + ")",
               targets.stream().anyMatch(t -> t.getLabel().endsWith("Rich.demo")));

    request(new DisconnectRequest());
  }

  @Test
  public void aClosureCallIsOfferedWithItsRuntimeTarget() throws Exception {
    // `fn()` has no static callee (OCallClosure): the target list resolves
    // the closure's RUNTIME fun pointer and labels it with the actual function
    StoppedEvent atCall = runToBreakpoint(FIXTURE_CLOSURE, FIXTURE_CLOSURE_CALL_LINE);
    List<StepInTarget> targets = requestStepInTargets(topFrameId(atCall.getBody().getThreadId()));

    assertTrue("the closure's runtime target is offered (targets: " + targets + ")",
               targets.stream().anyMatch(t -> t.getLabel().endsWith("Holder.grab")));

    request(new DisconnectRequest());
  }

  @Test
  public void stepInWithATargetIdEntersTheChosenCallSkippingTheOnesBefore() throws Exception {
    StoppedEvent atDemo = runToBreakpoint(FIXTURE_MAIN, FIXTURE_DEMO_LINE);
    int threadId = atDemo.getBody().getThreadId();
    List<StepInTarget> targets = requestStepInTargets(topFrameId(threadId));

    StepInTarget richDemo = targets.stream()
      .filter(t -> t.getLabel().endsWith("Rich.demo"))
      .findFirst()
      .orElseThrow();

    StepInRequest stepIn = stepInRequest(threadId, richDemo.getId());
    assertTrue("targeted stepIn accepted", request(stepIn).isSuccess());
    StoppedEvent landed = awaitStopped();

    assertEquals("step", landed.getBody().getReason());
    var frame = stackTrace(landed.getBody().getThreadId()).getBody().getStackFrames().get(0);
    assertTrue("landed in Rich.demo (was " + frame.getName() + ")", frame.getName().endsWith("Rich.demo"));
    assertFalse("did not stop in the earlier calls", frame.getName().contains("throwDemo"));

    request(new DisconnectRequest());
  }

  /**
   * Stepping out of an entered call parks the debuggee at the return address,
   * which maps MID-op back onto the finished call's opcode — the finished call
   * must NOT be offered as a target again (it was, before startOpCallDone),
   * while the not-yet-executed calls later on the line still are.
   */
  @Test
  public void aFinishedCallIsNotOfferedAgainAfterSteppingOut() throws Exception {
    StoppedEvent atDemo = runToBreakpoint(FIXTURE_MAIN, FIXTURE_DEMO_LINE);
    int threadId = atDemo.getBody().getThreadId();
    List<StepInTarget> targets = requestStepInTargets(topFrameId(threadId));
    StepInTarget first = targets.get(0); // Main.throwDemo, per the ordering test

    StepInRequest stepIn = stepInRequest(threadId, first.getId());
    assertTrue("targeted stepIn accepted", request(stepIn).isSuccess());
    awaitStopped(); // inside the chosen callee

    StepOutRequest stepOut = stepOutRequest(threadId);
    assertTrue("stepOut accepted", request(stepOut).isSuccess());
    StoppedEvent back = awaitStopped(); // back on the demo line, at the finished call's return

    List<StepInTarget> after = requestStepInTargets(topFrameId(back.getBody().getThreadId()));
    assertFalse("the finished call is not offered again (targets: " + after + ")",
                after.stream().anyMatch(t -> t.getLabel().equals(first.getLabel())));
    assertTrue("the later calls on the line are still offered (targets: " + after + ")",
               after.stream().anyMatch(t -> t.getLabel().endsWith("Main.inspectDemo")));

    request(new DisconnectRequest());
  }

  private List<StepInTarget> requestStepInTargets(int frameId) throws Exception {
    var response = request(stepInTargetsRequest(frameId));
    assertTrue("stepInTargets succeeds", response.isSuccess());
    assertTrue("typed response", response instanceof StepInTargetsResponse);
    return ((StepInTargetsResponse)response).getBody().getTargets();
  }
}
