package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxelibPathParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Build tools: haxelib path parser")
public class HaxelibPathParserTest {

  @Test
  @DisplayName("error output yields no sections")
  public void errorOutputYieldsNoSections() {
    List<HaxelibPathParser.LibrarySection> sections = HaxelibPathParser.parseSections("nosuchlib", List.of(
      "Error: Library nosuchlib is not installed",
      ""));
    assertTrue(sections.isEmpty());
  }

  @Test
  @DisplayName("sections attribute each classpath to its own library")
  public void sectionsAttributeEachClasspathToItsOwnLibrary() {
    // verbatim `haxelib path hexannotation` output shape: requested lib
    // first, transitive dependencies after, each closed by its -D marker
    List<HaxelibPathParser.LibrarySection> sections = HaxelibPathParser.parseSections("hexannotation", List.of(
      "C:\\HaxeToolkit\\haxe\\lib\\hexannotation/0,35,0/src/",
      "-D hexannotation=0.35.0",
      "C:\\HaxeToolkit\\haxe\\lib\\tink_macro/0,16,1/src/",
      "-D tink_macro=0.16.1",
      "C:\\HaxeToolkit\\haxe\\lib\\tink_core/2,1,1/src/",
      "-D tink_core=2.1.1",
      "-L lime"));

    assertEquals(3, sections.size());
    assertEquals("hexannotation", sections.get(0).name());
    assertEquals("0.35.0", sections.get(0).version());
    assertEquals(List.of("C:\\HaxeToolkit\\haxe\\lib\\hexannotation/0,35,0/src/"), sections.get(0).classpaths());
    assertEquals("tink_core", sections.get(2).name());
    assertEquals(List.of("C:\\HaxeToolkit\\haxe\\lib\\tink_core/2,1,1/src/"), sections.get(2).classpaths());
  }

  @Test
  @DisplayName("trailing classpaths without a marker fall to the requested lib")
  public void trailingClasspathsWithoutAMarkerFallToTheRequestedLib() {
    List<HaxelibPathParser.LibrarySection> sections = HaxelibPathParser.parseSections("mylib", List.of(
      "C:/HaxeToolkit/haxe/lib/tink_core/2,1,1/src/",
      "-D tink_core=2.1.1",
      "C:/dev/mylib/extra-src/"));

    assertEquals(2, sections.size());
    assertEquals("mylib", sections.get(1).name());
    assertNull(sections.get(1).version());
    assertEquals(List.of("C:/dev/mylib/extra-src/"), sections.get(1).classpaths());
  }
}
