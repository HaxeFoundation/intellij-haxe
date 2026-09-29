package com.intellij.plugins.haxe.profiler.bridge.data;

import com.intellij.plugins.haxe.profiler.hxcpp.HxcppProfileReport;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.profiler.DummyCallTreeBuilder;
import com.intellij.profiler.api.BaseCallStackElement;
import com.intellij.profiler.api.CallTreeBuildingData;
import com.intellij.profiler.api.SingleCallTreeProfilerData;
import com.intellij.profiler.model.ThreadInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A parsed hxcpp profiler report for the IU profiler views. The source data
 * is aggregated percentages with no samples and no time axis, so only the
 * standard call-tree family of tabs appears, flat: one root per function,
 * weighted by its SELF share in basis points (85.61% → 8561). The method
 * list is the first-class view; deeper nesting would have to be invented,
 * since the report only carries one level of callee shares.
 */
// TODO: a callers/callees detail view from HxcppProfileReport.Entry.callees()
//       — the one-level breakdown a flat tree cannot show.
public final class HaxeHxcppProfilerData extends SingleCallTreeProfilerData {

  private HaxeHxcppProfilerData(CallTreeBuildingData tree) {
    super(tree);
  }

  @NotNull
  public static HaxeHxcppProfilerData from(@NotNull HxcppProfileReport report) {
    // the report is per-process with no thread data; hxcpp profiles the thread that called start()
    ThreadInfo thread = new HaxeProfilerThreadInfo(ProfilerThread.MAIN_NAME, "0");
    DummyCallTreeBuilder<BaseCallStackElement> builder = new DummyCallTreeBuilder<>();
    for (HxcppProfileReport.Entry entry : report.entries()) {
      // basis points keep the report's two decimals as integer weights
      long weight = Math.round(entry.selfPercent() * 100);
      if (weight <= 0) continue;
      HaxeCallStackElement element = new HaxeCallStackElement(entry.symbol(), null, StackFrame.NO_LINE);
      builder.addStack(thread, List.of(element), weight);
    }

    return new HaxeHxcppProfilerData(HaxeChartedProfilerData.callTree("haxe.profiler.tree.name", builder, "haxe.hxcpp.cpu"));
  }
}
