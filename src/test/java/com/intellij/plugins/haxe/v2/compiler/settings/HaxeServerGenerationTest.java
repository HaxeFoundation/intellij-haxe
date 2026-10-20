package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** Haxe 4 renamed {@code --wait} to {@code --server-listen}; a server starts with the spelling its version knows. */
@DisplayName("Compiler settings: server generation")
public class HaxeServerGenerationTest {

  /** (language level, server generation, the flag a listening server starts with). */
  static final List<Arguments> LEVELS = List.of(
    arguments(HaxeLanguageLevel.HAXE_3_4, HaxeServerGeneration.HAXE_3, "--wait"),
    arguments(HaxeLanguageLevel.HAXE_4_0, HaxeServerGeneration.HAXE_4, "--server-listen"),
    // haxe 5 drops the --wait spelling
    arguments(HaxeLanguageLevel.HAXE_5_0, HaxeServerGeneration.HAXE_4, "--server-listen"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("LEVELS")
  public void testGeneration(HaxeLanguageLevel level, HaxeServerGeneration generation, String listenFlag) {
    assertEquals(generation, HaxeServerGeneration.of(level));
    assertEquals(listenFlag, generation.listenFlag());
  }
}
