package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartData.FrameSlice;
import com.intellij.plugins.haxe.profiler.model.PseudoFrames;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Profiler: frame breakdown")
public class HaxeFrameBreakdownTest {
  private static final UsSpan FRAME = new UsSpan(0, 1000);

  @Test
  @DisplayName("unsampled gc moves from script and the rest is idle")
  public void testUnsampledGcMovesFromScriptAndTheRestIsIdle() {
    List<FrameSlice> slices = List.of(new FrameSlice("Main.update", 600), new FrameSlice(PseudoFrames.GC, 100));

    HaxeFrameBreakdownView.Rows rows = HaxeFrameBreakdown.rows(slices, FRAME, List.of(new UsSpan(0, 300)));

    List<Long> summaryTotals = rows.summary().stream().map(HaxeFrameBreakdownView.Row::totalUs).toList();
    assertEquals(List.of(400L, 300L, 300L), summaryTotals, "script, GC, idle");
    List<String> partNames = rows.parts().stream().map(HaxeFrameBreakdownView.Row::name).toList();
    assertEquals(List.of("Main.update", PseudoFrames.GC), partNames);
  }

  @Test
  @DisplayName("parts beyond eight fold into other")
  public void testPartsBeyondEightFoldIntoOther() {
    List<FrameSlice> slices = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      slices.add(new FrameSlice("Main.part" + i, 10));
    }

    HaxeFrameBreakdownView.Rows rows = HaxeFrameBreakdown.rows(slices, FRAME, List.of());

    assertEquals(9, rows.parts().size());
    assertEquals(20, rows.parts().getLast().totalUs());
  }
}
