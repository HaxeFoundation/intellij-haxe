package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Library;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Requirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Nodes by name whatever the case, edges both ways, closures per container. */
@DisplayName("Build tools: library graph")
public class HaxeLibraryGraphTest {

  private static final Library HAXEPARSER = new Library("haxeparser", "4.3.0", List.of(new Requirement("hxparse", null)), List.of(), null);
  private static final Library HXPARSE = new Library("hxparse", "4.3.0", List.of(), List.of(), null);
  private static final Library UTEST = new Library("utest", "1.13.2", List.of(), List.of(), null);

  @Test
  @DisplayName("libraries are found whatever the case")
  public void testLibrariesAreFoundWhateverTheCase() {
    HaxeLibraryGraph graph = new HaxeLibraryGraph();
    graph.replaceAll(Map.of("haxeparser", HAXEPARSER, "HxParse", HXPARSE, "utest", UTEST), Map.of());

    assertEquals(HXPARSE, graph.library("hxparse"));
    assertEquals(List.of(new Requirement("hxparse", null)), graph.library("HaxeParser").requires());
    assertNull(graph.library("unknown"));
  }

  @Test
  @DisplayName("the graph knows whether a sync landed")
  public void testTheGraphKnowsWhetherASyncLanded() {
    HaxeLibraryGraph graph = new HaxeLibraryGraph();
    assertFalse(graph.hasSynced(), "empty before the first sync");

    graph.replaceAll(Map.of(), Map.of());

    assertTrue(graph.hasSynced(), "a sync that resolved nothing still landed");
  }

  @Test
  @DisplayName("a closure is per container and declared library")
  public void testAClosureIsPerContainerAndDeclaredLibrary() {
    HaxeLibraryGraph graph = new HaxeLibraryGraph();
    graph.replaceAll(Map.of(), Map.of("app", Map.of("HaxeParser", List.of("haxeparser", "hxparse"))));

    assertEquals(List.of("haxeparser", "hxparse"), graph.closureOf("app", "haxeparser"));
    assertEquals(List.of(), graph.closureOf("app", "utest"));
    assertEquals(List.of(), graph.closureOf("tests", "haxeparser"));
  }
}
