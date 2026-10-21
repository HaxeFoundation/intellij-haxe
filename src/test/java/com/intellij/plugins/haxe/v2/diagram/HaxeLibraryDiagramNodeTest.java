package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Library;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** A refresh keeps the node objects that stand for the same element, so a node must read the sync's graph as it is now. */
@DisplayName("Library diagram: node")
public class HaxeLibraryDiagramNodeTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a library node reads its state from the current graph")
  public void testALibraryNodeReadsItsStateFromTheCurrentGraph() {
    HaxeLibraryGraph graph = HaxeLibraryGraph.getInstance(getProject());
    graph.replaceAll(Map.of(), Map.of());
    HaxeLibraryDiagramNode node = new HaxeLibraryDiagramNode(new HaxeLibraryDiagramProvider(), graph, HaxeLibraryDiagramElement.library("hxparse"));
    assertSame(AllIcons.General.Warning, node.getIcon(), "nothing synced yet");

    graph.replaceAll(Map.of("hxparse", new Library("hxparse", "4.3.0", List.of(), List.of(), null)), Map.of());

    assertSame(AllIcons.Nodes.PpLib, node.getIcon());
    assertEquals("hxparse 4.3.0", node.getTooltip());

    graph.replaceAll(Map.of(), Map.of());
  }
}
