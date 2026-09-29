package com.intellij.plugins.haxe.display.protocol;

import tools.jackson.databind.JsonNode;

/**
 * A line and character position in a display RESULT, both 0-BASED. The std
 * {@code Position.hx} docs claim 1-based values, but the compiler converts
 * them to 0-based for LSP.
 */
public record Position(int line, int character) {

  /** Decodes the wire {@code {line, character}} object; absent fields read as 0. */
  public static Position fromJson(JsonNode node) {
    return new Position(node.path("line").asInt(0), node.path("character").asInt(0));
  }
}
