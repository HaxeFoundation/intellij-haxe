package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Navigation: show post-macro file action")
public class HaxeShowPostMacroFileActionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a build macro on the class is reached")
  public void testABuildMacroOnTheClassIsReached() {
    HaxeFile file = (HaxeFile)myFixture.configureByText("A.hx", "@:build(M.build()) class A {}");

    assertTrue(HaxeShowPostMacroFileAction.reachesBuildMacro(file));
  }

  @Test
  @DisplayName("an auto build macro on an implemented interface is reached")
  public void testAnAutoBuildMacroOnAnImplementedInterfaceIsReached() {
    HaxeFile file = (HaxeFile)myFixture.configureByText("C.hx", "@:autoBuild(M.build()) interface I {}\nclass C implements I {}");

    assertTrue(HaxeShowPostMacroFileAction.reachesBuildMacro(file));
  }

  @Test
  @DisplayName("a plain class is not reached")
  public void testAPlainClassIsNotReached() {
    HaxeFile file = (HaxeFile)myFixture.configureByText("B.hx", "class B { static function main() {} }");

    assertFalse(HaxeShowPostMacroFileAction.reachesBuildMacro(file));
  }
}
