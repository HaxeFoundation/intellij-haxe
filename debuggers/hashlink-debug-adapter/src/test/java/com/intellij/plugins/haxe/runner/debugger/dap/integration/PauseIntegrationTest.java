package com.intellij.plugins.haxe.runner.debugger.dap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pause: interrupt a freely-running debuggee, report a stop with reason
 * "pause" IN Haxe code (real frames + locals), then resume it to completion.
 * Exercises the forceBreak + drain path and the resume of the held break-event
 * thread against real HashLink, using a busy-loop fixture so the interrupt lands
 * in Haxe rather than a native call.
 */
public class PauseIntegrationTest extends DapIntegrationTestBase {

  @BeforeEach
  public void requireSpinFixture() {
    Assumptions.assumeTrue(spinFixtureHl != null, "spin fixture not built - skipping");
  }

  @Test
  public void pauseInterruptsRunningDebuggeeInHaxeCodeThenResumesToExit() throws Exception {
    initialize();
    assertTrue(launch(spinFixtureHl.toString()).isSuccess(), "launch succeeds");
    configurationDone();

    // wait until the busy loop is actually running so the pause interrupts a
    // running process rather than racing the launch
    awaitOutputContaining("spin-start");

    // interrupt the running debuggee
    PauseRequest pause = pauseRequest(1);
    assertTrue(request(pause).isSuccess(), "pause is acknowledged");

    // a stop with reason "pause" arrives on a real thread
    StoppedEvent stopped = awaitStopped();
    assertEquals("pause", stopped.getBody().getReason(), "stopped for pause");
    int threadId = stopped.getBody().getThreadId();

    // it landed IN the Haxe busy loop: top frame is Spin.main and its locals are
    // inspectable — the whole point of being able to pause
    assertEquals("Spin.main", topFrameName(threadId), "paused in Haxe code");
    Map<String, String> locals = variablesByName(localsScopeReference(topFrameId(threadId)));
    assertFalse(locals.isEmpty(), "loop locals are visible while paused (" + locals.keySet() + ")");

    // continue resumes the held break-event thread; the debuggee runs to the end
    assertTrue(request(continueRequest(threadId)).isSuccess(), "continue is acknowledged");
    awaitTerminated();
  }

  private void awaitOutputContaining(String needle) throws Exception {
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected output containing '" + needle + "'");
      if (event instanceof OutputEvent out && out.getBody().getOutput() != null
          && out.getBody().getOutput().contains(needle)) {
        return;
      }
    }
  }

  private void awaitTerminated() throws Exception {
    while (true) {
      Event event = client.pollEvent(TIMEOUT);
      assertNotNull(event, "expected termination after continue");
      if (event instanceof TerminatedEvent) {
        return;
      }
    }
  }
}
