package com.intellij.plugins.haxe.display.protocol;

/**
 * Wire codes of the std {@code haxe.display.DiagnosticKind}. The shape of a
 * diagnostic's {@code args} depends on its kind. UNRESOLVED_IDENTIFIER
 * carries import and typo suggestions, COMPILER_ERROR and PARSER_ERROR a
 * message string, REMOVABLE_CODE a description and a range, and
 * MISSING_FIELDS the missing members together with the reason they are
 * missing. Haxe 5 renames REMOVABLE_CODE to ReplaceableCode under the same
 * wire code.
 */
public enum DiagnosticKind {
  UNUSED_IMPORT(0),
  UNRESOLVED_IDENTIFIER(1),
  COMPILER_ERROR(2),
  REMOVABLE_CODE(3),
  PARSER_ERROR(4),
  DEPRECATION_WARNING(5),
  INACTIVE_BLOCK(6),
  MISSING_FIELDS(7),
  UNKNOWN(-1);

  private final int code;

  DiagnosticKind(int code) {
    this.code = code;
  }

  public static DiagnosticKind fromCode(int code) {
    for (DiagnosticKind kind : values()) {
      if (kind.code == code) return kind;
    }
    return UNKNOWN;
  }
}
