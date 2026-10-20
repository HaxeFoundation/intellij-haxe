package com.intellij.plugins.haxe.profiler.model;

import org.jetbrains.annotations.NotNull;

/**
 * One instant event on a capture's timeline: a mark with a label the program
 * emitted (a tracy message, for example). Times are session-relative
 * nanoseconds; {@code color} is RGB with 0 meaning unspecified.
 */
public record TimelineEvent(int threadId, long timeNs, @NotNull String text, int color) {
}
