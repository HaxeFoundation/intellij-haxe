package com.intellij.plugins.haxe.display.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Display protocol: completion item")
public class CompletionItemTest {

  /** (kind, name, detail as the decoder fills it, expected dot path). */
  static final List<Arguments> DOT_PATHS = List.of(
    arguments("Type", "Math", "go.math.Math", "go.math.Math"),
    // a secondary type keeps its module segment
    arguments("Type", "Kind", "go.reflect.Reflect.Kind", "go.reflect.Reflect.Kind"),
    arguments("Package", "math", "go.math", "go.math"),
    arguments("Module", "Rand", "go.math", "go.math.Rand"),
    arguments("Module", "StdTypes", "", "StdTypes"),
    arguments("Local", "field", null, null),
    arguments("Keyword", "function", null, null));

  @ParameterizedTest(name = "{0} {1}")
  @FieldSource("DOT_PATHS")
  @DisplayName("dot path")
  public void testDotPath(String kind, String name, String detail, String dotPath) {
    CompletionItem item = new CompletionItem(kind, name, detail, null, null, null, 0, null);

    assertEquals(dotPath, item.dotPath());
  }
}
