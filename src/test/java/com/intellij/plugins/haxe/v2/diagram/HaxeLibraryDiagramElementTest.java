package com.intellij.plugins.haxe.v2.diagram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The persisted identity of a diagram root round-trips, and garbage resolves to nothing. */
@DisplayName("Library diagram: element identity")
public class HaxeLibraryDiagramElementTest {

  @Test
  @DisplayName("qualified names round trip for build files and libraries")
  public void testQualifiedNamesRoundTripForBuildFilesAndLibraries() {
    HaxeLibraryDiagramElement buildFile = HaxeLibraryDiagramElement.buildFile("C:/work/compile.hxml");
    HaxeLibraryDiagramElement library = HaxeLibraryDiagramElement.library("hxparse");

    assertEquals(buildFile, HaxeLibraryDiagramElement.fromQualifiedName(buildFile.qualifiedName()));
    assertEquals(library, HaxeLibraryDiagramElement.fromQualifiedName(library.qualifiedName()));
    assertNull(HaxeLibraryDiagramElement.fromQualifiedName("something else"));
  }
}
