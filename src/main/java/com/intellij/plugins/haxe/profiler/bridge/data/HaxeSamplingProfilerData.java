package com.intellij.plugins.haxe.profiler.bridge.data;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.profiler.bridge.HaxeLiveCaptures;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeCallChartTab;
import com.intellij.plugins.haxe.profiler.hxt.HxtSessionTranslator;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import com.intellij.profiler.DummyCallTreeBuilder;
import com.intellij.profiler.api.BaseCallStackElement;
import com.intellij.profiler.api.CallTreeBuildingData;
import com.intellij.profiler.api.MultipleCallTreesProfilerData;
import com.intellij.profiler.model.ThreadInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A sampled Haxe capture for the IU profiler views: the standard tabs with
 * two states, CPU time (samples × the capture's tick period, in
 * microseconds) and raw sample counts, plus the Call Chart built from the
 * same snapshot's time data.
 */
// TODO: range selection on the timeline filtering the call tree to [t1,t2]
//       (rebuild a DummyCallTreeBuilder from the samples inside the range).
public final class HaxeSamplingProfilerData extends HaxeChartedProfilerData {

  /** Synthetic leaf frame of samples taken inside a collector pause. */
  private static final String GC_SYMBOL = "GC Major";

  private final ProfilerSnapshot snapshot;
  /** The session file this data was parsed from; a still-live capture of it renders the self-refreshing view. */
  private final @Nullable Path sourceFile;

  private HaxeSamplingProfilerData(MultipleCallTreesProfilerData trees, ProfilerSnapshot snapshot, @Nullable Path sourceFile) {
    super(trees);
    this.snapshot = snapshot;
    this.sourceFile = sourceFile;
  }

  /** The parsed capture; the gutter hints aggregate their line times from it. */
  @NotNull
  public ProfilerSnapshot snapshot() {
    return snapshot;
  }

  @NotNull
  public static HaxeSamplingProfilerData from(@NotNull ProfilerSnapshot snapshot) {
    return from(snapshot, null);
  }

  @NotNull
  public static HaxeSamplingProfilerData from(@NotNull ProfilerSnapshot snapshot, @Nullable Path sourceFile) {
    Map<Integer, ThreadInfo> threads = new HashMap<>();
    for (ProfilerThread thread : snapshot.threads()) {
      threads.put(thread.id(), new HaxeProfilerThreadInfo(thread.name(), String.valueOf(thread.id())));
    }

    // interned so identical frames merge into one tree node per thread
    Map<StackFrame, HaxeCallStackElement> elements = new HashMap<>();
    HaxeCallStackElement inGc = new HaxeCallStackElement(GC_SYMBOL, null, StackFrame.NO_LINE);
    DummyCallTreeBuilder<BaseCallStackElement> timeBuilder = new DummyCallTreeBuilder<>();
    timeBuilder.setMetric(HaxeValueMetrics.TIME_MICROSECONDS);
    DummyCallTreeBuilder<BaseCallStackElement> samplesBuilder = new DummyCallTreeBuilder<>();
    long periodUs = samplePeriodUs(snapshot);
    for (StackSample sample : snapshot.samples()) {
      List<BaseCallStackElement> stack = new ArrayList<>(sample.frames().size() + 1);
      for (StackFrame frame : sample.frames()) {
        stack.add(elements.computeIfAbsent(frame, HaxeSamplingProfilerData::toElement));
      }
      if (sample.inGc()) {
        stack.add(inGc);
      }
      ThreadInfo thread = threads.computeIfAbsent(sample.threadId(), HaxeSamplingProfilerData::unlistedThread);
      timeBuilder.addStack(thread, stack, sample.weight() * periodUs);
      samplesBuilder.addStack(thread, stack, sample.weight());
    }

    CallTreeBuildingData timeTree = callTree("haxe.profiler.tree.name", timeBuilder, "haxe.hashlink.cpu");
    CallTreeBuildingData samplesTree = callTree("haxe.profiler.tree.samples", samplesBuilder, "haxe.hashlink.samples");
    MultipleCallTreesProfilerData trees = new MultipleCallTreesProfilerData(List.of(timeTree, samplesTree));
    return new HaxeSamplingProfilerData(trees, snapshot, sourceFile);
  }

  /** One sample's worth of time; the sample rate is validated at parse time. */
  public static long samplePeriodUs(ProfilerSnapshot snapshot) {
    return Math.max(1_000_000L / Math.max(snapshot.samplesPerSecond(), 1), 1);
  }

  @NotNull
  private static HaxeCallStackElement toElement(StackFrame frame) {
    return new HaxeCallStackElement(frame.symbol(), frame.file(), frame.line());
  }

  /** A thread sampled without being listed in the capture's thread table. */
  private static ThreadInfo unlistedThread(int threadId) {
    return new HaxeProfilerThreadInfo(ProfilerThread.unnamed(threadId), String.valueOf(threadId));
  }

  @Override
  public boolean isEmpty() {
    return snapshot.samples().isEmpty();
  }

  @Override
  protected @Nullable Path sessionFile() {
    return sourceFile;
  }

  @Override
  protected boolean hasChartData() {
    return !snapshot.samples().isEmpty();
  }

  @Override
  protected @NotNull JComponent createCallChart(@NotNull Project project, HaxeLiveCaptures.@Nullable Entry live,
                                                @NotNull Disposable parent) {
    return live != null
           ? HaxeCallChartTab.createLive(project, snapshot, sourceFile, live, parent)
           : HaxeCallChartTab.create(project, snapshot);
  }

  /** Live captures only come from v1 (.hxtsession) receivers. */
  @Override
  protected @NotNull HaxeChartedProfilerData reparse(@NotNull Path completedFile) throws IOException {
    try (InputStream in = new BufferedInputStream(Files.newInputStream(completedFile))) {
      return from(HxtSessionTranslator.translate(in), completedFile);
    }
  }
}
