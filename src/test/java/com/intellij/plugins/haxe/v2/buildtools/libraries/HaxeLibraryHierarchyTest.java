package com.intellij.plugins.haxe.v2.buildtools.libraries;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Library;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Requirement;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryHierarchy.Edge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The walk from a build file's declarations: every library once, a loop closed where it repeats, a missing requirement kept as a node. */
@DisplayName("Build tools: library hierarchy")
public class HaxeLibraryHierarchyTest {

  @Test
  @DisplayName("a dependency loop yields each library once and every edge")
  public void testADependencyLoopYieldsEachLibraryOnceAndEveryEdge() {
    HaxeLibraryGraph graph = new HaxeLibraryGraph();
    graph.replaceAll(Map.of(
      "a", new Library("a", "1.0.0", false, List.of(new Requirement("b", "2.0.0")), List.of(), null),
      "b", new Library("b", "2.0.0", false, List.of(new Requirement("A", null)), List.of(), null)), Map.of());

    HaxeLibraryHierarchy hierarchy = HaxeLibraryHierarchy.of(graph, List.of(new HaxeLibDependency("a", null)));

    assertEquals(List.of("a", "b"), hierarchy.nodes());
    assertEquals(List.of(new Edge("a", "b", "2.0.0"), new Edge("b", "A", null)), hierarchy.edges());
  }

  @Test
  @DisplayName("a requirement no sync resolved is a node without children")
  public void testARequirementNoSyncResolvedIsANodeWithoutChildren() {
    HaxeLibraryGraph graph = new HaxeLibraryGraph();
    graph.replaceAll(Map.of("app", new Library("app", "0.1.0", false, List.of(new Requirement("missing", null)), List.of(), null)), Map.of());

    HaxeLibraryHierarchy hierarchy = HaxeLibraryHierarchy.of(graph, List.of(new HaxeLibDependency("app", null), new HaxeLibDependency("unknown", "3.0.0")));

    assertEquals(List.of("app", "unknown", "missing"), hierarchy.nodes());
    assertEquals(List.of(new Edge("app", "missing", null)), hierarchy.edges());
  }
}
