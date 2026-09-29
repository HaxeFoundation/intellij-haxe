package com.intellij.plugins.haxe.display.protocol;

/** Wire codes of std {@code haxe.display.DiagnosticSeverity}. */
public enum DiagnosticSeverity {
  ERROR(1),
  WARNING(2),
  INFORMATION(3),
  HINT(4),
  UNKNOWN(-1);

  private final int code;

  DiagnosticSeverity(int code) {
    this.code = code;
  }

  public static DiagnosticSeverity fromCode(int code) {
    for (DiagnosticSeverity severity : values()) {
      if (severity.code == code) return severity;
    }
    return UNKNOWN;
  }
}
