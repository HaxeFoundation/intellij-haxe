package com.intellij.plugins.haxe.execution.console;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Console: utest failure line")
public class HaxeUtestFailureLineTest {

  @Test
  @DisplayName("windows drive path")
  public void testWindowsDrivePath() {
    HaxeUtestFailureLine failure = HaxeUtestFailureLine.parse("C:/work/src/Other.hx:5: expected 1");

    assertNotNull(failure, "the drive letter is part of the path, not a separator");
    assertEquals("C:/work/src/Other.hx", failure.path());
    assertEquals(5, failure.line());
  }

  @Test
  @DisplayName("compiler messages are not failures")
  public void testCompilerMessagesAreNotFailures() {
    assertNull(HaxeUtestFailureLine.parse("src/ShapeTest.hx:12: characters 4-9 : Unknown identifier"));
  }
}
