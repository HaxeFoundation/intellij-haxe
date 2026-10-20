package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph.Library;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Library diagram: extras")
public class HaxeLibraryDiagramExtrasTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("a node's source is its file or its library's manifest")
  public void testANodesSourceIsItsFileOrItsLibrarysManifest() {
    VirtualFile include = myFixture.addFileToProject("lib/pkg/include.xml", "<extension/>").getVirtualFile();
    VirtualFile manifest = myFixture.addFileToProject("lib/pkg/haxelib.json", "{}").getVirtualFile();
    VirtualFile checkout = myFixture.addFileToProject("dev/tool/Tool.hx", "class Tool {}").getVirtualFile().getParent();
    HaxeLibraryGraph graph = HaxeLibraryGraph.getInstance(getProject());
    graph.replaceAll(Map.of("pkg", libraryAt("pkg", include.getParent()), "tool", libraryAt("tool", checkout)), Map.of());

    assertEquals(include, HaxeLibraryDiagramExtras.sourceFileOf(HaxeLibraryDiagramElement.buildFile(include.getPath()), graph));
    assertEquals(manifest, HaxeLibraryDiagramExtras.sourceFileOf(HaxeLibraryDiagramElement.library("pkg"), graph));
    assertEquals(checkout, HaxeLibraryDiagramExtras.sourceFileOf(HaxeLibraryDiagramElement.library("tool"), graph),
                 "without a haxelib.json the library's folder stands in");
    assertNull(HaxeLibraryDiagramExtras.sourceFileOf(HaxeLibraryDiagramElement.library("missing"), graph));

    graph.replaceAll(Map.of(), Map.of());
  }

  private static Library libraryAt(String name, VirtualFile root) {
    return new Library(name, "1.0.0", List.of(), List.of(), root.getPath());
  }
}
