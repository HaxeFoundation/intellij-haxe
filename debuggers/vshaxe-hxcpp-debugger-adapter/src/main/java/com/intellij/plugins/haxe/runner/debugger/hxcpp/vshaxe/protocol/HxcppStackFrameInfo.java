package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.protocol;

/**
 * One frame of a {@code stackTrace} result (StackFrameInfo in Protocol.hx).
 * {@code artificial} marks debugger-internal frames.
 */
public record HxcppStackFrameInfo(int id, String name, String source, int line, int column,
                                  Integer endLine, Integer endColumn, Boolean artificial) {
}
