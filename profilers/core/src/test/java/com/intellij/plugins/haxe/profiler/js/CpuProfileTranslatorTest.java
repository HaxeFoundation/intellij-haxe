package com.intellij.plugins.haxe.profiler.js;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JS profiler: cpuprofile translator")
public class CpuProfileTranslatorTest {

  /** (root) -> main(app.js:10) -> update(app.js:20), plus V8's idle and GC nodes. */
  private static final String PROFILE_JSON = """
    {
      "nodes": [
        {"id": 1, "callFrame": {"functionName": "(root)", "url": "", "lineNumber": -1}, "children": [2, 4, 5]},
        {"id": 2, "callFrame": {"functionName": "main", "url": "file:///C:/app/app.js", "lineNumber": 9}, "children": [3]},
        {"id": 3, "callFrame": {"functionName": "update", "url": "file:///C:/app/app.js", "lineNumber": 19}, "children": []},
        {"id": 4, "callFrame": {"functionName": "(idle)", "url": "", "lineNumber": -1}, "children": []},
        {"id": 5, "callFrame": {"functionName": "(garbage collector)", "url": "", "lineNumber": -1}, "children": []}
      ],
      "startTime": 1000000,
      "endTime": 1000500,
      "samples": [2, 3, 4, 5],
      "timeDeltas": [100, 100, 100, 100]
    }""";

  @Test
  @DisplayName("stacks are root paths with 1-based positions and exact microsecond weights")
  public void testStacksAreRootPathsWithOneBasedPositionsAndExactMicrosecondWeights() throws IOException {
    ProfilerSnapshot snapshot = CpuProfileTranslator.translate(stream(PROFILE_JSON));

    assertEquals("v8", snapshot.target());
    assertEquals(1_000_000, snapshot.samplesPerSecond(), "one tick per microsecond keeps times exact");
    List<StackSample> samples = snapshot.samples();
    assertEquals(3, samples.size(), "the (idle) sample is dropped");

    StackSample update = samples.get(1);
    assertEquals(List.of(new StackFrame("main", "C:/app/app.js", 10),
                         new StackFrame("update", "C:/app/app.js", 20)),
                 update.frames(), "root-first path, 0-based lines shifted to 1-based");
    assertEquals(100, update.weight());
    assertEquals(1.0002, update.time(), 1e-9, "startTime plus the cumulative deltas, in seconds");
  }

  @Test
  @DisplayName("garbage collector samples flag inGc")
  public void testGarbageCollectorSamplesFlagInGc() throws IOException {
    ProfilerSnapshot snapshot = CpuProfileTranslator.translate(stream(PROFILE_JSON));

    StackSample last = snapshot.samples().getLast();
    assertTrue(last.inGc());
    assertEquals("(garbage collector)", last.frames().getLast().symbol());
  }

  @Test
  @DisplayName("a file that is not a cpuprofile is refused with a clear message")
  public void testAFileThatIsNotACpuprofileIsRefusedWithAClearMessage() {
    ProfilerFormatException failure = assertThrows(ProfilerFormatException.class,
                                                   () -> CpuProfileTranslator.translate(stream("{\"foo\": 1}")));
    assertTrue(failure.getMessage().contains("cpuprofile"), failure.getMessage());
  }

  private static InputStream stream(String json) {
    return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
  }
}
