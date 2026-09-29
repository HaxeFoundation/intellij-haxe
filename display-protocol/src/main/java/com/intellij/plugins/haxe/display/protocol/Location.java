package com.intellij.plugins.haxe.display.protocol;

/** A range in a file, as the definition, references and implementation requests return it. */
public record Location(String file, Range range) {
}
