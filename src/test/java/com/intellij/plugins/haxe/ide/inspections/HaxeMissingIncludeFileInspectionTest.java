package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.buildfiles.HaxeMissingIncludeFileInspection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inspection: missing include file")
public class HaxeMissingIncludeFileInspectionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    // every fixture is inline
    return "";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new HaxeMissingIncludeFileInspection());
  }

  @Test
  @DisplayName("flags an include that resolves to nothing")
  public void testFlagsAnIncludeThatResolvesToNothing() {
    myFixture.addFileToProject("defs.xml", "<project/>");
    myFixture.addFileToProject("sub/include.xml", "<project/>");
    myFixture.addFileToProject("empty/readme.txt", "");
    myFixture.configureByText("project.xml",
                              """
                              <project>
                                <haxelib name="openfl"/>
                                <include path="defs.xml"/>
                                <include path="sub"/>
                                <include name="defs.xml"/>
                                <include path="<warning descr="Include file not found: missing.xml">missing.xml</warning>"/>
                                <include path="<warning descr="Include file not found: empty">empty</warning>"/>
                                <include path="${haxelib:lime}/include.xml"/>
                                <include haxelib="openfl"/>
                              </project>
                              """);

    myFixture.checkHighlighting();
  }

  @Test
  @DisplayName("ignores xml that is not a project file")
  public void testIgnoresXmlThatIsNotAProjectFile() {
    myFixture.configureByText("settings.xml", "<config><include path=\"missing.xml\"/></config>");

    myFixture.checkHighlighting();
  }
}
