package com.intellij.plugins.haxe.display.protocol;

import tools.jackson.databind.JsonNode;

/** A text range between two (0-based) {@link Position}s. */
public record Range(Position start, Position end) {

  /** Decodes the wire {@code {start, end}} object. */
  public static Range fromJson(JsonNode node) {
    return new Range(Position.fromJson(node.path("start")), Position.fromJson(node.path("end")));
  }
}
