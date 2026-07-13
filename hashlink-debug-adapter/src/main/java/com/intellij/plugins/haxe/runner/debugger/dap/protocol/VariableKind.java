package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a variable is classified, so the client can pick a node icon. Serialized as
 * its lowercase wire string; an unknown or absent value decodes to
 * {@link #UNSPECIFIED} (never {@code null}), so a newer adapter emitting a kind
 * this client doesn't know never breaks it.
 */
public enum VariableKind {
  UNSPECIFIED("unspecified"),
  ARGUMENT("argument"),
  LOCAL("local"),
  STATIC("static"),
  FIELD("field");

  private final String wire;

  VariableKind(String wire) {
    this.wire = wire;
  }

  @JsonValue
  public String wire() {
    return wire;
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static VariableKind fromWire(String wire) {
    if (wire != null) {
      for (VariableKind kind : values()) {
        if (kind.wire.equals(wire)) {
          return kind;
        }
      }
    }
    return UNSPECIFIED;
  }
}
