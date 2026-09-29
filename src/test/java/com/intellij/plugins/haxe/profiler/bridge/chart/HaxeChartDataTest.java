package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Profiler: chart data")
public class HaxeChartDataTest {

  @Test
  @DisplayName("busy buckets split a span at bucket edges")
  public void testBusyBucketsSplitASpanAtBucketEdges() {
    long[] busy = new long[3];

    HaxeChartData.addBusy(busy, 10, 5, 25);
    List<TracySession.PlotPoint> percent = HaxeChartData.busyPercent(busy, 10, 1000);

    assertArrayEquals(new long[]{5, 10, 5}, busy);
    assertEquals(List.of(new TracySession.PlotPoint(0, 50), new TracySession.PlotPoint(10_000, 100),
                         new TracySession.PlotPoint(20_000, 50)), percent);
  }
}
