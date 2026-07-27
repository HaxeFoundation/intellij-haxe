package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.protocol;

/** One thread of a {@code threads} result (ThreadInfo in Protocol.hx). */
public record HxcppThreadInfo(int id, String name) {
}
