package com.intellij.plugins.haxe.v2.compiler.settings.ui;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompletionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The form binds every control, and the per-feature toggles follow the master switch. */
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
}
