package com.intellij.plugins.haxe.runner.debugger.flash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The fdb-to-test-console relay: trace lines lose their marker so the SM converter sees the TeamCity protocol at line start. */
@DisplayName("Flash debugger: fdb test console bridge")
public class FdbTestConsoleBridgeTest {

  @Test
  @DisplayName("trace marker is stripped from every line of a chunk")
  public void testTraceMarkerIsStrippedFromEveryLineOfAChunk() {
    String relayed = FdbTestConsoleBridge.unwrapTraceLines("""
      [trace] ##teamcity[testStarted name='CalculatorTest.testAdd']
      [trace] multiplying in a test
      Connecting to debugger...
      """);
    assertEquals("""
      ##teamcity[testStarted name='CalculatorTest.testAdd']
      multiplying in a test
      Connecting to debugger...
      """, relayed, "trace lines unwrapped, fdb's own lines untouched");
  }

  @Test
  @DisplayName("marker inside a line survives")
  public void testMarkerInsideALineSurvives() {
    assertEquals("app printed [trace] literally\n",
                 FdbTestConsoleBridge.unwrapTraceLines("app printed [trace] literally\n"),
                 "only a line-leading marker is fdb's wrapping");
  }
}
