package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.lang.LanguageNamesValidation;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Rename: names validator")
public class HaxeNamesValidatorTest extends HaxeLightFixtureTestCase {

  /** (candidate name, whether Haxe accepts it as a name). */
  static final List<Arguments> CANDIDATES = List.of(
    arguments("count", true),
    arguments("_count1", true),
    // a soft keyword is a name everywhere but in its own construct
    arguments("to", true),
    arguments("class", false),
    arguments("null", false),
    arguments("1count", false),
    arguments("my-name", false),
    // the compiler's identifiers are ASCII only
    arguments("größe", false),
    arguments("", false));

  @Override
  protected String getBasePath() {
    return "";
  }

  // the index keeps the empty candidate's display name from being blank
  @ParameterizedTest(name = "[{index}] {0}")
  @FieldSource("CANDIDATES")
  public void testTheRegisteredValidatorDecidesWhatMayBeAName(String candidate, boolean accepted) {
    boolean isIdentifier = LanguageNamesValidation.isIdentifier(HaxeLanguage.INSTANCE, candidate, getProject());

    assertEquals(accepted, isIdentifier, candidate);
  }
}
