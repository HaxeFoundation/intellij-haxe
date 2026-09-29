package com.intellij.plugins.haxe.profiler.bridge.data;

import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerFormats;
import com.intellij.profiler.api.SamplesCountMetric;
import com.intellij.profiler.api.TimeValueMetric;
import com.intellij.profiler.api.ValueMetric;
import com.intellij.profiler.ui.threadview.ThreadMetric;

import java.util.Locale;

/**
 * The metrics the Haxe call trees carry. The stock {@link TimeValueMetric}
 * is hard-wired to milliseconds, which would floor exact sub-millisecond
 * zone times to 0, so time stays in MICROSECONDS with adaptive formatting;
 * thread metrics delegate to the stock singletons.
 */
final class HaxeValueMetrics {

  private HaxeValueMetrics() {
  }

  static final ValueMetric TIME_MICROSECONDS = new ValueMetric() {
    @Override
    public String getName() {
      return HaxeProfilerBundle.message("haxe.profiler.metric.time.name");
    }

    @Override
    public String getUnit() {
      return "µs";
    }

    @Override
    public String getColumnTitle() {
      return HaxeProfilerBundle.message("haxe.profiler.metric.time.column");
    }

    @Override
    public ThreadMetric<Float> getThreadMetric() {
      return TimeValueMetric.INSTANCE.getThreadMetric();
    }

    @Override
    public boolean getHasFixedUnits() {
      return false;
    }

    @Override
    public String formatValue(long us) {
      return HaxeProfilerFormats.formatUs(us);
    }

    @Override
    public String formatUnit(long us) {
      return HaxeProfilerFormats.formatUs(us);
    }
  };

  static final ValueMetric INVOCATIONS = new ValueMetric() {
    @Override
    public String getName() {
      return HaxeProfilerBundle.message("haxe.profiler.metric.invocations.name");
    }

    @Override
    public String getUnit() {
      return "";
    }

    @Override
    public String getColumnTitle() {
      return HaxeProfilerBundle.message("haxe.profiler.metric.invocations.name");
    }

    @Override
    public ThreadMetric<Float> getThreadMetric() {
      return SamplesCountMetric.INSTANCE.getThreadMetric();
    }

    @Override
    public boolean getHasFixedUnits() {
      return false;
    }

    @Override
    public String formatValue(long count) {
      return String.format(Locale.ROOT, "%,d", count);
    }

    @Override
    public String formatUnit(long count) {
      return String.format(Locale.ROOT, "%,d", count);
    }
  };
}
