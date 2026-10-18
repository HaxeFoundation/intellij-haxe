package com.intellij.plugins.haxe.display.protocol;

import org.jetbrains.annotations.Nullable;

/** Whether a completed type's short name works at the position, as the compiler reports it on the type's path. */
public enum ImportStatus {
  /** Imported, or in the same module or package: the short name works. */
  IMPORTED,
  /** Not imported: the short name needs an import. */
  UNIMPORTED,
  /** Another visible type has the same name: only the qualified path names this one. */
  SHADOWED;

  /** The status for the compiler's number (0, 1, 2), or null for anything else. */
  @Nullable
  public static ImportStatus fromWire(int status) {
    return switch (status) {
      case 0 -> IMPORTED;
      case 1 -> UNIMPORTED;
      case 2 -> SHADOWED;
      default -> null;
    };
  }
}
