package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
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

  @Test
  @DisplayName("notices and network paths are not classpaths")
  public void noticesAndNetworkPathsAreNotClasspaths() {
    // a newer haxelib than the one a haxe preview bundles warns before the classpaths
    List<HaxelibPathParser.LibrarySection> sections = HaxelibPathParser.parseSections("lime", List.of(
      "Warning: Repository requires reformatting. To reformat, run `haxelib fixrepo`.",
      "\\\\server\\share\\lime/8,1,2/src/",
      "C:/HaxeToolkit/haxe/lib/lime/8,1,2/src/",
      "-D lime=8.1.2"));

    assertEquals(1, sections.size());
    assertEquals(List.of("C:/HaxeToolkit/haxe/lib/lime/8,1,2/src/"), sections.getFirst().classpaths());
  }

  @Test
  @DisplayName("extra params defines belong to the library that follows")
  public void extraParamsDefinesBelongToTheLibraryThatFollows() {
    // haxelib prints a library's extraParams.hxml lines before its classpath and its marker
    List<HaxelibPathParser.LibrarySection> sections = HaxelibPathParser.parseSections("haxeui-core", List.of(
      "-D haxeui",
      "C:/HaxeToolkit/haxe/lib/haxeui-core/1,7,0/",
      "-D haxeui-core=1.7.0",
      "--macro allowPackage('sys')",
      // a macro sets its define at compile time; only the compiler knows it
      "--macro define('nodejs')",
      "-D flavour=pure",
      "C:/HaxeToolkit/haxe/lib/hxnodejs/12,2,0/src/",
      "-D hxnodejs=12.2.0"));

    List<String> names = sections.stream().map(HaxelibPathParser.LibrarySection::name).toList();
    assertEquals(List.of("haxeui-core", "hxnodejs"), names, "a valued extraParams define is no library");
    assertEquals(List.of(new HaxeDefine("haxeui", null)), sections.get(0).extraDefines());
    assertEquals(List.of(new HaxeDefine("flavour", "pure")), sections.get(1).extraDefines());
  }
}
