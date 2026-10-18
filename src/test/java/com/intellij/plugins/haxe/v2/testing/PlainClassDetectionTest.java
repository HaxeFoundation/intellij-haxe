package com.intellij.plugins.haxe.v2.testing;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Named.named;

/** A class carrying no framework marker: its test-shaped method name alone makes it nobody's test class. */
@DisplayName("Test detection: plain class")
public class PlainClassDetectionTest extends HaxeTestFrameworkDetectionTestBase {

  /** Every framework, named by its library. */
  static List<Named<HaxeTestFramework>> frameworks() {
    return HaxeTestFrameworks.ALL.stream()
      .map(framework -> named(framework.libraryName(), framework))
      .toList();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("frameworks")
  @DisplayName("no framework claims a plain class")
  public void testNoFrameworkClaimsAPlainClass(HaxeTestFramework framework) {
    myFixture.configureByFiles("cases/NotATest.hx");

    assertFalse(framework.isTestClass(classByQName("cases.NotATest")));
  }
}
