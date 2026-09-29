package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeLibDependency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build system: hxml file parser")
public class HxmlFileParserTest {

  @Test
  @DisplayName("parses target defines and libraries")
  public void parsesTargetDefinesAndLibraries() {
    HaxeBuildFileInfo info = HxmlFileParser.parse("""
      # build config
      -cp src
      -main Main
      -lib openfl:9.2.0
      -lib format
      -D analyzer-optimize
      -D myvalue=42
      -js bin/app.js
      """);

    assertEquals(HaxeTarget.JAVA_SCRIPT, info.target());
    assertEquals(List.of(new HaxeDefine("analyzer-optimize", null), new HaxeDefine("myvalue", "42")), info.defines());
    assertEquals(List.of(new HaxeLibDependency("openfl", "9.2.0"), new HaxeLibDependency("format", null)), info.libraries());
    assertEquals(List.of("src"), info.classpaths());
  }

  @Test
  @DisplayName("collects classpaths from every flag spelling")
  public void collectsClasspathsFromEveryFlagSpelling() {
    HaxeBuildFileInfo info = HxmlFileParser.parse("""
      -cp src
      -p C:/HaxeToolkit/haxe/lib/lime/8,1,2/src
      --class-path vendored/format
      -hl bin/app.hl
      """);

    assertEquals(List.of("src", "C:/HaxeToolkit/haxe/lib/lime/8,1,2/src", "vendored/format"), info.classpaths());
  }

  /** (hxml line, the target its flag selects). */
  static final List<Arguments> TARGET_FLAG_SPELLINGS = List.of(
    arguments("--hl out.hl", HaxeTarget.HL),
    arguments("--jvm out.jar", HaxeTarget.JAVA),
    arguments("-cpp out", HaxeTarget.CPP),
    arguments("--interp", HaxeTarget.INTERP),
    arguments("-as3 out", HaxeTarget.FLASH));

  @ParameterizedTest(name = "{0}")
  @FieldSource("TARGET_FLAG_SPELLINGS")
  @DisplayName("recognizes double dash and alias target flags")
  public void recognizesDoubleDashAndAliasTargetFlags(String line, HaxeTarget target) {
    assertEquals(target, HxmlFileParser.parse(line).target());
  }

  @Test
  @DisplayName("first target flag wins")
  public void firstTargetFlagWins() {
    HaxeBuildFileInfo info = HxmlFileParser.parse("""
      -js bin/app.js
      -hl bin/app.hl
      """);
    assertEquals(HaxeTarget.JAVA_SCRIPT, info.target());
  }

  @Test
  @DisplayName("custom target surfaces as the defines the compiler sets")
  public void customTargetSurfacesAsTheDefinesTheCompilerSets() {
    HaxeBuildFileInfo bare = HxmlFileParser.parse("--custom-target go");
    assertNull(bare.target());
    assertEquals(List.of(new HaxeDefine("custom_target", null), new HaxeDefine("target.name", "go")), bare.defines());

    // "name=path" carries the generator path, which the defines do not; first declaration wins
    HaxeBuildFileInfo withPath = HxmlFileParser.parse("""
      --custom-target go=gen/go-target
      --custom-target rust
      """);
    assertEquals(List.of(new HaxeDefine("custom_target", null), new HaxeDefine("target.name", "go")), withPath.defines());
  }

  @Test
  @DisplayName("flatten expands a reference trailing the next separator")
  public void flattenExpandsAReferenceTrailingTheNextSeparator() {
    // "--next other.hxml" inside a file: the rest of the line is the next
    // content's first argument, so the reference still expands
    HxmlFileParser.IncludeReader reader = path ->
      path.equals("js.hxml") ? "-js bin/app.js" : null;
    String flat = HxmlFileParser.flatten("-neko bin/app.n\n--next js.hxml", reader);

    assertTrue(flat.contains("# include js.hxml"), "the expansion keeps its marker: " + flat);
    assertTrue(flat.contains("-js bin/app.js"), "the referenced content must expand: " + flat);
  }

  @Test
  @DisplayName("leading included file is the first marker before real content")
  public void leadingIncludedFileIsTheFirstMarkerBeforeRealContent() {
    assertEquals("js.hxml", HxmlFileParser.leadingIncludedFile("# include js.hxml\n-js bin/app.js"));
    // inline content before any marker means the section is the root's own
    assertNull(HxmlFileParser.leadingIncludedFile("-cp src\n# include late.hxml\n-lib utest"));
    // of stacked markers the FIRST (the chain's own entry) wins - the second
    // is a nested include, that entry's implementation detail
    assertEquals("outer.hxml",
                 HxmlFileParser.leadingIncludedFile("# include outer.hxml\n# include inner.hxml\n-js bin/app.js"));
  }

  @Test
  @DisplayName("section ids name sections by file with occurrence suffixes")
  public void sectionIdsNameSectionsByFileWithOccurrenceSuffixes() {
    List<String> sections = List.of(
      "-js bin/app.js",
      "# include cs.hxml\n-cs bin/cs",
      "# include cs.hxml\n-D unsafe\n-cs bin/cs_unsafe");

    List<String> ids = HxmlFileParser.sectionIds("build.hxml", sections);
    assertEquals(List.of("build.hxml", "cs.hxml", "cs.hxml#2"), ids);
  }

  @Test
  @DisplayName("section descriptors name each section by target")
  public void sectionDescriptorsNameEachSectionByTarget() {
    List<String> sections = List.of(
      "-python bin/app.py",
      "-php bin/php",
      "--jvm bin/app.jar");

    assertEquals(List.of("Python", "PHP", "Java"), HxmlFileParser.sectionDescriptors(sections));
  }

  @Test
  @DisplayName("section descriptors extend colliding targets with their outputs")
  public void sectionDescriptorsExtendCollidingTargetsWithTheirOutputs() {
    List<String> sections = List.of(
      "-js bin/app.js",
      "-js bin/worker.js",
      "-python bin/app.py");

    assertEquals(List.of("JavaScript · bin/app.js", "JavaScript · bin/worker.js", "Python"),
                 HxmlFileParser.sectionDescriptors(sections));
  }

  @Test
  @DisplayName("section descriptors fall back to the main class when outputs do not differ")
  public void sectionDescriptorsFallBackToTheMainClassWhenOutputsDoNotDiffer() {
    // same target, same output: appending the shared output would separate
    // nothing, so the main classes take over
    List<String> sections = List.of(
      "-main app.Client\n-js bin/app.js",
      "-main app.Worker\n-js bin/app.js");

    assertEquals(List.of("JavaScript · Client", "JavaScript · Worker"),
                 HxmlFileParser.sectionDescriptors(sections));
  }

  @Test
  @DisplayName("target less sections use the main class or stay blank")
  public void targetLessSectionsUseTheMainClassOrStayBlank() {
    List<String> sections = List.of(
      "-main tools.Generate",
      "-cp src\n-D setup");

    assertEquals(List.of("Generate", ""), HxmlFileParser.sectionDescriptors(sections));
  }

  @Test
  @DisplayName("flatten merges hxml includes")
  public void flattenMergesHxmlIncludes() {
    HxmlFileParser.IncludeReader reader = path ->
      path.equals("common.hxml") ? "-lib heaps\n-D common-define" : null;

    HaxeBuildFileInfo info = HxmlFileParser.parse(HxmlFileParser.flatten("""
      common.hxml
      -hl bin/app.hl
      """, reader));

    assertEquals(HaxeTarget.HL, info.target());
    assertEquals(List.of(new HaxeDefine("common-define", null)), info.defines());
    assertEquals(List.of(new HaxeLibDependency("heaps", null)), info.libraries());
  }

  @Test
  @DisplayName("flatten resolves nested includes against the root")
  public void flattenResolvesNestedIncludesAgainstTheRoot() {
    // haxe resolves every hxml reference against the working directory, not
    // the declaring file - a nested include reads next to the root
    HxmlFileParser.IncludeReader rootReader = path -> switch (path) {
      case "sub/inner.hxml" -> "leaf.hxml";
      case "leaf.hxml" -> "-D from-root-leaf";
      default -> null;
    };

    HaxeBuildFileInfo info = HxmlFileParser.parse(HxmlFileParser.flatten("sub/inner.hxml", rootReader));
    assertEquals(List.of(new HaxeDefine("from-root-leaf", null)), info.defines());
  }

  @Test
  @DisplayName("flatten drops include cycles")
  public void flattenDropsIncludeCycles() {
    HxmlFileParser.IncludeReader reader = path -> "self.hxml\n-lib once";
    HaxeBuildFileInfo info = HxmlFileParser.parse(HxmlFileParser.flatten("self.hxml", reader));
    assertEquals(List.of(new HaxeLibDependency("once", null)), info.libraries());
  }

  @Test
  @DisplayName("no target yields null")
  public void noTargetYieldsNull() {
    HaxeBuildFileInfo info = HxmlFileParser.parse("-cp src\n-main Main");
    assertNull(info.target());
    assertTrue(info.defines().isEmpty());
    assertTrue(info.libraries().isEmpty());
  }

  @Test
  @DisplayName("git library versions keep the full suffix")
  public void gitLibraryVersionsKeepTheFullSuffix() {
    HaxeBuildFileInfo info = HxmlFileParser.parse("-lib mylib:git:https://example.com/repo.git");
    assertEquals(List.of(new HaxeLibDependency("mylib", "git:https://example.com/repo.git")), info.libraries());
  }

  @Test
  @DisplayName("git library versions keep the ref suffix")
  public void gitLibraryVersionsKeepTheRefSuffix() {
    // '#' only starts a comment at line START - a mid-line ref must survive
    HaxeBuildFileInfo info = HxmlFileParser.parse("-lib mylib:git:https://example.com/repo.git#v1.2.0");
    assertEquals(List.of(new HaxeLibDependency("mylib", "git:https://example.com/repo.git#v1.2.0")), info.libraries());
  }
}
