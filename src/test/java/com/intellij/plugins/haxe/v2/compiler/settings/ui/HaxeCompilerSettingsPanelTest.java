package com.intellij.plugins.haxe.v2.compiler.settings.ui;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import com.intellij.plugins.haxe.v2.compiler.settings.ui.HaxeCompilerSettingsPanel.ModuleSdk;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The form binds every control, the per-feature toggles follow the master switch, and module rows follow their SDK. */
@DisplayName("Compiler settings: panel")
public class HaxeCompilerSettingsPanelTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("feature toggles round trip and follow the master switch")
  public void testFeatureTogglesRoundTripAndFollowTheMasterSwitch() {
    HaxeCompilerSettingsPanel panel = new HaxeCompilerSettingsPanel();
    assertNotNull(panel.getComponent());

    panel.setCompilerIdeFeaturesEnabled(true);
    panel.setCompilerInlaysEnabled(false);
    panel.setCompilerFindUsagesEnabled(true);
    panel.setCompilerNavigationEnabled(false);
    panel.setCompilerCompletionEnabled(true);
    panel.setCompletionMode(HaxeCompletionMode.COMPILER_ONLY);

    assertTrue(panel.isCompilerIdeFeaturesEnabled());
    assertFalse(panel.isCompilerInlaysEnabled());
    assertTrue(panel.isCompilerFindUsagesEnabled());
    assertFalse(panel.isCompilerNavigationEnabled());
    assertTrue(panel.isCompilerCompletionEnabled());
    assertEquals(HaxeCompletionMode.COMPILER_ONLY, panel.getCompletionMode());
    assertTrue(panel.isCompletionModeChoiceEnabled());

    panel.setCompilerCompletionEnabled(false);
    assertFalse(panel.isCompletionModeChoiceEnabled(), "the completion source is moot while completion is no compiler feature");

    panel.setCompilerIdeFeaturesEnabled(false);
    assertFalse(panel.isCompletionModeChoiceEnabled());
    assertTrue(panel.isCompilerFindUsagesEnabled(), "a child keeps its value while disabled");
  }

  @Test
  @DisplayName("generated member preview follows navigation")
  public void testGeneratedMemberPreviewFollowsNavigation() {
    HaxeCompilerSettingsPanel panel = new HaxeCompilerSettingsPanel();
    panel.setCompilerIdeFeaturesEnabled(true);
    panel.setCompilerNavigationEnabled(true);
    panel.setGeneratedMemberPreviewEnabled(false);
    assertFalse(panel.isGeneratedMemberPreviewEnabled());
    assertTrue(panel.isGeneratedMemberPreviewChoiceEnabled());

    panel.setCompilerNavigationEnabled(false);
    assertFalse(panel.isGeneratedMemberPreviewChoiceEnabled(), "the preview is moot while navigation is no compiler feature");

    panel.setCompilerNavigationEnabled(true);
    panel.setCompilerIdeFeaturesEnabled(false);
    assertFalse(panel.isGeneratedMemberPreviewChoiceEnabled());
  }

  @Test
  @DisplayName("module rows show their own sdk level")
  public void testModuleRowsShowTheirOwnSdkLevel() {
    HaxeCompilerSettingsPanel panel = new HaxeCompilerSettingsPanel();
    String legacyLevel = HaxeLanguageLevel.HAXE_3_4.getPresentableText();
    String currentLevel = HaxeLanguageLevel.HAXE_4_3.getPresentableText();
    List<ModuleSdk> modules = List.of(new ModuleSdk("legacy", HaxeLanguageLevel.HAXE_3_4),
                                      new ModuleSdk("current", HaxeLanguageLevel.HAXE_4_3));

    panel.reset(null, Map.of(), modules);
    assertEquals(HaxeBundle.message("haxe.compiler.sdk.level", legacyLevel), panel.moduleLevelText("legacy"));
    assertEquals(HaxeBundle.message("haxe.compiler.sdk.level", currentLevel), panel.moduleLevelText("current"));

    panel.reset(HaxeLanguageLevel.HAXE_4_3, Map.of("legacy", HaxeLanguageLevel.HAXE_3_4), modules);
    assertEquals(legacyLevel, panel.moduleLevelText("legacy"));
    assertEquals(HaxeBundle.message("haxe.compiler.project.default.level", currentLevel), panel.moduleLevelText("current"));
  }
}
