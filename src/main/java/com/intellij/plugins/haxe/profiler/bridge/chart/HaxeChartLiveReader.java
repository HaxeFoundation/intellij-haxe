package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.profiler.hxt.HxtLiveSession;
import com.intellij.plugins.haxe.profiler.hxt.HxtZoneStore;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Re-reads one kind of still-streaming session file for the live chart; runs on a pooled thread. */
interface HaxeChartLiveReader {

  /** Fresh chart data when the file grew since the last call (always when {@code force}), else null. */
  @Nullable
  HaxeChartData reload(boolean force) throws IOException;

  /** The thread's tree for the window [fromUs, toUs]. */
  @NotNull
  FlameNode treeFor(HaxeChartData data, int threadId, long fromUs, long toUs);

  /** How often the live chart polls the file. */
  int refreshMs();

  /** Sample streams: an incremental reader consumes only the newly appended records. */
  static HaxeChartLiveReader forSamples(Path sessionFile) {
    return new HaxeChartLiveReader() {
      private HxtLiveSession reader;

      @Override
      public @Nullable HaxeChartData reload(boolean force) throws IOException {
        if (reader == null) {
          reader = HxtLiveSession.open(sessionFile);
        }
        boolean grew = reader.poll();
        return force || grew ? new HaxeSnapshotChartData(reader.snapshot()) : null;
      }

      @Override
      public @NotNull FlameNode treeFor(HaxeChartData data, int threadId, long fromUs, long toUs) {
        HaxeSnapshotChartData samples = (HaxeSnapshotChartData)data;
        return ProfilerTimeline.flameTree(samples.snapshot(), threadId, HaxeChartData.MAX_DEPTH, fromUs, toUs);
      }

      @Override
      public int refreshMs() {
        return 250;
      }
    };
  }

  /**
   * Zone stores: the record index rescans from scratch on every reload
   * (headers only, payloads are skipped), so growth gates on the file size
   * and the poll runs slower. The activity curve folds incrementally from
   * the newly appended chunks; refolding the whole file made each refresh
   * slower than the last.
   */
  static HaxeChartLiveReader forZones(Path sessionFile) {
    return new HaxeChartLiveReader() {
      private final HaxeLiveZoneChartData.Activity activity = new HaxeLiveZoneChartData.Activity();
      private long lastSize = -1;

      @Override
      public @Nullable HaxeChartData reload(boolean force) throws IOException {
        long size = Files.size(sessionFile);
        if (!force && size == lastSize) return null;
        lastSize = size;
        HxtZoneStore store = HxtZoneStore.open(sessionFile);
        activity.foldNewChunks(store);
        return new HaxeLiveZoneChartData(new HaxeZoneChartData(store), activity);
      }

      @Override
      public @NotNull FlameNode treeFor(HaxeChartData data, int threadId, long fromUs, long toUs) {
        return data.treeFor(threadId, fromUs, toUs, data.durationUs() / HaxeChartWindowLoader.RESOLUTION);
      }

      @Override
      public int refreshMs() {
        return 1_000;
      }
    };
  }
}
