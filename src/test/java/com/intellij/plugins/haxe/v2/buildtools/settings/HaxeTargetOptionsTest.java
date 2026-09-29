package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.TargetDefinition;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tool window: target options")
public class HaxeTargetOptionsTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/toolwindow/";
  }

  @AfterEach
  void resetConfiguredTargets() {
    for (Framework framework : Framework.values()) {
      HaxeFrameworkTargetSettings.getInstance().setTargets(framework, List.of());
    }
  }

  @Test
  @DisplayName("hxml target is not selectable")
  public void testHxmlTargetIsNotSelectable() {
    assertFalse(HaxeTargetOptions.isTargetSelectable(HaxeBuildFileType.HXML));
    assertTrue(HaxeTargetOptions.isTargetSelectable(HaxeBuildFileType.OPENFL));
    assertTrue(HaxeTargetOptions.isTargetSelectable(HaxeBuildFileType.LIME));
    assertTrue(HaxeTargetOptions.isTargetSelectable(HaxeBuildFileType.NMML));
    assertTrue(HaxeTargetOptions.isTargetSelectable(HaxeBuildFileType.HXP_PROJECT));
  }

  @Test
  @DisplayName("default target is html5 for lime family and host desktop for nme")
  public void testDefaultTargetIsHtml5ForLimeFamilyAndHostDesktopForNme() {
    assertEquals("HTML5", HaxeTargetOptions.defaultChoice(HaxeBuildFileType.OPENFL).id());
    assertEquals("HTML5", HaxeTargetOptions.defaultChoice(HaxeBuildFileType.LIME).id());
    // nme's html5 needs an Emscripten runtime stock installs lack; the tool's own default is cpp
    assertEquals("Desktop (C++)", HaxeTargetOptions.defaultChoice(HaxeBuildFileType.NMML).id());
  }

  @Test
  @DisplayName("display name falls back to default for stale ids")
  public void testDisplayNameFallsBackToDefaultForStaleIds() {
    String fallback = HaxeTargetOptions.displayNameFor(HaxeBuildFileType.OPENFL, "NO_SUCH_TARGET");
    assertEquals(HaxeTargetOptions.defaultChoice(HaxeBuildFileType.OPENFL).displayName(), fallback);
    assertEquals(fallback, HaxeTargetOptions.displayNameFor(HaxeBuildFileType.OPENFL, null));
  }

  @Test
  @DisplayName("each build system offers its own configured list")
  public void testEachBuildSystemOffersItsOwnConfiguredList() {
    boolean nmeHasHashLink = HaxeTargetOptions.choicesFor(HaxeBuildFileType.NMML).stream()
      .anyMatch(choice -> choice.id().equals("HashLink"));
    boolean limeHasHashLink = HaxeTargetOptions.choicesFor(HaxeBuildFileType.LIME).stream()
      .anyMatch(choice -> choice.id().equals("HashLink"));
    assertFalse(nmeHasHashLink);
    assertTrue(limeHasHashLink);

    boolean nmeHasHostDesktop = HaxeTargetOptions.choicesFor(HaxeBuildFileType.NMML).stream()
      .anyMatch(choice -> choice.id().equals("Desktop (C++)"));
    boolean openflHasHostDesktop = HaxeTargetOptions.choicesFor(HaxeBuildFileType.OPENFL).stream()
      .anyMatch(choice -> choice.id().equals("Desktop (C++)"));
    assertTrue(nmeHasHostDesktop);
    assertFalse(openflHasHostDesktop);
  }

  @Test
  @DisplayName("configured targets replace the defaults and drive flags")
  public void testConfiguredTargetsReplaceTheDefaultsAndDriveFlags() {
    TargetDefinition console = new TargetDefinition("Playstation 4", null, List.of("ps4", "-64"));
    HaxeFrameworkTargetSettings.getInstance().setTargets(Framework.LIME, List.of(console));

    List<HaxeTargetOptions.TargetChoice> choices = HaxeTargetOptions.choicesFor(HaxeBuildFileType.LIME);
    assertEquals(1, choices.size());
    assertEquals("Playstation 4", choices.get(0).id());
    assertEquals("ps4", HaxeTargetOptions.targetFlagFor(HaxeBuildFileType.LIME, "Playstation 4"));
    assertEquals(List.of("ps4", "-64"), HaxeTargetOptions.targetFlagsFor(HaxeBuildFileType.LIME, "Playstation 4"));

    // the openfl list is independent of the lime customization
    assertEquals("HTML5", HaxeTargetOptions.defaultChoice(HaxeBuildFileType.OPENFL).id());
  }

  @Test
  @DisplayName("removing every configured row restores the defaults")
  public void testRemovingEveryConfiguredRowRestoresTheDefaults() {
    TargetDefinition console = new TargetDefinition("Nintendo Wii", null, List.of("wii"));
    HaxeFrameworkTargetSettings.getInstance().setTargets(Framework.NME, List.of(console));
    assertEquals("Nintendo Wii", HaxeTargetOptions.defaultChoice(HaxeBuildFileType.NMML).id());

    HaxeFrameworkTargetSettings.getInstance().setTargets(Framework.NME, List.of());
    assertEquals("Desktop (C++)", HaxeTargetOptions.defaultChoice(HaxeBuildFileType.NMML).id());
  }
}
