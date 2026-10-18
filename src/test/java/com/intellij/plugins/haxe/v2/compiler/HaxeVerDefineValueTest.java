package com.intellij.plugins.haxe.v2.compiler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The compiler defines {@code haxe_ver} as a number (4.307 for 4.3.7); the IDE mirrors that form, not the dotted version. */
@DisplayName("Compiler settings: haxe_ver define value")
public class HaxeVerDefineValueTest {

  /** (dotted version, haxe_ver value). */
  static final List<Arguments> VERSIONS = List.of(
    arguments("4.3.7", "4.307"),
    arguments("4.0.2", "4.002"),
    arguments("4.2.0", "4.200"),
    arguments("3.4.7", "3.407"),
    // a pre-release suffix does not reach the number
    arguments("5.0.0-preview.1", "5.000"),
    // an unparsable version passes through
    arguments("NA", "NA"));

  @ParameterizedTest(name = "{0} is {1}")
  @FieldSource("VERSIONS")
  public void testHaxeVerDefineValue(String version, String expected) {
    assertEquals(expected, HaxeLanguageLevelUtil.haxeVerDefineValue(version));
  }
}
