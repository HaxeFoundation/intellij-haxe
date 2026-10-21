package com.intellij.plugins.haxe.display.protocol;

/**
 * A line and character position in a display RESULT, both 0-BASED. The std
 * {@code Position.hx} docs claim 1-based values, but the compiler converts
 * them to 0-based for LSP.
 */
public record Position(int line, int character) {
}
