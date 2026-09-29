package com.intellij.plugins.haxe.profiler.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * One recorded stack, ROOT-FIRST: {@code frames.get(0)} is the outermost
 * caller and the last frame is where the thread was executing. A sampling
 * capture records weight 1 per sample; a pre-aggregated source (hxcpp's
 * report) folds its counts into {@code weight}.
 *
 * @param time seconds since the target's own capture epoch
 * @param inGc the sample was taken during a collector pause (HashLink's major GC)
 */
public record StackSample(double time, int threadId, @NotNull List<StackFrame> frames, long weight, boolean inGc) {
}
