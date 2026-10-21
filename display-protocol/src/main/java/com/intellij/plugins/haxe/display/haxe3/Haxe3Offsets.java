package com.intellij.plugins.haxe.display.haxe3;

/** Where in the text a haxe 3 request must point to be answered about what the caller means. */
final class Haxe3Offsets {

  private Haxe3Offsets() {
  }

  /**
   * The offset to ask at for the identifier starting at the offset. Haxe 3
   * answers at an identifier's first character for the token BEFORE it
   * ({@code trace(|greet} asks about {@code trace}), and inside the
   * identifier or at its end for the identifier itself.
   */
  static int insideIdentifier(String text, int offset) {
    boolean atIdentifier = offset >= 0 && offset < text.length() && isIdentifierPart(text.charAt(offset));
    return atIdentifier ? offset + 1 : offset;
  }

  /** The start of the identifier that ends at the offset; the offset itself when none does. */
  static int identifierStart(String text, int offset) {
    int start = Math.clamp(offset, 0, text.length());
    while (start > 0 && isIdentifierPart(text.charAt(start - 1))) {
      start--;
    }
    return start;
  }

  /** The start of the dotted chain of identifiers that ends at the offset ({@code haxe.ds} before {@code haxe.ds.}). */
  static int dottedChainStart(String text, int offset) {
    int start = Math.clamp(offset, 0, text.length());
    while (start > 0 && (isIdentifierPart(text.charAt(start - 1)) || text.charAt(start - 1) == '.')) {
      start--;
    }
    return start;
  }

  static boolean isIdentifierPart(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }
}
