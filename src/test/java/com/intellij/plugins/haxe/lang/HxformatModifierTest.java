package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatConfigs;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatSettingsModifier;
import com.intellij.psi.PsiFile;
import com.intellij.application.options.CodeStyle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.modifier.CodeStyleStatusBarUIContributor;
import com.intellij.psi.codeStyle.modifier.TransientCodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A project's own hxformat.json overrides the scheme's Haxe settings per
 * file, EditorConfig-style: nearest config wins, the toggle opts out, and
 * disableFormatting falls back to the scheme.
 */
@DisplayName("Formatting: hxformat modifier")
public class HxformatModifierTest extends HaxeLightFixtureTestCase {

  private static final String MAIN_HX_SOURCE = "class Main {}";

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("nearest config overrides the scheme")
  public void testNearestConfigOverridesTheScheme() {
    myFixture.addFileToProject("hxformat.json", """
      { "wrapping": { "maxLineLength": 101 }, "indentation": { "character": "  ", "tabWidth": 2 } }""");
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);

    TransientCodeStyleSettings settings = transientFor(file);
    assertTrue(new HxformatSettingsModifier().modifySettings(settings, file), "the config must apply");

    assertEquals(101, settings.getRightMargin(HaxeLanguage.INSTANCE));
    assertFalse(settings.getIndentOptions(file.getFileType()).USE_TAB_CHARACTER);
    assertEquals(2, settings.getIndentOptions(file.getFileType()).INDENT_SIZE);
  }

  @Test
  @DisplayName("override config applies when the upward search finds nothing")
  public void testOverrideConfigAppliesWhenTheUpwardSearchFindsNothing() {
    PsiFile override = myFixture.addFileToProject("configs/hxformat.json", """
      { "wrapping": { "maxLineLength": 91 } }""");
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);
    HxformatConfigs.getInstance(getProject())
      .setOverrideConfigUrl(override.getVirtualFile().getUrl());

    TransientCodeStyleSettings settings = transientFor(file);
    assertTrue(new HxformatSettingsModifier().modifySettings(settings, file), "the override must apply");
    assertEquals(91, settings.getRightMargin(HaxeLanguage.INSTANCE));
  }

  @Test
  @DisplayName("config found by the upward search beats the override")
  public void testConfigFoundByTheUpwardSearchBeatsTheOverride() {
    PsiFile override = myFixture.addFileToProject("configs/hxformat.json", """
      { "wrapping": { "maxLineLength": 91 } }""");
    myFixture.addFileToProject("hxformat.json", """
      { "wrapping": { "maxLineLength": 101 } }""");
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);
    HxformatConfigs.getInstance(getProject())
      .setOverrideConfigUrl(override.getVirtualFile().getUrl());

    TransientCodeStyleSettings settings = transientFor(file);
    assertTrue(new HxformatSettingsModifier().modifySettings(settings, file), "the config must apply");
    assertEquals(101, settings.getRightMargin(HaxeLanguage.INSTANCE));
  }

  @Test
  @DisplayName("toggle off keeps the scheme settings")
  public void testToggleOffKeepsTheSchemeSettings() {
    CodeStyleSettings projectSettings = projectSettingsCopy();
    projectSettings.getCustomSettings(HaxeCodeStyleSettings.class).USE_PROJECT_HXFORMAT = false;
    CodeStyleSettingsManager.getInstance(getProject()).setTemporarySettings(projectSettings);
    myFixture.addFileToProject("hxformat.json", """
      { "wrapping": { "maxLineLength": 101 } }""");
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);

    assertFalse(new HxformatSettingsModifier().modifySettings(transientFor(file), file),
                "the toggle must opt out of the project config");
  }

  @Test
  @DisplayName("disable formatting falls back to the scheme")
  public void testDisableFormattingFallsBackToTheScheme() {
    myFixture.addFileToProject("hxformat.json", """
      { "disableFormatting": true, "wrapping": { "maxLineLength": 101 } }""");
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);

    assertFalse(new HxformatSettingsModifier().modifySettings(transientFor(file), file),
                "disableFormatting must fall back to the scheme settings");
  }

  @Test
  @DisplayName("no config leaves the scheme untouched")
  public void testNoConfigLeavesTheSchemeUntouched() {
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);

    assertFalse(new HxformatSettingsModifier().modifySettings(transientFor(file), file));
  }

  @Test
  @DisplayName("status bar contributor names the governing config")
  public void testStatusBarContributorNamesTheGoverningConfig() {
    myFixture.addFileToProject("hxformat.json", "{}");
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);

    CodeStyleStatusBarUIContributor contributor =
      new HxformatSettingsModifier().getStatusBarUiContributor(transientFor(file));

    assertNotNull(contributor, "a governed file must get a status bar entry");
    assertEquals("hxformat", contributor.getStatusText(file));
    assertTrue(contributor.getTooltip().contains("hxformat.json"), "the tooltip must name the config file");
    assertNotNull(contributor.getActions(file), "the open-config action must be offered");
    assertNotNull(contributor.createDisableAction(getProject()));
  }

  @Test
  @DisplayName("status bar contributor absent without a config")
  public void testStatusBarContributorAbsentWithoutAConfig() {
    PsiFile file = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);

    assertNull(new HxformatSettingsModifier().getStatusBarUiContributor(transientFor(file)));
  }

  private TransientCodeStyleSettings transientFor(PsiFile file) {
    return new TransientCodeStyleSettings(file.getVirtualFile(), getProject(), CodeStyle.getSettings(getProject()));
  }
}
