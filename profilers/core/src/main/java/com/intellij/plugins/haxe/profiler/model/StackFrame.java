package com.intellij.plugins.haxe.profiler.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One frame of a sampled stack. {@code symbol} is the qualified name as the
 * target reports it (typically {@code pack.Class.method}); {@code file} uses
 * forward slashes and is null when the target had no source position, with
 * {@code line} then {@link #NO_LINE}.
 */
public record StackFrame(@NotNull String symbol, @Nullable String file, int line) {

  public static final int NO_LINE = -1;
}
