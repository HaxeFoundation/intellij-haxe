package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

/** Wire codes of std {@code haxe.display.DiagnosticSeverity}. */
public enum DiagnosticSeverity {
  ERROR(1),
  WARNING(2),
  INFORMATION(3),
  HINT(4),
  @JsonEnumDefaultValue
  UNKNOWN(-1);

  private final int code;

  DiagnosticSeverity(int code) {
    this.code = code;
  }

  /** The wire code. */
  @JsonValue
  public int code() {
    return code;
  }
}
