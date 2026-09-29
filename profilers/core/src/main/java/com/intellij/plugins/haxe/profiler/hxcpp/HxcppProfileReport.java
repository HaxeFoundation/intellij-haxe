package com.intellij.plugins.haxe.profiler.hxcpp;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A parsed hxcpp profiler report ({@code cpp.vm.Profiler.start/stop}).
 * The source data is AGGREGATED: per-function percentages of the whole run
 * plus a one-level callee breakdown — no timestamps, no threads, no
 * file/line positions, no absolute durations. Entries keep the report's
 * order (total share, descending).
 */
public record HxcppProfileReport(@NotNull List<Entry> entries) {

  /**
   * One profiled function, shares of the whole run. {@code totalPercent}
   * counts only ticks where the function sat on the stack WITH A CALLEE
   * BELOW it — a leaf always executing its own code reports total 0.00%
   * with a large self. Inclusive time is approximately total + self.
   */
  public record Entry(@NotNull String symbol, double totalPercent, double selfPercent,
                      @NotNull List<Callee> callees) {
  }

  /**
   * A callee's share of its CALLER's attribution mass (callees + the
   * caller's own code), as the report prints it — not a share of the run.
   */
  public record Callee(@NotNull String symbol, double percentOfCaller) {
  }
}
