package com.intellij.plugins.haxe.display.protocol;

/** A declaration's position as the compiler reports it: the file as the compiler saw it and BYTE offsets into it. */
public record JsonPos(String file, int min, int max) {
}
