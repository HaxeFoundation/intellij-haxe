package com.intellij.plugins.haxe.v2.buildtools.info;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The defines a compiler sets about itself, as the IDE mirrors them for conditional compilation. */
@DisplayName("Define context: compiler identity defines")
public class HaxeCompilerIdentityDefinesTest {

  /** (compiler version, the defines it sets about itself). */
  static Stream<Arguments> versions() {
    return Stream.of(
      arguments("3.4.7", Map.of(
        "haxe_ver", "3.407",
        "haxe", "3.4.7",
        "haxe3", "3.4.7")),
      arguments("4.3.7", Map.of(
        "haxe_ver", "4.307",
        "haxe", "4.3.7",
        "haxe3", "4.3.7",
        "haxe4", "4.3.7")),
      // a pre-release keeps its suffix everywhere but in the number
      arguments("5.0.0-preview.1", Map.of(
        "haxe_ver", "5.000",
        "haxe", "5.0.0-preview.1",
        "haxe3", "5.0.0-preview.1",
        "haxe4", "5.0.0-preview.1",
        "haxe5", "5.0.0-preview.1")),
      // a version that does not parse sets no major-version flag
      arguments("NA", Map.of(
        "haxe_ver", "NA",
        "haxe", "NA")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("versions")
  public void testCompilerIdentityDefines(String version, Map<String, String> expected) {
    assertEquals(expected, HaxeDefineContextService.compilerIdentityDefines(version));
  }
}
