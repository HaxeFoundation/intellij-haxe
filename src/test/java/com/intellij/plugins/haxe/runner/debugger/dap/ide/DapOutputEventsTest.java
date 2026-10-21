package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DAP debugger: output events")
public class DapOutputEventsTest {

  @Test
  @DisplayName("telemetry is never shown")
  public void testTelemetryIsNeverShown() {
    assertFalse(DapOutputEvents.shownToUser("telemetry", false));
    assertFalse(DapOutputEvents.shownToUser("telemetry", true));
  }

  @Test
  @DisplayName("echoed program output is dropped and adapter messages stay")
  public void testEchoedProgramOutputIsDroppedAndAdapterMessagesStay() {
    assertFalse(DapOutputEvents.shownToUser("stdout", true));
    assertFalse(DapOutputEvents.shownToUser("stderr", true));
    assertTrue(DapOutputEvents.shownToUser("console", true));
    assertTrue(DapOutputEvents.shownToUser("important", true));
    assertTrue(DapOutputEvents.shownToUser(null, true));
  }

  @Test
  @DisplayName("without an echo every program line is shown")
  public void testWithoutAnEchoEveryProgramLineIsShown() {
    assertTrue(DapOutputEvents.shownToUser("stdout", false));
    assertTrue(DapOutputEvents.shownToUser("stderr", false));
  }
}
