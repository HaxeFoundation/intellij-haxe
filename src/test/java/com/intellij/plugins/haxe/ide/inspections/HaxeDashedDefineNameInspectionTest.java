package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.buildfiles.HaxeDashedDefineNameInspection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Inspection: dashed define name")
public class HaxeDashedDefineNameInspectionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    // every fixture is inline
    return "";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new HaxeDashedDefineNameInspection());
  }

  /** (file name, source with every expected warning marked). */
  static final List<Arguments> MARKED_SOURCES = List.of(
    // flags hxml define names with a dash
    arguments("build.hxml",
              """
              -D <warning descr="%s">my-flag</warning>
              -D ok_flag
              -D <warning descr="%s">a-b</warning>=1
              --define <warning descr="%s">other-flag</warning>
              -lib some-lib
              -cp my-src
              """.formatted(warning("my-flag", "my_flag"), warning("a-b", "a_b"), warning("other-flag", "other_flag"))),
    // flags project xml define names with a dash
    arguments("project.xml",
              """
              <project>
                <haxelib name="openfl"/>
                <haxedef name="<warning descr="%s">my-flag</warning>"/>
                <define name="ok_flag"/>
                <define name="<warning descr="%s">a-b</warning>" value="1"/>
              </project>
              """.formatted(warning("my-flag", "my_flag"), warning("a-b", "a_b"))),
    // flags nmml define names with a dash (the NME classification of the project file)
    arguments("build.nmml",
              """
              <project>
                <haxelib name="nme"/>
                <haxedef name="<warning descr="%s">my-flag</warning>"/>
              </project>
              """.formatted(warning("my-flag", "my_flag"))),
    // ignores xml that is not a project file
    arguments("settings.xml",
              """
              <config>
                <define name="my-flag"/>
              </config>
              """));

  @ParameterizedTest(name = "{0}")
  @FieldSource("MARKED_SOURCES")
  @DisplayName("flags dashed define names in build files only")
  public void testFlagsDashedDefineNamesInBuildFilesOnly(String fileName, String markedSource) {
    myFixture.configureByText(fileName, markedSource);

    myFixture.checkHighlighting();
  }

  /** (file name, source with the caret on a dashed define, fix text, result). */
  static final List<Arguments> FIXES = List.of(
    // replaces the hxml define name and keeps the value
    arguments("build.hxml", "-D a-<caret>b=1\n-D my-flag\n", "Replace with 'a_b'", "-D a_b=1\n-D my-flag\n"),
    // replaces the project xml haxedef name; a haxedef is no condition define, so an if= token stays
    arguments("project.xml",
              """
              <project>
                <haxelib name="openfl"/>
                <haxedef name="my-<caret>flag"/>
                <section if="my-flag"/>
              </project>
              """,
              "Replace with 'my_flag'",
              """
              <project>
                <haxelib name="openfl"/>
                <haxedef name="my_flag"/>
                <section if="my-flag"/>
              </project>
              """),
    // a <define> is matched by name in conditions and substitutions: those follow, a path that happens to match does not
    arguments("project.xml",
              """
              <project>
                <haxelib name="openfl"/>
                <define name="my-<caret>flag"/>
                <section if="my-flag debug">
                  <haxeflag name="--no-traces" unless="other || my-flag"/>
                  <set name="label" value="${my-flag}"/>
                  <set name="legacy" value="${my-flag == 1}"/>
                  <source path="my-flag"/>
                  <section if="my-flag-extra"/>
                </section>
              </project>
              """,
              "Replace with 'my_flag'",
              """
              <project>
                <haxelib name="openfl"/>
                <define name="my_flag"/>
                <section if="my_flag debug">
                  <haxeflag name="--no-traces" unless="other || my_flag"/>
                  <set name="label" value="${my_flag}"/>
                  <set name="legacy" value="${my_flag == 1}"/>
                  <source path="my-flag"/>
                  <section if="my-flag-extra"/>
                </section>
              </project>
              """));

  @ParameterizedTest(name = "{0}")
  @FieldSource("FIXES")
  @DisplayName("replaces the define name")
  public void testReplacesTheDefineName(String fileName, String before, String fixText, String after) {
    myFixture.configureByText(fileName, before);

    IntentionAction fix = myFixture.findSingleIntention(fixText);
    myFixture.launchAction(fix);

    myFixture.checkResult(after);
  }

  private static String warning(String name, String compilerName) {
    return "For conditional compilation, '%s' is mapped to '%s'".formatted(name, compilerName);
  }
}
