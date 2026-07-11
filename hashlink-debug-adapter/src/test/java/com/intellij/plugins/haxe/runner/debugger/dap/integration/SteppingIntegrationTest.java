package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.DisconnectRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import org.junit.Test;

/**
 * Drives step over / into / out against a real HashLink debug session, one step
 * kind per test. Every test starts stopped at the loop breakpoint in main, where
 * the next statement is the call to add().
 */
public class SteppingIntegrationTest extends DapIntegrationTestBase {

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
}
