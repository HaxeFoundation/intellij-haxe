package com.intellij.plugins.haxe.profiler.bridge;

import java.util.Locale;

/** Human-readable time and size texts shared by the chart, the gutter hints and the tree metrics. */
public final class HaxeProfilerFormats {

  private HaxeProfilerFormats() {
  }

  /**
   * "3288.617 ms – 3288.813 ms (196 µs)": endpoints in the ruler's
   * milliseconds — second-rounded endpoints say nothing about a sub-ms span.
   */
  public static String formatRange(long startUs, long endUs) {
    long durationUs = endUs - startUs;
    String pattern = "%." + rangeDecimals(durationUs) + "f ms";
    return String.format(Locale.ROOT, pattern, startUs / 1000.0) + " – "
           + String.format(Locale.ROOT, pattern, endUs / 1000.0)
           + " (" + formatUs(durationUs) + ")";
  }

  /** Enough fractional digits that the two endpoints visibly differ across the span. */
  private static int rangeDecimals(long durationUs) {
    if (durationUs < 1000) return 3;
    if (durationUs < 10_000) return 2;
    if (durationUs < 100_000) return 1;
    return 0;
  }

  /** One instant on the chart's axis, in the ruler's milliseconds: "3288.617 ms". */
  public static String formatInstant(long us) {
    return String.format(Locale.ROOT, "%.3f ms", us / 1000.0);
  }

  /** 0 B, 512 B, 34.5 KB, 3.2 MB, 1.5 GB - the shortest form for the magnitude. */
  public static String formatBytes(double bytes) {
    if (bytes < 1024) return (long)bytes + " B";
    if (bytes < 1024 * 1024) return trimTrailingZero(bytes / 1024) + " KB";
    if (bytes < 1024L * 1024 * 1024) return trimTrailingZero(bytes / (1024 * 1024)) + " MB";
    return trimTrailingZero(bytes / (1024L * 1024 * 1024)) + " GB";
  }

  /** A percentage reading ("37.5 %"); values arrive on the 0..100 convention curve providers follow. */
  public static String formatPercentValue(double value) {
    String text = String.format(Locale.ROOT, "%.1f", value);
    if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
    return text + " %";
  }

  /** 0, 250 µs, 1.5 ms, 320 ms, 4.2 s - the shortest form for the magnitude. */
  public static String formatUs(long us) {
    if (us == 0) return "0";
    if (us < 1000) return us + " µs";
    if (us < 1_000_000) return trimTrailingZero(us / 1000.0) + " ms";
    return trimTrailingZero(us / 1_000_000.0) + " s";
  }

  private static String trimTrailingZero(double value) {
    String text = String.format(Locale.ROOT, "%.1f", value);
    return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
  }
}
