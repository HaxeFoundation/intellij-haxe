package com.intellij.plugins.haxe.display.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Display protocol: completion list")
public class CompletionListTest {

  /** (completion mode, whether the position qualifies an inserted type). */
  static final List<Arguments> QUALIFYING_MODES = List.of(
    // field: after a package
    arguments(0, true),
    // import and using paths
    arguments(8, true),
    arguments(9, true),
    // toplevel, type hint, new
    arguments(2, false),
    arguments(4, false),
    arguments(10, false));

  @ParameterizedTest(name = "mode {0}")
  @FieldSource("QUALIFYING_MODES")
  @DisplayName("qualifies inserted types")
  public void testQualifiesInsertedTypes(int modeKind, boolean qualifies) {
    CompletionList completion = new CompletionList(List.of(), modeKind, null, null, false);

    assertEquals(qualifies, completion.qualifiesInsertedTypes());
  }
}
