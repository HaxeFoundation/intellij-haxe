package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.OutputEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.StoppedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.TerminatedEvent;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.ConfigurationDoneRequest;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.PauseArguments;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.PauseRequest;
import java.util.Map;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Pause: interrupt a freely-running debuggee, report a stop with reason
 * "pause" IN Haxe code (real frames + locals), then resume it to completion.
 * Exercises the forceBreak + drain path and the resume of the held break-event
 * thread against real HashLink, using a busy-loop fixture so the interrupt lands
 * in Haxe rather than a native call.
 */
public class PauseIntegrationTest extends DapIntegrationTestBase {

  @Before
  public void requireSpinFixture() {
    Assume.assumeTrue("spin fixture not built - skipping", spinFixtureHl != null);
  }

  @Test
  public void pauseInterruptsRunningDebuggeeInHaxeCodeThenResumesToExit() throws Exception {
    initialize();
    assertTrue("launch succeeds", launch(spinFixtureHl.toString()).isSuccess());
    assertTrue("configurationDone succeeds", request(new ConfigurationDoneRequest()).isSuccess());

    // wait until the busy loop is actually running so the pause interrupts a
    // running process rather than racing the launch
    awaitOutputContaining("spin-start");

    // interrupt the running debuggee
    PauseRequest pause = new PauseRequest();
    PauseArguments args = new PauseArguments();
    args.setThreadId(1);
    pause.setArguments(args);
    assertTrue("pause is acknowledged", request(pause).isSuccess());

    // a stop with reason "pause" arrives on a real thread
    StoppedEvent stopped = awaitStopped();
    assertEquals("stopped for pause", "pause", stopped.getBody().getReason());
    int threadId = stopped.getBody().getThreadId();

    // it landed IN the Haxe busy loop: top frame is Spin.main and its locals are
    // inspectable — the whole point of being able to pause
    assertEquals("paused in Haxe code", "Spin.main", topFrameName(threadId));
    Map<String, String> locals = variablesByName(localsScopeReference(topFrameId(threadId)));
    assertFalse("loop locals are visible while paused (" + locals.keySet() + ")", locals.isEmpty());

    // continue resumes the held break-event thread; the debuggee runs to the end
    assertTrue("continue is acknowledged", request(continueRequest(threadId)).isSuccess());
    awaitTerminated();
  }

  private void awaitOutputContaining(String needle) throws Exception {
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected output containing '" + needle + "'", event);
      if (event instanceof OutputEvent out && out.getBody().getOutput() != null
          && out.getBody().getOutput().contains(needle)) {
        return;
      }
    }
  }

  private void awaitTerminated() throws Exception {
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull("expected termination after continue", event);
      if (event instanceof TerminatedEvent) {
        return;
      }
    }
  }
}
