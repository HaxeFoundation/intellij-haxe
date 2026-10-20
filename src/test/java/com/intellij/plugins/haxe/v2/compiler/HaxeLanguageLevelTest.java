package com.intellij.plugins.haxe.v2.compiler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Compiler: language level")
public class HaxeLanguageLevelTest {

  @Test
  @DisplayName("exact versions resolve")
  public void exactVersionsResolve() {
    assertEquals(HaxeLanguageLevel.HAXE_3_4, HaxeLanguageLevel.fromVersionString("3.4"));
    assertEquals(HaxeLanguageLevel.HAXE_4_0, HaxeLanguageLevel.fromVersionString("4.0"));
    assertEquals(HaxeLanguageLevel.HAXE_4_3, HaxeLanguageLevel.fromVersionString("4.3"));
    assertEquals(HaxeLanguageLevel.HAXE_5_0, HaxeLanguageLevel.fromVersionString("5.0"));
  }

  @Test
  @DisplayName("patch and pre release versions resolve")
  public void patchAndPreReleaseVersionsResolve() {
    assertEquals(HaxeLanguageLevel.HAXE_4_3, HaxeLanguageLevel.fromVersionString("4.3.7"));
    assertEquals(HaxeLanguageLevel.HAXE_5_0, HaxeLanguageLevel.fromVersionString("5.0.0-rc.1"));
    assertEquals(HaxeLanguageLevel.HAXE_4_0, HaxeLanguageLevel.fromVersionString(" 4.0.5 "));
  }

  @Test
  @DisplayName("unknown versions clamp to closest lower level")
  public void unknownVersionsClampToClosestLowerLevel() {
    assertEquals(HaxeLanguageLevel.HAXE_4_3, HaxeLanguageLevel.fromVersionString("4.4"));
    assertEquals(HaxeLanguageLevel.HAXE_3_4, HaxeLanguageLevel.fromVersionString("3.9"));
    assertEquals(HaxeLanguageLevel.HAXE_5_0, HaxeLanguageLevel.fromVersionString("6.0"));
  }

  @Test
  @DisplayName("too old or invalid versions return null")
  public void tooOldOrInvalidVersionsReturnNull() {
    assertNull(HaxeLanguageLevel.fromVersionString("3.2"));
    assertNull(HaxeLanguageLevel.fromVersionString("2.10"));
    assertNull(HaxeLanguageLevel.fromVersionString("not-a-version"));
    assertNull(HaxeLanguageLevel.fromVersionString(""));
    assertNull(HaxeLanguageLevel.fromVersionString(null));
  }

  @Test
  @DisplayName("latest is highest level")
  public void latestIsHighestLevel() {
    assertEquals(HaxeLanguageLevel.HAXE_5_0, HaxeLanguageLevel.latest());
  }

  @Test
  @DisplayName("is at least compares by version order")
  public void isAtLeastComparesByVersionOrder() {
    assertTrue(HaxeLanguageLevel.HAXE_4_3.isAtLeast(HaxeLanguageLevel.HAXE_4_0));
    assertTrue(HaxeLanguageLevel.HAXE_4_3.isAtLeast(HaxeLanguageLevel.HAXE_4_3));
    assertFalse(HaxeLanguageLevel.HAXE_4_0.isAtLeast(HaxeLanguageLevel.HAXE_4_3));
  }

  @Test
  @DisplayName("version string round trips")
  public void versionStringRoundTrips() {
    for (HaxeLanguageLevel level : HaxeLanguageLevel.values()) {
      assertEquals(level, HaxeLanguageLevel.fromVersionString(level.getVersionString()));
    }
  }
}
