package com.intellij.plugins.haxe.profiler.js;

import com.intellij.plugins.haxe.profiler.hxt.HxtSessionTranslator;
import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("JS profiler: cpuprofile session builder")
public class CpuProfileSessionBuilderTest {

  /** (root) -> main(app.js:10) -> update(app.js:20), plus V8's idle and GC nodes. */
  private static final String NODES_JSON = """
    "nodes": [
      {"id": 1, "callFrame": {"functionName": "(root)", "url": "", "lineNumber": -1}, "children": [2, 4, 5]},
      {"id": 2, "callFrame": {"functionName": "main", "url": "file:///C:/app/app.js", "lineNumber": 9}, "children": [3]},
      {"id": 3, "callFrame": {"functionName": "update", "url": "file:///C:/app/app.js", "lineNumber": 19}, "children": []},
      {"id": 4, "callFrame": {"functionName": "(idle)", "url": "", "lineNumber": -1}, "children": []},
      {"id": 5, "callFrame": {"functionName": "(garbage collector)", "url": "", "lineNumber": -1}, "children": []}
    ]""";

  /** Two stop/start segments on one V8 clock, 400 us of stopped gap between them. */
  private static final String FIRST_SEGMENT_JSON = """
    {
      %s,
      "startTime": 1000000,
      "endTime": 1000500,
      "samples": [2, 3, 4, 5],
      "timeDeltas": [100, 100, 100, 100]
    }""".formatted(NODES_JSON);
  private static final String SECOND_SEGMENT_JSON = """
    {
      %s,
      "startTime": 1000900,
      "endTime": 1001100,
      "samples": [3, 3],
      "timeDeltas": [100, 100]
    }""".formatted(NODES_JSON);

  @Test
  @DisplayName("segments append on one rebased clock without posing as display frames")
  public void testSegmentsAppendOnOneRebasedClockWithoutPosingAsDisplayFrames() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CpuProfileSessionBuilder builder = new CpuProfileSessionBuilder(out);
    builder.appendSegment(FIRST_SEGMENT_JSON);
    builder.appendSegment(SECOND_SEGMENT_JSON);
    assertEquals(out.size(), builder.bytesWritten());

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(new ByteArrayInputStream(out.toByteArray()));

    assertEquals(CpuProfileTranslator.TARGET, snapshot.target());
    boolean frameEvents = snapshot.events().stream().anyMatch(event -> event.code() == ProfilerEvent.FRAME_CODE);
    assertFalse(frameEvents, "segment flushes are collection windows, not display frames");
    assertTrue(snapshot.memory().isEmpty(), "V8 supplies no heap readings - no zero-valued memory lane");

    double firstSampleSeconds = snapshot.samples().get(0).time();
    assertTrue(firstSampleSeconds < 0.001, "times rebase to the first segment's start, got " + firstSampleSeconds);
  }

  @Test
  @DisplayName("positions survive the v1 name round trip")
  public void testPositionsSurviveTheV1NameRoundTrip() throws IOException {
    ProfilerSnapshot snapshot = transcodedBoth();

    boolean updatePositioned = snapshot.samples().stream()
      .flatMap(sample -> sample.frames().stream())
      .anyMatch(CpuProfileSessionBuilderTest::isPositionedUpdateFrame);
    assertTrue(updatePositioned, "update(C:/app/app.js:20) keeps file and 1-based line through the session");
  }

  @Test
  @DisplayName("idle, GC and the stop gap tile the clock")
  public void testIdleGcAndTheStopGapTileTheClock() throws IOException {
    ProfilerSnapshot snapshot = transcodedBoth();

    long idleUs = weightOf(snapshot, "[idle]");
    assertEquals(100 + 400, idleUs, "the (idle) sample plus the 400 us stop-to-start gap");

    assertEquals(100, weightOf(snapshot, "[gc]"), "the (garbage collector) sample");
    boolean gcTimed = snapshot.events().stream()
      .anyMatch(event -> event.code() == ProfilerEvent.GC_TIME_CODE && "100".equals(event.data()));
    assertTrue(gcTimed, "GC weight also counts into the frame's GC time");

    // 400 sampled + 400 gap + 200 sampled; segment 1's endTime slack (100 us
    // between its last sample and its stamp) is not fabricated into anything
    long totalUs = snapshot.samples().stream().mapToLong(StackSample::weight).sum();
    assertEquals(1000, totalUs, "samples tile the clock with nothing double counted");
  }

  @Test
  @DisplayName("observed display frames split a segment into real frame windows")
  public void testObservedDisplayFramesSplitASegmentIntoRealFrameWindows() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CpuProfileSessionBuilder builder = new CpuProfileSessionBuilder(out);
    // DrawFrame instants on the profile's own clock, mid-sample on purpose
    builder.appendSegment(FIRST_SEGMENT_JSON, List.of(1_000_150L, 1_000_350L), 1_000_000, 2_000_000);

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(new ByteArrayInputStream(out.toByteArray()));

    List<Double> frameStamps = snapshot.events().stream()
      .filter(event -> event.code() == ProfilerEvent.FRAME_CODE)
      .map(ProfilerEvent::time)
      .toList();
    assertEquals(List.of(0.00015, 0.00035), frameStamps,
                 "the DrawFrame instants chart as frames; the flush boundary at 500 us does not");

    assertEquals(3, snapshot.memory().size(), "every window of the segment carries the polled heap reading");
    assertEquals(1_000_000, snapshot.memory().get(0).usedBytes());
    assertEquals(2_000_000, snapshot.memory().get(0).reservedBytes());

    long totalUs = snapshot.samples().stream().mapToLong(StackSample::weight).sum();
    assertEquals(400, totalUs, "splitting a sample at a boundary never changes its total weight");

    List<String> gcTimes = snapshot.events().stream()
      .filter(event -> event.code() == ProfilerEvent.GC_TIME_CODE)
      .map(ProfilerEvent::data)
      .toList();
    assertEquals(List.of("50", "50"), gcTimes, "the gc sample's 100 us splits across the boundary at 350");
  }

  @Test
  @DisplayName("a source map redirects sampled positions to the haxe sources")
  public void testASourceMapRedirectsSampledPositionsToTheHaxeSources(@TempDir Path directory) throws IOException {
    // one mapping on generated line 9 column 0 -> source 0 line 41 (both 0-based)
    Path mapFile = directory.resolve("out").resolve("app.js.map");
    Files.createDirectories(mapFile.getParent());
    Files.writeString(mapFile, """
      {"version": 3, "sources": ["../src/Main.hx"], "names": [], "mappings": ";;;;;;;;;AAyCA"}""");
    JsSourceMap map = JsSourceMap.parse(mapFile);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CpuProfileSessionBuilder builder = new CpuProfileSessionBuilder(out, url -> map);
    builder.appendSegment(FIRST_SEGMENT_JSON);
    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(new ByteArrayInputStream(out.toByteArray()));

    String expectedSource = directory.resolve("src").resolve("Main.hx").toString().replace('\\', '/');
    boolean mainMapped = snapshot.samples().stream()
      .flatMap(sample -> sample.frames().stream())
      .anyMatch(frame -> frame.symbol().equals("Main.main") && expectedSource.equals(frame.file()) && frame.line() == 42);
    assertTrue(mainMapped, "main sits on generated line 9, maps to Main.hx:42 and gains its module for identity");
  }

  @Test
  @DisplayName("a script-less frame reads as a browser-native call")
  public void testAScriptLessFrameReadsAsABrowserNativeCall() throws IOException {
    // main(app.js) -> setTransform, whose callFrame has no url: canvas API execution
    String segment = """
      {
        "nodes": [
          {"id": 1, "callFrame": {"functionName": "(root)", "url": "", "lineNumber": -1}, "children": [2]},
          {"id": 2, "callFrame": {"functionName": "main", "url": "file:///C:/app/app.js", "lineNumber": 9}, "children": [3]},
          {"id": 3, "callFrame": {"functionName": "setTransform", "url": "", "lineNumber": -1}, "children": []}
        ],
        "startTime": 1000000,
        "endTime": 1000100,
        "samples": [3],
        "timeDeltas": [100]
      }""";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CpuProfileSessionBuilder builder = new CpuProfileSessionBuilder(out);
    builder.appendSegment(segment);

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(new ByteArrayInputStream(out.toByteArray()));

    List<String> leafStack = snapshot.samples().get(0).frames().stream().map(StackFrame::symbol).toList();
    assertEquals(List.of("main", "setTransform (native)"), leafStack,
                 "an extern's target has no script or position - named so it does not read as a failed mapping");
  }

  /** (generated js name, demangled symbol). */
  static final List<Arguments> MANGLED_NAMES = List.of(
    arguments("openfl_display3D_textures_RectangleTexture", "openfl.display3D.textures.RectangleTexture"),
    arguments("openfl_display__$internal_Context3DShape.render", "openfl.display._internal.Context3DShape.render"),
    arguments("lime__$internal_backend_html5_HTML5Application", "lime._internal.backend.html5.HTML5Application"),
    arguments("__updateGL", "__updateGL"),
    arguments("createRectangleTexture", "createRectangleTexture"),
    arguments("_$Main", "_$Main"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("MANGLED_NAMES")
  @DisplayName("generated js names demangle to dotted haxe paths")
  public void testGeneratedJsNamesDemangleToDottedHaxePaths(String mangled, String demangled) {
    assertEquals(demangled, CpuProfileSessionBuilder.demangle(mangled));
  }

  private static boolean isPositionedUpdateFrame(StackFrame frame) {
    return frame.symbol().equals("update") && "C:/app/app.js".equals(frame.file()) && frame.line() == 20;
  }

  private static long weightOf(ProfilerSnapshot snapshot, String leafSymbol) {
    return snapshot.samples().stream()
      .filter(sample -> sample.frames().get(sample.frames().size() - 1).symbol().equals(leafSymbol))
      .mapToLong(StackSample::weight)
      .sum();
  }

  private static ProfilerSnapshot transcodedBoth() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CpuProfileSessionBuilder builder = new CpuProfileSessionBuilder(out);
    builder.appendSegment(FIRST_SEGMENT_JSON);
    builder.appendSegment(SECOND_SEGMENT_JSON);
    return HxtSessionTranslator.translate(new ByteArrayInputStream(out.toByteArray()));
  }
}
