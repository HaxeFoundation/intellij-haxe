package com.intellij.plugins.haxe.display.protocol;

/**
 * The part of a {@code display/hover} result the plugin reads: the hovered
 * range, the item kind ({@code Local}, {@code ClassField}, {@code Type}, ...)
 * and the item's resolved type.
 */
public record HoverInfo(Range range, String itemKind, JsonTypeRef type) {
}
