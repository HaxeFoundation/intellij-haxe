package com.intellij.plugins.haxe.profiler.tracy.wire;

import org.jetbrains.annotations.NotNull;

/**
 * The client's packed welcome message, sent right after a successful
 * handshake, plus the protocol version that handshake settled on.
 * {@code timerMul} converts the client's raw timer ticks to NANOSECONDS;
 * {@code epoch} is the unix time the program started; {@code onDemand}
 * mirrors the client's TRACY_ON_DEMAND build flag; {@code delay} is the
 * queue-delay calibration versions before v76 sent (0 afterwards).
 */
public record TracyWelcome(@NotNull TracyProtocolVersion protocolVersion, double timerMul, long initBegin,
                           long initEnd, long delay, long resolution, long epoch, long execTime, long pid,
                           long samplingPeriod, boolean onDemand, @NotNull String programName) {
}
