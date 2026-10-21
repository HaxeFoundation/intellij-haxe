package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Compiler: project settings")
public class HaxeCompilerProjectSettingsTest {

  @Test
  @DisplayName("fresh settings defaults")
  public void freshSettingsDefaults() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);

    assertNull(settings.getExplicitDefaultLanguageLevel(), "use compiler level is the default mode");
    assertEquals(HaxeLanguageLevel.latest(), settings.getDefaultLanguageLevel(), "no project, no SDK: the latest level");
    assertTrue(settings.isUseLanguageLevelForConditionals());
    assertEquals(HaxeCompletionMode.IDE_AND_COMPILER, settings.getCompletionMode());
    assertEquals(HaxeDisplayConnectionMode.DEDICATED_PROCESS, settings.getDisplayConnectionMode(HaxeServerGeneration.HAXE_4));
    assertEquals(HaxeDisplayConnectionMode.DEDICATED_PROCESS, settings.getDisplayConnectionMode(HaxeServerGeneration.HAXE_3));
    assertFalse(settings.isCompilerDiagnosticsEnabled());
    assertTrue(settings.isDiagnosticsErrorsEnabled(), "diagnostics default to errors only");
    assertFalse(settings.isDiagnosticsUnusedImportsEnabled());
    assertFalse(settings.isDiagnosticsRemovableCodeEnabled());
    assertFalse(settings.isCompilerDiagnosticsOnly());
  }

  @Test
  @DisplayName("each server generation keeps a connection mode of its own")
  public void eachServerGenerationKeepsAConnectionModeOfItsOwn() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);

    settings.setDisplayConnectionMode(HaxeServerGeneration.HAXE_3, HaxeDisplayConnectionMode.SHARED_SERVER);

    assertEquals(HaxeDisplayConnectionMode.SHARED_SERVER, settings.getDisplayConnectionMode(HaxeServerGeneration.HAXE_3));
    assertEquals(HaxeDisplayConnectionMode.DEDICATED_PROCESS, settings.getDisplayConnectionMode(HaxeServerGeneration.HAXE_4),
                 "setting the haxe 3 mode leaves haxe 4 alone");
  }

  @Test
  @DisplayName("explicit default turns compiler mode off and null restores it")
  public void explicitDefaultTurnsCompilerModeOffAndNullRestoresIt() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setDefaultLanguageLevel(HaxeLanguageLevel.HAXE_4_2);
    assertEquals(HaxeLanguageLevel.HAXE_4_2, settings.getExplicitDefaultLanguageLevel());
    settings.setDefaultLanguageLevel(null);
    assertNull(settings.getExplicitDefaultLanguageLevel());
    // without a project there is no SDK to resolve - the fallback is the latest level
    assertEquals(HaxeLanguageLevel.latest(), settings.getDefaultLanguageLevel());
  }

  @Test
  @DisplayName("compiler features default on and count only under the master switch")
  public void compilerFeaturesDefaultOnAndCountOnlyUnderTheMasterSwitch() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    assertTrue(settings.isCompilerInlaysEnabled());
    assertTrue(settings.isCompilerFindUsagesEnabled());
    assertTrue(settings.isCompilerNavigationEnabled());
    assertTrue(settings.isCompilerCompletionEnabled());
    assertFalse(settings.usesCompilerInlays(), "the master switch is off by default");
    assertEquals(HaxeCompletionMode.IDE_ONLY, settings.getEffectiveCompletionMode());

    settings.setCompilerIdeFeaturesEnabled(true);
    settings.setCompilerNavigationEnabled(false);

    assertTrue(settings.usesCompilerInlays());
    assertTrue(settings.usesCompilerFindUsages());
    assertFalse(settings.usesCompilerNavigation());
    assertEquals(HaxeCompletionMode.IDE_AND_COMPILER, settings.getEffectiveCompletionMode());
    settings.setCompilerCompletionEnabled(false);
    assertEquals(HaxeCompletionMode.IDE_ONLY, settings.getEffectiveCompletionMode(), "the stored mode stays, the effective one is IDE only");
    assertEquals(HaxeCompletionMode.IDE_AND_COMPILER, settings.getCompletionMode());
  }

  @Test
  @DisplayName("generated member preview defaults on and counts only under compiler navigation")
  public void generatedMemberPreviewDefaultsOnAndCountsOnlyUnderCompilerNavigation() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    assertTrue(settings.isGeneratedMemberPreviewEnabled());
    assertFalse(settings.usesGeneratedMemberPreview(), "the master switch is off by default");

    settings.setCompilerIdeFeaturesEnabled(true);
    assertTrue(settings.usesGeneratedMemberPreview());

    settings.setCompilerNavigationEnabled(false);
    assertFalse(settings.usesGeneratedMemberPreview(), "the preview is part of compiler navigation");
  }

  @Test
  @DisplayName("unknown completion mode falls back to ide and compiler")
  public void unknownCompletionModeFallsBackToIdeAndCompiler() {
    HaxeCompilerProjectSettings.State state = new HaxeCompilerProjectSettings.State();
    state.completionMode = "garbage";
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.loadState(state);
    assertEquals(HaxeCompletionMode.IDE_AND_COMPILER, settings.getCompletionMode());
  }

  @Test
  @DisplayName("effective level falls back to default")
  public void effectiveLevelFallsBackToDefault() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setDefaultLanguageLevel(HaxeLanguageLevel.HAXE_4_2);
    settings.setContainerLanguageLevelOverride("app", HaxeLanguageLevel.HAXE_3_4);

    assertEquals(HaxeLanguageLevel.HAXE_3_4, settings.getEffectiveLanguageLevel("app"));
    assertEquals(HaxeLanguageLevel.HAXE_4_2, settings.getEffectiveLanguageLevel("lib"));
  }

  @Test
  @DisplayName("clearing an override restores default")
  public void clearingAnOverrideRestoresDefault() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setContainerLanguageLevelOverride("app", HaxeLanguageLevel.HAXE_4_0);
    settings.setContainerLanguageLevelOverride("app", null);

    assertNull(settings.getContainerLanguageLevelOverride("app"));
    assertEquals(settings.getDefaultLanguageLevel(), settings.getEffectiveLanguageLevel("app"));
  }

  @Test
  @DisplayName("overrides map replaces previous entries")
  public void overridesMapReplacesPreviousEntries() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setContainerLanguageLevelOverride("old", HaxeLanguageLevel.HAXE_4_0);
    settings.setContainerLanguageLevelOverrides(Map.of("app", HaxeLanguageLevel.HAXE_4_1));

    assertNull(settings.getContainerLanguageLevelOverride("old"));
    assertEquals(Map.of("app", HaxeLanguageLevel.HAXE_4_1), settings.getContainerLanguageLevelOverrides());
  }

  /** Every State field set away from its default; loadState only stores the bean, so this pins that each field serializes. */
  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setDefaultLanguageLevel(HaxeLanguageLevel.HAXE_4_3);
    settings.setContainerLanguageLevelOverride("app", HaxeLanguageLevel.HAXE_5_0);
    settings.setContainerLanguageLevelOverride("legacy", HaxeLanguageLevel.HAXE_3_4);
    settings.setCompilerDiagnosticsEnabled(true);
    settings.setDiagnosticsErrorsEnabled(false);
    settings.setDiagnosticsUnusedImportsEnabled(true);
    settings.setDiagnosticsRemovableCodeEnabled(true);
    settings.setCompilerDiagnosticsOnly(true);
    settings.setUseLanguageLevelForConditionals(false);
    settings.setCompletionMode(HaxeCompletionMode.COMPILER_ONLY);
    settings.setCompilerIdeFeaturesEnabled(true);
    settings.setCompilerInlaysEnabled(false);
    settings.setCompilerFindUsagesEnabled(false);
    settings.setCompilerNavigationEnabled(false);
    settings.setGeneratedMemberPreviewEnabled(false);
    settings.setCompilerCompletionEnabled(false);
    settings.setDisplayConnectionMode(HaxeServerGeneration.HAXE_4, HaxeDisplayConnectionMode.SHARED_SERVER);
    settings.setDisplayConnectionMode(HaxeServerGeneration.HAXE_3, HaxeDisplayConnectionMode.SHARED_SERVER);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);
    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertEquals(HaxeLanguageLevel.HAXE_4_3, reloaded.getExplicitDefaultLanguageLevel());
    assertEquals(HaxeLanguageLevel.HAXE_5_0, reloaded.getContainerLanguageLevelOverride("app"));
    assertEquals(HaxeLanguageLevel.HAXE_3_4, reloaded.getContainerLanguageLevelOverride("legacy"));
    assertTrue(reloaded.isCompilerDiagnosticsEnabled());
    assertFalse(reloaded.isDiagnosticsErrorsEnabled());
    assertTrue(reloaded.isDiagnosticsUnusedImportsEnabled());
    assertTrue(reloaded.isDiagnosticsRemovableCodeEnabled());
    assertTrue(reloaded.isCompilerDiagnosticsOnly());
    assertFalse(reloaded.isUseLanguageLevelForConditionals());
    assertEquals(HaxeCompletionMode.COMPILER_ONLY, reloaded.getCompletionMode());
    assertTrue(reloaded.isCompilerIdeFeaturesEnabled());
    assertFalse(reloaded.isCompilerInlaysEnabled());
    assertFalse(reloaded.isCompilerFindUsagesEnabled());
    assertFalse(reloaded.isCompilerNavigationEnabled());
    assertFalse(reloaded.isGeneratedMemberPreviewEnabled());
    assertFalse(reloaded.isCompilerCompletionEnabled());
    assertEquals(HaxeDisplayConnectionMode.SHARED_SERVER, reloaded.getDisplayConnectionMode(HaxeServerGeneration.HAXE_4));
    assertEquals(HaxeDisplayConnectionMode.SHARED_SERVER, reloaded.getDisplayConnectionMode(HaxeServerGeneration.HAXE_3));
  }

  @Test
  @DisplayName("unknown stored versions are ignored gracefully")
  public void unknownStoredVersionsAreIgnoredGracefully() {
    HaxeCompilerProjectSettings.State state = new HaxeCompilerProjectSettings.State();
    state.defaultLanguageLevel = "garbage";
    state.moduleLanguageLevels.put("app", "also-garbage");

    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.loadState(state);

    assertEquals(HaxeLanguageLevel.latest(), settings.getDefaultLanguageLevel());
    assertNull(settings.getContainerLanguageLevelOverride("app"));
    assertTrue(settings.getContainerLanguageLevelOverrides().isEmpty());
  }
}
