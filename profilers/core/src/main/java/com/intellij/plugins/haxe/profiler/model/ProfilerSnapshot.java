package com.intellij.plugins.haxe.profiler.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A profiling capture in target-neutral form: the threads that were sampled,
 * every recorded stack sample in capture order, the target's custom events,
 * and per-frame heap readings when the target reports them. Deliberately
 * dumber than any one source format — aggregation (identical-stack merging),
 * inversion and filtering are viewer concerns and happen downstream;
 * translators fill this losslessly.
 *
 * @param target           which collector produced the capture (e.g. "hashlink")
 * @param formatVersion    the source format's own version stamp
 * @param samplesPerSecond the sampling rate the capture ran at; 0 when the source does not record one
 */
public record ProfilerSnapshot(@NotNull String target,
                               int formatVersion,
                               int samplesPerSecond,
                               @NotNull List<ProfilerThread> threads,
                               @NotNull List<StackSample> samples,
                               @NotNull List<ProfilerEvent> events,
                               @NotNull List<ProfilerMemorySample> memory) {
}
