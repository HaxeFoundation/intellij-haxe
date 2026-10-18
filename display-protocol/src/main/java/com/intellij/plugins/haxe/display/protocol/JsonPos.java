package com.intellij.plugins.haxe.display.protocol;

import tools.jackson.databind.JsonNode;

/** A declaration's position as the compiler reports it: the file as the compiler saw it and BYTE offsets into it. */
public record JsonPos(String file, int min, int max) {

  public static JsonPos of(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) return null;
    return new JsonPos(node.path("file").asString(""), node.path("min").asInt(-1), node.path("max").asInt(-1));
  }
}
