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
  @DisplayName("default language level is latest when unset")
  public void defaultLanguageLevelIsLatestWhenUnset() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    assertEquals(HaxeLanguageLevel.latest(), settings.getDefaultLanguageLevel());
  }

  @Test
  @DisplayName("use compiler level is the default mode")
  public void useCompilerLevelIsTheDefaultMode() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    assertNull(settings.getExplicitDefaultLanguageLevel());
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
  @DisplayName("conditional compilation uses the language level by default")
  public void conditionalCompilationUsesTheLanguageLevelByDefault() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    assertTrue(settings.isUseLanguageLevelForConditionals());
  }

  @Test
  @DisplayName("use language level for conditionals survives xml round trip")
  public void useLanguageLevelForConditionalsSurvivesXmlRoundTrip() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setUseLanguageLevelForConditionals(false);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);
    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertFalse(reloaded.isUseLanguageLevelForConditionals());
  }

  @Test
  @DisplayName("display connection mode defaults to the shared server and survives xml round trip")
  public void displayConnectionModeDefaultsToTheSharedServerAndSurvivesXmlRoundTrip() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    assertEquals(HaxeDisplayConnectionMode.SHARED_SERVER, settings.getDisplayConnectionMode());
    settings.setDisplayConnectionMode(HaxeDisplayConnectionMode.DEDICATED_PROCESS);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);
    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertEquals(HaxeDisplayConnectionMode.DEDICATED_PROCESS, reloaded.getDisplayConnectionMode());
  }

  @Test
  @DisplayName("use compiler level survives xml serialization round trip")
  public void useCompilerLevelSurvivesXmlSerializationRoundTrip() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setDefaultLanguageLevel(HaxeLanguageLevel.HAXE_4_1);
    settings.setDefaultLanguageLevel(null);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);
    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertNull(reloaded.getExplicitDefaultLanguageLevel());
  }

  @Test
  @DisplayName("completion mode defaults to ide and compiler and survives round trip")
  public void completionModeDefaultsToIdeAndCompilerAndSurvivesRoundTrip() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    assertEquals(HaxeCompletionMode.IDE_AND_COMPILER, settings.getCompletionMode());
    settings.setCompletionMode(HaxeCompletionMode.IDE_ONLY);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);
    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertEquals(HaxeCompletionMode.IDE_ONLY, reloaded.getCompletionMode());
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
  @DisplayName("compiler feature toggles survive xml round trip")
  public void compilerFeatureTogglesSurviveXmlRoundTrip() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setCompilerInlaysEnabled(false);
    settings.setCompilerFindUsagesEnabled(false);
    settings.setGeneratedMemberPreviewEnabled(false);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);
    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertFalse(reloaded.isCompilerInlaysEnabled());
    assertFalse(reloaded.isCompilerFindUsagesEnabled());
    assertTrue(reloaded.isCompilerNavigationEnabled());
    assertFalse(reloaded.isGeneratedMemberPreviewEnabled());
    assertTrue(reloaded.isCompilerCompletionEnabled());
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

  @Test
  @DisplayName("state survives xml serialization round trip")
  public void stateSurvivesXmlSerializationRoundTrip() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setDefaultLanguageLevel(HaxeLanguageLevel.HAXE_4_3);
    settings.setContainerLanguageLevelOverride("app", HaxeLanguageLevel.HAXE_5_0);
    settings.setContainerLanguageLevelOverride("legacy", HaxeLanguageLevel.HAXE_3_4);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);

    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertEquals(HaxeLanguageLevel.HAXE_4_3, reloaded.getDefaultLanguageLevel());
    assertEquals(HaxeLanguageLevel.HAXE_5_0, reloaded.getContainerLanguageLevelOverride("app"));
    assertEquals(HaxeLanguageLevel.HAXE_3_4, reloaded.getContainerLanguageLevelOverride("legacy"));
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

  @Test
  @DisplayName("diagnostics feature toggles default to errors only")
  public void diagnosticsFeatureTogglesDefaultToErrorsOnly() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);

    assertFalse(settings.isCompilerDiagnosticsEnabled());
    assertTrue(settings.isDiagnosticsErrorsEnabled());
    assertFalse(settings.isDiagnosticsUnusedImportsEnabled());
    assertFalse(settings.isDiagnosticsRemovableCodeEnabled());
  }

  @Test
  @DisplayName("diagnostics feature toggles survive serialization")
  public void diagnosticsFeatureTogglesSurviveSerialization() {
    HaxeCompilerProjectSettings settings = new HaxeCompilerProjectSettings(null);
    settings.setCompilerDiagnosticsEnabled(true);
    settings.setDiagnosticsErrorsEnabled(false);
    settings.setDiagnosticsUnusedImportsEnabled(true);
    settings.setDiagnosticsRemovableCodeEnabled(true);

    Element serialized = XmlSerializer.serialize(settings.getState());
    HaxeCompilerProjectSettings.State deserialized =
      XmlSerializer.deserialize(serialized, HaxeCompilerProjectSettings.State.class);
    HaxeCompilerProjectSettings reloaded = new HaxeCompilerProjectSettings(null);
    reloaded.loadState(deserialized);

    assertTrue(reloaded.isCompilerDiagnosticsEnabled());
    assertFalse(reloaded.isDiagnosticsErrorsEnabled());
    assertTrue(reloaded.isDiagnosticsUnusedImportsEnabled());
    assertTrue(reloaded.isDiagnosticsRemovableCodeEnabled());
  }
}
