package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A target's std override declares the same types and members as the generic
 * std declaration, so a navigation popup listing both (the compiler answers
 * both for a std member) needs the override's location to name the target.
 */
@DisplayName("Presentation: std override location")
public class HaxeStdOverridePresentationTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/gotoImplementation/";
  }

  @Test
  @DisplayName("a target's std override names the target in its location")
  public void testATargetsStdOverrideNamesTheTargetInItsLocation() {
    myFixture.configureByFiles("std/js/_std/Rope.hx", "std/StdTypes.hx", "std/Rope.hx");
    HaxeClass override = ((HaxeFile)myFixture.getFile()).getClassList().getFirst();
    HaxeNamedComponent push = override.findHaxeMemberByName("push", null).getFirst();

    assertEquals("Rope, js/_std", push.getPresentation().getLocationString());
    assertEquals("js/_std", override.getPresentation().getLocationString());
  }

  @Test
  @DisplayName("the generic std declaration keeps its plain location")
  public void testTheGenericStdDeclarationKeepsItsPlainLocation() {
    myFixture.configureByFiles("std/Rope.hx", "std/StdTypes.hx");
    HaxeClass generic = ((HaxeFile)myFixture.getFile()).getClassList().getFirst();
    HaxeNamedComponent push = generic.findHaxeMemberByName("push", null).getFirst();

    assertEquals("Rope", push.getPresentation().getLocationString());
    assertEquals("", generic.getPresentation().getLocationString());
  }
}
