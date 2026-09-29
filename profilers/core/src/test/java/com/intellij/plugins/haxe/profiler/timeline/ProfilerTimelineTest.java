package com.intellij.plugins.haxe.profiler.timeline;

import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.SeriesPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Profiler: timeline")
public class ProfilerTimelineTest {

  private static final int MAIN_TID = 1;
  private static final int WORKER_TID = 2;

  @Test
  @DisplayName("flame tree merges runs and keeps callee changes as time ordered children")
  public void testFlameTreeMergesRunsAndKeepsCalleeChangesAsTimeOrderedChildren() {
    // 100/s = 10 ms period: [A,B] x3 then [A,C] x2, all contiguous
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    StackFrame c = new StackFrame("Game.render", "Game.hx", 30);
    List<StackSample> samples = new ArrayList<>();
    for (int i = 0; i < 3; i++) samples.add(new StackSample(1.0 + i * 0.01, MAIN_TID, List.of(a, b), 1, false));
    for (int i = 3; i < 5; i++) samples.add(new StackSample(1.0 + i * 0.01, MAIN_TID, List.of(a, c), 1, false));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());

    ProfilerTimeline.FlameNode root = ProfilerTimeline.flameTree(snapshot, MAIN_TID, 8);

    assertEquals(1, root.children().size(), "the shared caller merges across the callee change");
    ProfilerTimeline.FlameNode main = root.children().getFirst();
    assertEquals(a, main.frame());
    assertEquals(5, main.samples());
    assertEquals(50_000, main.durationUs());
    assertEquals(List.of(b, c), main.children().stream().map(ProfilerTimeline.FlameNode::frame).toList());
    assertEquals(30_000, main.children().get(0).endUs(), "the callee runs tile at the switch point");
    assertEquals(30_000, main.children().get(1).startUs());
  }

  @Test
  @DisplayName("flame tree never overlaps siblings under timer jitter at full rate")
  public void testFlameTreeNeverOverlapsSiblingsUnderTimerJitterAtFullRate() {
    // 10000/s = 100 us period, but the samples arrive 90 us apart with the
    // leaf changing every sample - the optimistic period tail must give the
    // overshoot back or children sum past their parent
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    StackFrame c = new StackFrame("Game.render", "Game.hx", 30);
    List<StackSample> samples = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      samples.add(new StackSample(1.0 + i * 0.00009, MAIN_TID, List.of(a, i % 2 == 0 ? b : c), 1, false));
    }
    ProfilerSnapshot snapshot = snapshot(10000, samples, List.of());

    ProfilerTimeline.FlameNode main = ProfilerTimeline.flameTree(snapshot, MAIN_TID, 8).children().getFirst();

    long covered = main.startUs();
    long childSum = 0;
    for (ProfilerTimeline.FlameNode child : main.children()) {
      assertTrue(child.startUs() >= covered, "siblings must never overlap");
      covered = child.endUs();
      childSum += child.durationUs();
    }
    assertTrue(childSum <= main.durationUs(), "children must fit inside their parent");
  }

  @Test
  @DisplayName("flame tree fills gaps with idle nodes")
  public void testFlameTreeFillsGapsWithIdleNodes() {
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    // the thread idles for one second between two runs; B only covers the second run
    List<StackSample> samples = List.of(new StackSample(1.0, MAIN_TID, List.of(a), 1, false),
                                        new StackSample(2.0, MAIN_TID, List.of(a, b), 1, false));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());

    ProfilerTimeline.FlameNode root = ProfilerTimeline.flameTree(snapshot, MAIN_TID, 8);

    assertEquals(3, root.children().size(), "run, idle filler, run");
    assertEquals(a, root.children().get(0).frame());
    assertTrue(root.children().get(1).idle(), "the sampling gap becomes an idle filler");
    assertEquals(a, root.children().get(2).frame());
    ProfilerTimeline.FlameNode secondRun = root.children().get(2);
    assertEquals(List.of(b), secondRun.children().stream().map(ProfilerTimeline.FlameNode::frame).toList());
  }

  @Test
  @DisplayName("flame tree children always fit inside their parent for the model validator")
  public void testFlameTreeChildrenAlwaysFitInsideTheirParentForTheModelValidator() {
    // the flame component rejects a tree whose children exceed the parent -
    // walk a mixed-shape tree and check the invariant everywhere
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    List<StackSample> samples = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      List<StackFrame> frames = i % 3 == 0 ? List.of(a) : List.of(a, b);
      samples.add(new StackSample(1.0 + i * 0.00011, MAIN_TID, frames, 1, false));
    }
    ProfilerSnapshot snapshot = snapshot(10000, samples, List.of());

    assertChildrenFit(ProfilerTimeline.flameTree(snapshot, MAIN_TID, 8));
  }

  private static void assertChildrenFit(ProfilerTimeline.FlameNode node) {
    long childSum = node.children().stream().mapToLong(ProfilerTimeline.FlameNode::durationUs).sum();
    assertTrue(childSum <= node.durationUs(),
               "children (" + childSum + "us) exceed parent (" + node.durationUs() + "us): " + node);
    node.children().forEach(ProfilerTimelineTest::assertChildrenFit);
  }

  @Test
  @DisplayName("flame tree folds frames below the depth cap into their ancestor")
  public void testFlameTreeFoldsFramesBelowTheDepthCapIntoTheirAncestor() {
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    List<StackSample> samples = List.of(new StackSample(1.0, MAIN_TID, List.of(a, b), 1, false));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());

    ProfilerTimeline.FlameNode root = ProfilerTimeline.flameTree(snapshot, MAIN_TID, 1);

    ProfilerTimeline.FlameNode main = root.children().getFirst();
    assertEquals(a, main.frame());
    assertTrue(main.children().isEmpty(), "the capped callee folds into its caller");
  }

  @Test
  @DisplayName("node at walks levels by time for the chart hit test")
  public void testNodeAtWalksLevelsByTimeForTheChartHitTest() {
    // [A,B] x3 then [A,C] x2 at 100/s: one A run [0,50) ms with B on [0,30) and C on [30,50)
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    StackFrame c = new StackFrame("Game.render", "Game.hx", 30);
    List<StackSample> samples = new ArrayList<>();
    for (int i = 0; i < 3; i++) samples.add(new StackSample(1.0 + i * 0.01, MAIN_TID, List.of(a, b), 1, false));
    for (int i = 3; i < 5; i++) samples.add(new StackSample(1.0 + i * 0.01, MAIN_TID, List.of(a, c), 1, false));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());
    ProfilerTimeline.FlameNode root = ProfilerTimeline.flameTree(snapshot, MAIN_TID, 8);

    assertEquals(a, ProfilerTimeline.nodeAt(root, 15_000, 1).frame());
    assertEquals(b, ProfilerTimeline.nodeAt(root, 15_000, 2).frame());
    assertEquals(c, ProfilerTimeline.nodeAt(root, 35_000, 2).frame());
    assertNull(ProfilerTimeline.nodeAt(root, 15_000, 3), "below the deepest sampled frame");
    assertNull(ProfilerTimeline.nodeAt(root, 60_000, 1), "past the capture");
  }

  @Test
  @DisplayName("path to lists the call chain outermost first")
  public void testPathToListsTheCallChainOutermostFirst() {
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    List<StackSample> samples = List.of(new StackSample(1.0, MAIN_TID, List.of(a, b), 1, false),
                                        new StackSample(1.01, MAIN_TID, List.of(a, b), 1, false));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());
    ProfilerTimeline.FlameNode root = ProfilerTimeline.flameTree(snapshot, MAIN_TID, 8);

    List<ProfilerTimeline.FlameNode> path = ProfilerTimeline.pathTo(root, 15_000, 2);

    assertEquals(List.of(a, b), path.stream().map(ProfilerTimeline.FlameNode::frame).toList());
    assertTrue(ProfilerTimeline.pathTo(root, 60_000, 2).isEmpty(), "outside the capture there is no stack");
  }

  @Test
  @DisplayName("node at resolves idle fillers and tree depth counts rows")
  public void testNodeAtResolvesIdleFillersAndTreeDepthCountsRows() {
    StackFrame a = new StackFrame("Main.main", "Main.hx", 1);
    StackFrame b = new StackFrame("Game.update", "Game.hx", 10);
    // a one-second gap between two A runs; only the second has B underneath
    List<StackSample> samples = List.of(new StackSample(1.0, MAIN_TID, List.of(a), 1, false),
                                        new StackSample(2.0, MAIN_TID, List.of(a, b), 1, false));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());
    ProfilerTimeline.FlameNode root = ProfilerTimeline.flameTree(snapshot, MAIN_TID, 8);

    assertTrue(ProfilerTimeline.nodeAt(root, 500_000, 1).idle(), "the gap resolves to the idle filler");
    assertEquals(2, ProfilerTimeline.treeDepth(root), "A above B - two chart rows");
  }

  @Test
  @DisplayName("gc spans merge flagged runs and break on clean samples")
  public void testGcSpansMergeFlaggedRunsAndBreakOnCleanSamples() {
    // 100/s = 10 ms period: two GC samples, a clean one, one more GC; the worker's flag does not bleed in
    List<StackSample> samples = List.of(sample(1.00, MAIN_TID, true),
                                        sample(1.01, MAIN_TID, true),
                                        sample(1.02, MAIN_TID, false),
                                        sample(1.03, MAIN_TID, true),
                                        sample(1.03, WORKER_TID, true));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());

    List<ProfilerTimeline.UsSpan> spans = ProfilerTimeline.gcSpans(snapshot, MAIN_TID);

    assertEquals(List.of(new ProfilerTimeline.UsSpan(0, 20_000), new ProfilerTimeline.UsSpan(30_000, 40_000)), spans);
  }

  @Test
  @DisplayName("gc spans from events sit at frame ends with exact widths and never overlap")
  public void testGcSpansFromEventsSitAtFrameEndsWithExactWidthsAndNeverOverlap() {
    // frames at 16 and 32 ms; 2 ms of GC in each - the second frame's span
    // would reach into the first one's if the clamp failed
    List<ProfilerEvent> events = List.of(new ProfilerEvent(1.016, 0, ProfilerEvent.GC_TIME_CODE, "2000"),
                                         new ProfilerEvent(1.017, 0, ProfilerEvent.GC_TIME_CODE, "17000"),
                                         new ProfilerEvent(1.020, 0, ProfilerEvent.GC_TIME_CODE, "garbage"),
                                         new ProfilerEvent(1.030, 0, ProfilerEvent.FRAME_CODE, ""));
    ProfilerSnapshot snapshot = snapshot(1000, List.of(sample(1.0, MAIN_TID, false)), events);

    List<ProfilerTimeline.UsSpan> spans = ProfilerTimeline.gcSpansFromEvents(snapshot);

    assertEquals(2, spans.size(), "frame markers and unparsable data produce no spans");
    assertEquals(new ProfilerTimeline.UsSpan(14_000, 16_000), spans.get(0));
    assertEquals(new ProfilerTimeline.UsSpan(16_000, 17_000), spans.get(1), "clamped against the previous span");
  }

  @Test
  @DisplayName("frame spans pair consecutive end of frame events")
  public void testFrameSpansPairConsecutiveEndOfFrameEvents() {
    List<ProfilerEvent> events = List.of(new ProfilerEvent(1.0, MAIN_TID, 0, ""),
                                         new ProfilerEvent(1.016, MAIN_TID, 0, ""),
                                         new ProfilerEvent(1.05, MAIN_TID, 0, ""),
                                         // another thread's marker and a custom event do not bound frames
                                         new ProfilerEvent(1.06, WORKER_TID, 0, ""),
                                         new ProfilerEvent(1.07, MAIN_TID, 7, "custom"));
    ProfilerSnapshot snapshot = snapshot(100, List.of(), events);

    List<ProfilerTimeline.UsSpan> spans = ProfilerTimeline.frameSpans(snapshot, MAIN_TID);

    assertEquals(List.of(new ProfilerTimeline.UsSpan(0, 16_000), new ProfilerTimeline.UsSpan(16_000, 50_000)), spans);
  }

  @Test
  @DisplayName("activity series buckets samples into rates")
  public void testActivitySeriesBucketsSamplesIntoRates() {
    // 4 samples inside the first 100 ms bucket, none in the second
    List<StackSample> samples = new ArrayList<>();
    for (int i = 0; i < 4; i++) samples.add(sample(1.0 + i * 0.02, MAIN_TID, false));
    samples.add(sample(1.25, MAIN_TID, false));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());

    List<SeriesPoint> series = ProfilerTimeline.activitySeries(snapshot, 100, false);

    assertEquals(new SeriesPoint(0, 40.0), series.get(0), "4 samples in 100 ms = 40/s");
    assertEquals(new SeriesPoint(100, 0.0), series.get(1));
    assertEquals(new SeriesPoint(200, 10.0), series.get(2));
  }

  @Test
  @DisplayName("gc series counts only flagged samples")
  public void testGcSeriesCountsOnlyFlaggedSamples() {
    List<StackSample> samples = List.of(sample(1.0, MAIN_TID, false),
                                        sample(1.01, MAIN_TID, true),
                                        sample(1.02, MAIN_TID, true));
    ProfilerSnapshot snapshot = snapshot(100, samples, List.of());

    List<SeriesPoint> series = ProfilerTimeline.activitySeries(snapshot, 100, true);

    assertEquals(new SeriesPoint(0, 20.0), series.getFirst(), "2 GC samples in 100 ms = 20/s");
  }

  @Test
  @DisplayName("duration spans samples and events")
  public void testDurationSpansSamplesAndEvents() {
    ProfilerSnapshot snapshot = snapshot(100,
                                         List.of(sample(1.0, MAIN_TID, false)),
                                         List.of(new ProfilerEvent(2.5, MAIN_TID, 0, "")));

    assertEquals(1500, ProfilerTimeline.durationMs(snapshot));
    assertTrue(ProfilerTimeline.captureStartSeconds(snapshot) == 1.0, "the earliest timestamp is the epoch");
  }

  private static StackSample sample(double time, int threadId, boolean inGc) {
    return new StackSample(time, threadId, List.of(), 1, inGc);
  }

  private static ProfilerSnapshot snapshot(int samplesPerSecond, List<StackSample> samples, List<ProfilerEvent> events) {
    List<ProfilerThread> threads = List.of(new ProfilerThread(MAIN_TID, "Main"), new ProfilerThread(WORKER_TID, "worker"));
    return new ProfilerSnapshot("hashlink", 115, samplesPerSecond, threads, samples, events, List.of());
  }
}
