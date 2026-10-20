package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Whether a completed type's short name works at the position, as the
 * compiler reports it on the type's path. Any other number reads as null.
 */
public enum ImportStatus {
  /** Imported, or in the same module or package: the short name works. */
  IMPORTED(0),
  /** Not imported: the short name needs an import. */
  UNIMPORTED(1),
  /** Another visible type has the same name: only the qualified path names this one. */
  SHADOWED(2);

  private final int code;

  ImportStatus(int code) {
    this.code = code;
  }

  /** The wire code. */
  @JsonValue
  public int code() {
    return code;
  }
}
