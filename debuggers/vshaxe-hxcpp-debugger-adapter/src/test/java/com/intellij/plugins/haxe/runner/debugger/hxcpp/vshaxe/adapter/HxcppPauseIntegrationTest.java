package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.events.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests.*;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pause against a genuinely RUNNING debuggee (the spin fixture loops until
 * killed): pause must produce a stop with an inspectable stack, and continue
 * must let it run again.
 */
@DisplayName("HXCPP debugger (vshaxe): pause (integration)")
public class HxcppPauseIntegrationTest extends HxcppIntegrationTestBase {

  @BeforeEach
  public void setUp() throws Exception {
    launchFixture("hxcpp.fixture.spin.exe", "Spin.hx");
  }

  @Test
  @DisplayName("pause stops a running program and continue resumes it")
  public void pauseStopsARunningProgramAndContinueResumesIt() throws Exception {
    initializeAndLaunch();
    require(new ConfigurationDoneRequest());

    // let it actually run before interrupting
    Thread.sleep(500);
    assertTrue(debuggee.isAlive(), "spin fixture died prematurely:\n" + output());

    require(new PauseRequest());
    StoppedEvent stopped = (StoppedEvent)awaitEvent(StoppedEvent.class);
    assertEquals("pause", stopped.getBody().getReason());

    StackTraceResponse stack = stackTrace(stopped.getBody().getThreadId());
    assertFalse(stack.getBody().getStackFrames().isEmpty(), "paused stop has no frames");

    sendContinue(stopped.getBody().getThreadId());

    Thread.sleep(300);
    assertTrue(debuggee.isAlive(), "debuggee should still be running after continue");
  }
}
