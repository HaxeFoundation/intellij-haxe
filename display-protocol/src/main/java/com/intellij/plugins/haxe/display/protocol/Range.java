package com.intellij.plugins.haxe.display.protocol;

/** A text range between two (0-based) {@link Position}s. */
public record Range(Position start, Position end) {
}
