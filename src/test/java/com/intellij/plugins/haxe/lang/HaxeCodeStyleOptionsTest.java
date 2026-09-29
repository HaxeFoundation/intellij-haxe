package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.*;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.psi.codeStyle.DocCommentSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The custom code style options the settings UI shows: each names a real
 * setting with a resolved title, and every custom setting is reachable in
 * the UI - the few that dedicated tabs own are listed here.
 */
@DisplayName("Code style: options")
public class HaxeCodeStyleOptionsTest extends HaxeLightFixtureTestCase {
  // settings the Wrap Rules, Imports, Conditional Compilation and hxformat tabs own
  private static final Set<String> DEDICATED_TAB_SETTINGS = Set.of(
    "USE_PROJECT_HXFORMAT", "FORMAT_INACTIVE_BRANCHES", "ALIGN_INACTIVE_CONDITIONAL_BRANCHES",
    "BLANK_LINES_BETWEEN_IMPORT_GROUPS", "IMPORT_GROUP_PACKAGE_DEPTH", "KEEP_BLANK_LINES_BETWEEN_IMPORTS",
    "BOOL_CHAIN_SPLIT_LINE_LENGTH", "BOOL_CHAIN_SPLIT_ITEM_LENGTH", "BOOL_CHAIN_SPLIT_ITEM_COUNT", "BOOL_CHAIN_SPLIT_TOTAL_LENGTH",
    "ADD_CHAIN_SPLIT_LINE_LENGTH", "ADD_CHAIN_SPLIT_ITEM_LENGTH", "ADD_CHAIN_SPLIT_ITEM_COUNT", "ADD_CHAIN_SPLIT_TOTAL_LENGTH",
    "MULTI_VAR_SPLIT_WIDTH", "MULTI_VAR_FILL_ITEM_LENGTH",
    "ARRAY_KEEP_TOTAL_LENGTH", "ARRAY_FILL_EQUAL_ITEM_LENGTH", "ARRAY_FILL_EQUAL_ITEM_COUNT",
    "ARRAY_FILL_ITEM_LENGTH", "ARRAY_FILL_ITEM_COUNT", "ARRAY_CHOP_ITEM_LENGTH", "ARRAY_CHOP_ITEM_COUNT",
    "MAP_KEEP_TOTAL_LENGTH", "MAP_FILL_EQUAL_ITEM_LENGTH", "MAP_FILL_EQUAL_ITEM_COUNT",
    "MAP_FILL_ITEM_LENGTH", "MAP_FILL_ITEM_COUNT", "MAP_CHOP_ITEM_LENGTH", "MAP_CHOP_ITEM_COUNT",
    "OBJECT_KEEP_ITEM_COUNT", "OBJECT_CHOP_ITEM_LENGTH", "OBJECT_CHOP_TOTAL_LENGTH", "OBJECT_CHOP_ITEM_COUNT");

  private static final List<SettingsType> TABS = List.of(
    SettingsType.SPACING_SETTINGS, SettingsType.BLANK_LINES_SETTINGS, SettingsType.WRAPPING_AND_BRACES_SETTINGS,
    SettingsType.INDENT_SETTINGS);

  @Override
  protected String getBasePath() {
    return "/formatter/comparison/";
  }

  @Test
  @DisplayName("custom options name settings and resolve their titles")
  public void testCustomOptionsNameSettingsAndResolveTheirTitles() {
    List<CustomOption> options = recordedOptions();

    List<String> problems = new ArrayList<>();
    for (CustomOption option : options) {
      if (option.settingsClass != HaxeCodeStyleSettings.class) problems.add(option.field + ": not a Haxe setting");
      else if (settingField(option.field) == null) problems.add(option.field + ": no such setting");
      if (option.title.isBlank() || option.title.startsWith("!")) problems.add(option.field + ": unresolved title " + option.title);
      if (option.group != null && option.group.startsWith("!")) problems.add(option.field + ": unresolved group " + option.group);
    }
    assertTrue(problems.isEmpty(), String.join("\n", problems));
  }

  @Test
  @DisplayName("every custom setting is exposed")
  public void testEveryCustomSettingIsExposed() {
    List<CustomOption> options = recordedOptions();

    Set<String> exposed = new TreeSet<>(DEDICATED_TAB_SETTINGS);
    options.forEach(option -> exposed.add(option.field));
    Set<String> settings = new TreeSet<>();
    for (Field field : HaxeCodeStyleSettings.class.getFields()) {
      boolean setting = !Modifier.isStatic(field.getModifiers()) && field.getDeclaringClass() == HaxeCodeStyleSettings.class;
      if (setting) settings.add(field.getName());
    }
    assertEquals(settings, exposed, "custom settings without a UI option");
  }

  /** The option tables render per field type: a mismatch (an int under a boolean table) throws while the tab builds. */
  @Test
  @DisplayName("every tab builds")
  public void testEveryTabBuilds() {
    CodeStyleSettings settings = projectSettingsCopy();
    HaxeCodeStyleConfigurable configurable = new HaxeCodeStyleConfigurable(settings, settings.clone());

    try {
      assertNotNull(configurable.createComponent());
    }
    finally {
      configurable.disposeUIResources();
    }
  }

  /** A sample the preview cannot parse renders as error text instead of demonstrating its options. */
  @ParameterizedTest(name = "{0}")
  @FieldSource("TABS")
  @DisplayName("preview samples parse and reformat")
  public void testPreviewSamplesParseAndReformat(SettingsType tab) {
    String sample = new HaxeLanguageCodeStyleSettingsProvider().getCodeSample(tab);

    reformat(sample);

    assertNull(PsiTreeUtil.findChildOfType(myFixture.getFile(), PsiErrorElement.class), tab + " sample has a syntax error");
  }

  /** (tab title, its preview sample) for the tabs with a sample of their own. */
  static final List<Arguments> PANEL_SAMPLES = List.of(
    arguments("Wrap Rules", HaxeWrapRulesCodeStylePanel.WRAP_RULES_CODE_SAMPLE),
    arguments("Imports", HaxeImportsCodeStylePanel.IMPORTS_CODE_SAMPLE),
    arguments("Conditional Compilation", HaxeConditionalCompilationPanel.CONDITIONAL_CODE_SAMPLE));

  @ParameterizedTest(name = "{0}")
  @FieldSource("PANEL_SAMPLES")
  @DisplayName("panel samples parse and reformat")
  public void testPanelSamplesParseAndReformat(String tab, String sample) {
    reformat(sample);

    assertNull(PsiTreeUtil.findChildOfType(myFixture.getFile(), PsiErrorElement.class), tab + " sample has a syntax error");
  }

  @Test
  @DisplayName("doc comment settings back the haxe toggle")
  public void testDocCommentSettingsBackTheHaxeToggle() {
    CodeStyleSettings settings = projectSettingsCopy();
    DocCommentSettings docSettings = new HaxeLanguageCodeStyleSettingsProvider().getDocCommentSettings(settings);

    docSettings.setDocFormattingEnabled(false);

    assertFalse(settings.getCustomSettings(HaxeCodeStyleSettings.class).FORMAT_DOC_COMMENTS);
    assertFalse(docSettings.isDocFormattingEnabled());
  }

  /** The custom options the provider shows across every option-table tab. */
  private static List<CustomOption> recordedOptions() {
    RecordingCustomizable recorder = new RecordingCustomizable();
    HaxeLanguageCodeStyleSettingsProvider provider = new HaxeLanguageCodeStyleSettingsProvider();
    for (SettingsType tab : TABS) {
      provider.customizeSettings(recorder, tab);
    }
    return recorder.options;
  }

  private static Field settingField(String name) {
    try {
      return HaxeCodeStyleSettings.class.getField(name);
    }
    catch (NoSuchFieldException e) {
      return null;
    }
  }

  private record CustomOption(Class<? extends CustomCodeStyleSettings> settingsClass, String field, String title, String group) {
  }

  /** Collects the custom options a provider shows; the standard-option calls are ignored. */
  private static final class RecordingCustomizable implements CodeStyleSettingsCustomizable {
    final List<CustomOption> options = new ArrayList<>();

    @Override
    public void showAllStandardOptions() {
    }

    @Override
    public void showStandardOptions(String @NotNull ... optionNames) {
    }

    @Override
    public void showCustomOption(@NotNull Class<? extends CustomCodeStyleSettings> settingsClass, @NotNull String fieldName,
                                 @NotNull String title, String groupName, Object... options) {
      this.options.add(new CustomOption(settingsClass, fieldName, title, groupName));
    }

    @Override
    public void showCustomOption(@NotNull Class<? extends CustomCodeStyleSettings> settingsClass, @NotNull String fieldName,
                                 @NotNull String title, String groupName, @NotNull OptionAnchor anchor, String anchorFieldName,
                                 Object... options) {
      this.options.add(new CustomOption(settingsClass, fieldName, title, groupName));
    }

    @Override
    public void renameStandardOption(@NotNull String fieldName, @NotNull String newTitle) {
    }
  }
}
