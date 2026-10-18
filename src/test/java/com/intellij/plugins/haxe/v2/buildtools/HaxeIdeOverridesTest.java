package com.intellij.plugins.haxe.v2.buildtools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: IDE overrides")
public class HaxeIdeOverridesTest {

  /** (stored custom target, whether the build selects a target itself, the custom target analysis uses). */
  static final List<Arguments> CUSTOM_TARGET_RULE = List.of(
    arguments("go", false, "go"),
    // the build's own target wins: a second target fails the compile
    arguments("go", true, null),
    arguments(null, false, null));

  @ParameterizedTest(name = "{0}, build selects a target: {1}")
  @FieldSource("CUSTOM_TARGET_RULE")
  @DisplayName("custom target for")
  public void testCustomTargetFor(String stored, boolean buildSelectsTarget, String used) {
    assertEquals(used, HaxeIdeOverrides.customTargetFor(stored, buildSelectsTarget));
  }
}
