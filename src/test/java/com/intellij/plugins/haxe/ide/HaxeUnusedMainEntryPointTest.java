package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedMethodInspection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The static main of a class a build starts from is the program's entry point, not an unused method. */
@DisplayName("Annotation: unused main entry point")
public class HaxeUnusedMainEntryPointTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/annotation.unused/";
  }

  @Test
  @DisplayName("only the entry class main is used")
  public void testOnlyTheEntryClassMainIsUsed() {
    // the build looks its main class up on disk, so this fixture keeps real files
    myFixture.addFileToProject("build.hxml", "-cp .\n-main UnusedMainEntryPointTest\n--interp\n");
    myFixture.configureByFiles("UnusedMainEntryPointTest.hx");
    myFixture.enableInspections(HaxeUnusedMethodInspection.class);

    myFixture.checkHighlighting(true, false, true);
  }
}
