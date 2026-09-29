package com.intellij.plugins.haxe.profiler.js;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JS profiler: source map")
public class JsSourceMapTest {

  @Test
  @DisplayName("positions resolve to the floor mapping of their line")
  public void testPositionsResolveToTheFloorMappingOfTheirLine(@TempDir Path directory) throws IOException {
    // line 0: col 0 -> source 0 line 0 ("AAAA"); col 8 -> source 0 line 2 ("QAEA": +8, +0, +2, +0)
    // line 1: no mappings; line 2: col 0 -> source 1 line 9 ("ACOA": +0, +1, +7, +0)
    JsSourceMap map = parsed(directory, """
      {"version": 3, "sources": ["a.hx", "b.hx"], "names": [], "mappings": "AAAA,QAEA;;ACOA"}""");

    String aSource = directory.resolve("a.hx").toString().replace('\\', '/');
    String bSource = directory.resolve("b.hx").toString().replace('\\', '/');
    assertEquals(new JsSourceMap.Position(aSource, 1), map.resolve(0, 0));
    assertEquals(new JsSourceMap.Position(aSource, 1), map.resolve(0, 7), "columns before the next mapping floor down");
    assertEquals(new JsSourceMap.Position(aSource, 3), map.resolve(0, 8));
    assertEquals(new JsSourceMap.Position(aSource, 3), map.resolve(0, 500), "columns past the last mapping keep it");
    assertEquals(new JsSourceMap.Position(bSource, 10), map.resolve(2, 0), "source deltas carry across lines");

    assertNull(map.resolve(1, 0), "a line without mappings has no answer");
    assertNull(map.resolve(40, 0), "a line past the mappings has no answer");
  }

  @Test
  @DisplayName("file urls and relative sources both become plain paths")
  public void testFileUrlsAndRelativeSourcesBothBecomePlainPaths(@TempDir Path directory) throws IOException {
    JsSourceMap map = parsed(directory, """
      {"version": 3, "sources": ["file:///C:/proj/src/Main.hx"], "names": [], "mappings": "AAAA"}""");

    JsSourceMap.Position position = map.resolve(0, 0);
    assertEquals("C:/proj/src/Main.hx", position.file());
  }

  @Test
  @DisplayName("a non-v3 map is rejected")
  public void testANonV3MapIsRejected(@TempDir Path directory) throws IOException {
    Path mapFile = directory.resolve("bad.js.map");
    Files.writeString(mapFile, """
      {"version": 2, "mappings": ""}""");

    assertThrows(ProfilerFormatException.class, () -> JsSourceMap.parse(mapFile));
  }

  private static JsSourceMap parsed(Path directory, String json) throws IOException {
    Path mapFile = directory.resolve("app.js.map");
    Files.writeString(mapFile, json);
    return JsSourceMap.parse(mapFile);
  }
}
