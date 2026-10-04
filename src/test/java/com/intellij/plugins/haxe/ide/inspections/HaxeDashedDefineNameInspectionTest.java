package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.buildfiles.HaxeDashedDefineNameInspection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

  @Test
  @DisplayName("flags hxml define names with a dash")
  public void testFlagsHxmlDefineNamesWithADash() {
    myFixture.configureByText("build.hxml", """
      -D <warning descr="%s">my-flag</warning>
      -D ok_flag
      -D <warning descr="%s">a-b</warning>=1
      --define <warning descr="%s">other-flag</warning>
      -lib some-lib
      -cp my-src
      """.formatted(warning("my-flag", "my_flag"), warning("a-b", "a_b"), warning("other-flag", "other_flag")));

    myFixture.checkHighlighting();
  }

  @Test
  @DisplayName("replaces the hxml define name and keeps the value")
  public void testReplacesTheHxmlDefineNameAndKeepsTheValue() {
    myFixture.configureByText("build.hxml", "-D a-<caret>b=1\n-D my-flag\n");

    IntentionAction fix = myFixture.findSingleIntention("Replace with 'a_b'");
    myFixture.launchAction(fix);

    myFixture.checkResult("-D a_b=1\n-D my-flag\n");
  }

  @Test
  @DisplayName("flags project xml define names with a dash")
  public void testFlagsProjectXmlDefineNamesWithADash() {
    myFixture.configureByText("project.xml", """
      <project>
        <haxelib name="openfl"/>
        <haxedef name="<warning descr="%s">my-flag</warning>"/>
        <define name="ok_flag"/>
        <define name="<warning descr="%s">a-b</warning>" value="1"/>
      </project>
      """.formatted(warning("my-flag", "my_flag"), warning("a-b", "a_b")));

    myFixture.checkHighlighting();
  }

  @Test
  @DisplayName("flags nmml define names with a dash")
  public void testFlagsNmmlDefineNamesWithADash() {
    myFixture.configureByText("build.nmml", """
      <project>
        <haxelib name="nme"/>
        <haxedef name="<warning descr="%s">my-flag</warning>"/>
      </project>
      """.formatted(warning("my-flag", "my_flag")));

    myFixture.checkHighlighting();
  }

  @Test
  @DisplayName("replaces the project xml define name")
  public void testReplacesTheProjectXmlDefineName() {
    myFixture.configureByText("project.xml", """
      <project>
        <haxelib name="openfl"/>
        <haxedef name="my-<caret>flag"/>
      </project>
      """);

    IntentionAction fix = myFixture.findSingleIntention("Replace with 'my_flag'");
    myFixture.launchAction(fix);

    myFixture.checkResult("""
      <project>
        <haxelib name="openfl"/>
        <haxedef name="my_flag"/>
      </project>
      """);
  }

  @Test
  @DisplayName("ignores xml that is not a project file")
  public void testIgnoresXmlThatIsNotAProjectFile() {
    myFixture.configureByText("settings.xml", """
      <config>
        <define name="my-flag"/>
      </config>
      """);

    myFixture.checkHighlighting();
  }

  private static String warning(String name, String compilerName) {
    return "For conditional compilation, '%s' is mapped to '%s'".formatted(name, compilerName);
  }
}
