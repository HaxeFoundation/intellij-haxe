package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.plugins.haxe.config.HaxeTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The one shared answer to "does a debugger lane exist" - program and test actions both gate on it. */
@DisplayName("Run configurations: debug support")
public class HaxeDebugSupportTest {

  /** (target, program-session lane exists, test-session lane exists). */
  static final List<Arguments> DEBUG_LANES = List.of(
    arguments(HaxeTarget.HL, true, true),
    arguments(HaxeTarget.CPP, true, true),
    // js tests run under node with the js-debug adapter attached
    arguments(HaxeTarget.JAVA_SCRIPT, true, true),
    // fdb hosts flash tests and the test console parses its relayed traces
    arguments(HaxeTarget.FLASH, true, true),
    // programs have no interp lane; the eval adapter serves tests
    arguments(HaxeTarget.INTERP, false, true),
    arguments(HaxeTarget.NEKO, false, false),
    // no target resolved: never debuggable
    arguments(null, false, false));

  @ParameterizedTest(name = "{0}")
  @FieldSource("DEBUG_LANES")
  @DisplayName("debug lanes per target and session kind")
  public void testDebugLanesPerTargetAndSessionKind(HaxeTarget target, boolean programDebug, boolean testDebug) {
    assertEquals(programDebug, HaxeDebugSupport.supportsProgramDebug(target), "program session");
    assertEquals(testDebug, HaxeDebugSupport.supportsTestDebug(target), "test session");
  }
}
