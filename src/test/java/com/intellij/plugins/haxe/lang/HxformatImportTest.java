package com.intellij.plugins.haxe.lang;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatDefaultProfile;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatJsonMapper;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The hxformat.json mapping behind the scheme importer: overrides land on the
 * right settings fields and unsupported keys are reported, not dropped.
 */
@DisplayName("Formatting: hxformat.json import")
public class HxformatImportTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/formatter/comparison/";
  }

  @Test
  @DisplayName("defaults plus overrides")
  public void testDefaultsPlusOverrides() throws Exception {
    CodeStyleSettings settings = freshDefaults();
    var root = new ObjectMapper().readTree("""
      {
        "indentation": { "character": "  ", "tabWidth": 2 },
        "wrapping": { "maxLineLength": 100 },
        "lineEnds": { "leftCurly": "before", "emptyCurly": "break" },
        "sameLine": { "ifElse": "next", "ifBody": "keep", "elseBody": "keep", "forBody": "keep",
                      "whileBody": "keep", "doWhileBody": "keep", "tryBody": "keep", "catchBody": "keep" },
        "whitespace": { "typeHintColonPolicy": "after", "typeCheckColonPolicy": "none", "unknownKey": true },
        "emptyLines": { "betweenSingleLineTypes": 2, "afterLeftCurly": "keep", "beforeBlocks": "keep",
                        "importAndUsing": { "betweenImports": 1, "betweenImportsLevel": "secondLevelPackage" } }
      }""");

    List<String> unsupported = HxformatJsonMapper.apply(settings, root);

    CommonCodeStyleSettings common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    assertFalse(settings.getIndentOptions(HaxeFileType.INSTANCE).USE_TAB_CHARACTER);
    assertEquals(2, settings.getIndentOptions(HaxeFileType.INSTANCE).INDENT_SIZE);
    assertEquals(100, settings.getRightMargin(HaxeLanguage.INSTANCE));
    assertEquals(CommonCodeStyleSettings.NEXT_LINE, common.BRACE_STYLE);
    assertFalse(common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE);
    assertTrue(common.ELSE_ON_NEW_LINE);
    assertEquals(HaxeCodeStyleSettings.BODY_PLACEMENT_KEEP, haxe.IF_BODY_PLACEMENT);
    assertEquals(HaxeCodeStyleSettings.BODY_PLACEMENT_KEEP, haxe.DO_WHILE_BODY_PLACEMENT);
    assertTrue(common.KEEP_CONTROL_STATEMENT_IN_ONE_LINE, "no body breaks, so the mirroring checkbox flips back on");
    assertEquals(1, haxe.KEEP_BLANK_LINES_AFTER_LBRACE, "afterLeftCurly=keep lifts the cap to the file-wide maximum");
    assertEquals(1, haxe.KEEP_BLANK_LINES_AFTER_CASE_COLON, "beforeBlocks=keep lifts the cap to the file-wide maximum");
    assertTrue(haxe.SPACE_AFTER_TYPE_REFERENCE_COLON);
    assertFalse(haxe.SPACE_BEFORE_TYPE_REFERENCE_COLON);
    assertFalse(haxe.SPACE_AROUND_TYPE_CHECK_COLON);
    assertEquals(2, haxe.KEEP_BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES);
    assertEquals(1, haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS);
    assertEquals(2, haxe.IMPORT_GROUP_PACKAGE_DEPTH);
    assertEquals(List.of("whitespace.unknownKey"), unsupported);
  }

  /** A config with only lineEnds and sameLine keys leaves wrapping at the profile. */
  @Test
  @DisplayName("line ends and same line only config")
  public void testLineEndsAndSameLineOnlyConfig() throws Exception {
    CodeStyleSettings settings = freshDefaults();
    var root = new ObjectMapper().readTree("""
      {
        "excludes": ["/assets", "/node_modules"],
        "lineEnds": {
          "leftCurly": "both",
          "rightCurly": "both",
          "objectLiteralCurly": { "leftCurly": "after" }
        },
        "sameLine": {
          "ifBody": "same", "ifElse": "next", "doWhile": "next",
          "tryBody": "next", "tryCatch": "next"
        }
      }""");

    List<String> unsupported = HxformatJsonMapper.apply(settings, root);

    CommonCodeStyleSettings common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    assertEquals(CommonCodeStyleSettings.NEXT_LINE, common.BRACE_STYLE);
    assertTrue(common.ELSE_ON_NEW_LINE);
    assertTrue(common.WHILE_ON_NEW_LINE);
    assertTrue(common.CATCH_ON_NEW_LINE);
    // ifBody=same joins if-bodies while tryBody=next keeps breaking; the
    // unset bodies stay at the Next default
    HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    assertEquals(HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE, haxe.IF_BODY_PLACEMENT);
    assertEquals(HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE, haxe.TRY_BODY_PLACEMENT);
    assertEquals(HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE, haxe.ELSE_BODY_PLACEMENT);
    assertFalse(common.KEEP_CONTROL_STATEMENT_IN_ONE_LINE);
    int uiChop = CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM | CommonCodeStyleSettings.WRAP_AS_NEEDED;
    assertEquals(uiChop, common.ARRAY_INITIALIZER_WRAP);
    assertEquals(uiChop, common.METHOD_CALL_CHAIN_WRAP);
    assertTrue(unsupported.isEmpty(), "a lineEnds/sameLine-only config maps completely, got: " + unsupported);
  }

  /** Wrapping rules, parens, brackets, the maxAnywhereInFile clamp and line ends. */
  @Test
  @DisplayName("audited sections map")
  public void testAuditedSectionsMap() throws Exception {
    CodeStyleSettings settings = freshDefaults();
    var root = new ObjectMapper().readTree("""
      {
        "lineEnds": { "lineEndCharacter": "LF" },
        "emptyLines": { "maxAnywhereInFile": 1, "betweenTypes": 3,
                        "importAndUsing": { "betweenImports": 1, "betweenImportsLevel": "all" } },
        "whitespace": {
          "parenConfig": { "callParens": { "openingPolicy": "around" } },
          "bracketConfig": { "arrayLiteralBrackets": { "openingPolicy": "around" } }
        },
        "wrapping": {
          "arrayWrap": { "defaultWrap": "noWrap" },
          "functionSignature": { "defaultWrap": "fillLine" },
          "methodChain": { "rules": [
            { "conditions": [ { "cond": "itemCount >= n", "value": 7 } ], "type": "fillLine" },
            { "conditions": [ { "cond": "exceedsMaxLineLength" } ], "type": "onePerLineAfterFirst" }
          ] },
          "multiVar": { "rules": [
            { "conditions": [ { "cond": "lineLength >= n", "value": 100 } ], "type": "onePerLineAfterFirst" }
          ] }
        }
      }""");

    List<String> unsupported = HxformatJsonMapper.apply(settings, root);

    CommonCodeStyleSettings common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    assertEquals("\n", settings.LINE_SEPARATOR);
    assertEquals(1, common.BLANK_LINES_AROUND_CLASS, "betweenTypes=3 clamped by maxAnywhereInFile=1");
    assertEquals(1, haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS);
    assertEquals(99, haxe.IMPORT_GROUP_PACKAGE_DEPTH, "level=all separates every import");
    assertTrue(common.SPACE_BEFORE_METHOD_CALL_PARENTHESES);
    assertTrue(common.SPACE_WITHIN_METHOD_CALL_PARENTHESES);
    assertTrue(common.SPACE_WITHIN_BRACKETS);
    assertEquals(CommonCodeStyleSettings.DO_NOT_WRAP, common.ARRAY_INITIALIZER_WRAP);
    assertEquals(CommonCodeStyleSettings.WRAP_AS_NEEDED, common.METHOD_PARAMETERS_WRAP);
    int uiChop = CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM | CommonCodeStyleSettings.WRAP_AS_NEEDED;
    assertEquals(uiChop, common.METHOD_CALL_CHAIN_WRAP, "the exceedsMaxLineLength rule decides");
    assertEquals(100, haxe.MULTI_VAR_SPLIT_WIDTH, "the multiVar split width lifts from the config rule");
    List<String> sortedUnsupported = unsupported.stream().sorted().toList();
    assertEquals(List.of("wrapping.methodChain.rules (rule engine approximated by one policy)",
                         "wrapping.multiVar (only the line-length split is reproduced)"),
                 sortedUnsupported);
  }

  @Test
  @DisplayName("custom bool chain rules set the split thresholds")
  public void testCustomBoolChainRulesSetTheSplitThresholds() throws Exception {
    CodeStyleSettings settings = freshDefaults();
    var root = new ObjectMapper().readTree("""
      {
        "wrapping": { "opBoolChain": { "rules": [
          { "conditions": [ { "cond": "totalItemLength <= n", "value": 90 } ], "type": "noWrap" },
          { "conditions": [ { "cond": "lineLength >= n", "value": 100 },
                            { "cond": "anyItemLength >= n", "value": 30 } ],
            "type": "onePerLineAfterFirst" },
          { "conditions": [ { "cond": "itemCount >= n", "value": 6 } ], "type": "onePerLineAfterFirst" }
        ] } }
      }""");

    HxformatJsonMapper.apply(settings, root);

    HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    assertEquals(100, haxe.BOOL_CHAIN_SPLIT_LINE_LENGTH);
    assertEquals(30, haxe.BOOL_CHAIN_SPLIT_ITEM_LENGTH);
    assertEquals(6, haxe.BOOL_CHAIN_SPLIT_ITEM_COUNT);
    assertEquals(90, haxe.BOOL_CHAIN_SPLIT_TOTAL_LENGTH);
  }

  /**
   * The tool's own complete default configuration must land exactly on the
   * profile: every guard rule preceding the overflow rule in its wrap rule
   * lists ("itemCount <= 3 and not exceeding -> noWrap") must lose to it.
   */
  @Test
  @DisplayName("tool default config round trips onto the profile")
  public void testToolDefaultConfigRoundTripsOntoTheProfile() throws Exception {
    CodeStyleSettings settings = freshDefaults();
    Map<String, Object> profile = settingsImage(settings);
    var root = new ObjectMapper().readTree(Path.of(getTestDataPath(), "default-hxformat.json").toFile());

    List<String> unsupported = HxformatJsonMapper.apply(settings, root);

    assertEquals(profile, settingsImage(settings));
    // an explained note ("... (approximated)", "key=value") is a known limit; a bare path is a key the mapper never read
    List<String> unreadKeys = unsupported.stream().filter(note -> !note.contains("(") && !note.contains("=")).toList();
    assertEquals(List.of(), unreadKeys, "every key of the default config is read by the mapper");
  }

  @Test
  @DisplayName("add line comment space off is honoured")
  public void testAddLineCommentSpaceOffIsHonoured() throws Exception {
    CodeStyleSettings settings = freshDefaults();
    var root = new ObjectMapper().readTree("""
      { "whitespace": { "addLineCommentSpace": false } }""");

    List<String> unsupported = HxformatJsonMapper.apply(settings, root);

    assertFalse(settings.getCustomSettings(HaxeCodeStyleSettings.class).ADD_LINE_COMMENT_SPACE);
    assertTrue(unsupported.isEmpty(), "a mapped key is not reported, got: " + unsupported);
  }

  @Test
  @DisplayName("empty file imports completely")
  public void testEmptyFileImportsCompletely() throws Exception {
    CodeStyleSettings settings = freshDefaults();

    List<String> unsupported = HxformatJsonMapper.apply(settings, new ObjectMapper().readTree("{}"));

    assertTrue(unsupported.isEmpty(), "a default config maps completely");
  }

  @Test
  @DisplayName("explicit defaults import completely")
  public void testExplicitDefaultsImportCompletely() throws Exception {
    CodeStyleSettings settings = freshDefaults();
    var root = new ObjectMapper().readTree("""
      {
        "indentation": { "character": "tab", "tabWidth": 4, "conditionalPolicy": "aligned" },
        "wrapping": { "maxLineLength": 160 },
        "lineEnds": { "leftCurly": "after", "rightCurly": "both", "emptyCurly": "noBreak" },
        "sameLine": { "ifElse": "same", "ifBody": "next", "functionBody": "next", "anonFunctionBody": "same" },
        "whitespace": { "binopPolicy": "around", "commaPolicy": "onlyAfter",
                        "typeHintColonPolicy": "none", "typeCheckColonPolicy": "around",
                        "formatStringInterpolation": true },
        "emptyLines": { "maxAnywhereInFile": 1, "afterPackage": 1, "betweenSingleLineTypes": 0,
                        "importAndUsing": { "betweenImports": 0 } }
      }""");

    List<String> unsupported = HxformatJsonMapper.apply(settings, root);

    assertTrue(unsupported.isEmpty(), "spelled-out defaults map completely, got: " + unsupported);
  }

  private CodeStyleSettings freshDefaults() {
    CodeStyleSettings settings = projectSettingsCopy();
    HxformatDefaultProfile.apply(settings);
    return settings;
  }

  /** Every Haxe-relevant value by name: the common and custom option fields, the indent options and the margin. */
  private static Map<String, Object> settingsImage(CodeStyleSettings settings) throws IllegalAccessException {
    Map<String, Object> image = new TreeMap<>();
    CommonCodeStyleSettings common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    for (Field field : CommonCodeStyleSettings.class.getFields()) {
      if (!Modifier.isStatic(field.getModifiers())) image.put("common." + field.getName(), field.get(common));
    }
    HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    for (Field field : HaxeCodeStyleSettings.class.getFields()) {
      if (!Modifier.isStatic(field.getModifiers())) image.put("haxe." + field.getName(), field.get(haxe));
    }
    CommonCodeStyleSettings.IndentOptions indent = settings.getIndentOptions(HaxeFileType.INSTANCE);
    image.put("indent.USE_TAB_CHARACTER", indent.USE_TAB_CHARACTER);
    image.put("indent.TAB_SIZE", indent.TAB_SIZE);
    image.put("indent.INDENT_SIZE", indent.INDENT_SIZE);
    image.put("indent.CONTINUATION_INDENT_SIZE", indent.CONTINUATION_INDENT_SIZE);
    image.put("rightMargin", settings.getRightMargin(HaxeLanguage.INSTANCE));
    image.put("lineSeparator", settings.LINE_SEPARATOR);
    return image;
  }
}
