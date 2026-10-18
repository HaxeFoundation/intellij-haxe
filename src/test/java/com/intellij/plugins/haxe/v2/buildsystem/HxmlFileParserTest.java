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

  /** (hxml content, whether it selects a target). */
  static final List<Arguments> TARGET_SELECTIONS = List.of(
    arguments("-js out.js", true),
    arguments("--interp", true),
    arguments("--custom-target go=export", true),
    arguments("-cp src\n--run Main", true),
    // a library's extraParams.hxml: classpaths, defines and macros, no target
    arguments("-cp std\n-D hx2go\n--macro include('runtime', true)", false));

  @ParameterizedTest(name = "{0}")
  @FieldSource("TARGET_SELECTIONS")
  @DisplayName("declares any target")
  public void declaresAnyTarget(String content, boolean selectsTarget) {
    assertEquals(selectsTarget, HxmlFileParser.declaresAnyTarget(content));
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

  /** (hxml content, the defines it yields): both debug spellings define debug, in line order among the -D defines. */
  static final List<Arguments> DEBUG_FLAG_DEFINES = List.of(
    arguments("-debug\n-js bin/app.js", List.of(new HaxeDefine("debug", null))),
    arguments("--debug", List.of(new HaxeDefine("debug", null))),
    arguments("-D first\n-debug\n-D last=2", List.of(new HaxeDefine("first", null), new HaxeDefine("debug", null), new HaxeDefine("last", "2"))));

  @ParameterizedTest(name = "{0}")
  @FieldSource("DEBUG_FLAG_DEFINES")
  @DisplayName("debug flag defines debug")
  public void debugFlagDefinesDebug(String content, List<HaxeDefine> defines) {
    assertEquals(defines, HxmlFileParser.parse(content).defines());
  }

  @Test
  @DisplayName("custom target surfaces as the defines the compiler sets")
  public void customTargetSurfacesAsTheDefinesTheCompilerSets() {
    HaxeBuildFileInfo bare = HxmlFileParser.parse("--custom-target go");
    assertNull(bare.target());
    assertEquals(List.of(new HaxeDefine("custom_target", null), new HaxeDefine("target.name", "go")), bare.defines());
    assertEquals("go", bare.customTargetName());
    assertNull(HxmlFileParser.parse("-js out.js").customTargetName(), "a stock target is no custom target");

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

  /** (section contents, their descriptors). */
  static final List<Arguments> SECTION_DESCRIPTORS = List.of(
    // section descriptors name each section by target
    arguments(List.of("-python bin/app.py", "-php bin/php", "--jvm bin/app.jar"), List.of("Python", "PHP", "Java")),
    // section descriptors extend colliding targets with their outputs
    arguments(List.of("-js bin/app.js", "-js bin/worker.js", "-python bin/app.py"),
              List.of("JavaScript · bin/app.js", "JavaScript · bin/worker.js", "Python")),
    // section descriptors fall back to the main class when outputs do not differ:
    // appending the shared output would separate nothing, so the main classes take over
    arguments(List.of("-main app.Client\n-js bin/app.js", "-main app.Worker\n-js bin/app.js"),
              List.of("JavaScript · Client", "JavaScript · Worker")),
    // target less sections use the main class or stay blank
    arguments(List.of("-main tools.Generate", "-cp src\n-D setup"), List.of("Generate", "")));

  @ParameterizedTest(name = "{1}")
  @FieldSource("SECTION_DESCRIPTORS")
  @DisplayName("section descriptors name sections by target output or main class")
  public void sectionDescriptorsNameSectionsByTargetOutputOrMainClass(List<String> sections, List<String> descriptors) {
    assertEquals(descriptors, HxmlFileParser.sectionDescriptors(sections));
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

  /** (-lib argument, the dependency it declares). */
  static final List<Arguments> LIBRARY_VERSIONS = List.of(
    arguments("format", new HaxeLibDependency("format", null)),
    arguments("openfl:9.2.0", new HaxeLibDependency("openfl", "9.2.0")),
    // git library versions keep the full suffix
    arguments("mylib:git:https://example.com/repo.git", new HaxeLibDependency("mylib", "git:https://example.com/repo.git")),
    // git library versions keep the ref suffix: '#' only starts a comment at line START
    arguments("mylib:git:https://example.com/repo.git#v1.2.0",
              new HaxeLibDependency("mylib", "git:https://example.com/repo.git#v1.2.0")));

  @ParameterizedTest(name = "{0}")
  @FieldSource("LIBRARY_VERSIONS")
  @DisplayName("lib arguments keep their version suffix")
  public void libArgumentsKeepTheirVersionSuffix(String libArgument, HaxeLibDependency dependency) {
    assertEquals(List.of(dependency), HxmlFileParser.parse("-lib " + libArgument).libraries());
  }
}
