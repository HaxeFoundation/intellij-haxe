package com.intellij.plugins.haxe.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Resolve: module variant file names")
public class HaxeModuleVariantsTest {

  /** (module file name without .hx, expected variant or null, expected base module name). */
  static final List<Arguments> VARIANT_SHAPES = List.of(
    arguments("String.go", "go", "String"),
    arguments("String", null, "String"),
    arguments("Syntax.macro", "macro", "Syntax"),
    arguments("UInt64.custom_js2", "custom_js2", "UInt64"),
    // only the LAST segment is the variant; the rest stays the module name
    arguments("A.b.c", "c", "A.b"),
    // an uppercase segment is not a variant name (platform and custom-target names are lowercase)
    arguments("String.Go", null, "String.Go"),
    // punctuation disqualifies the segment
    arguments("String.go-x", null, "String.go-x"),
    // degenerate dots are left alone
    arguments("String.", null, "String."),
    arguments(".go", null, ".go"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("VARIANT_SHAPES")
  @DisplayName("variant and base module derive from the file name")
  public void variantAndBaseModuleDeriveFromTheFileName(String moduleFileName, String variant, String baseModule) {
    assertEquals(variant, HaxeModuleVariants.variantOf(moduleFileName));
    assertEquals(baseModule, HaxeModuleVariants.moduleNameOf(moduleFileName));
  }
}
