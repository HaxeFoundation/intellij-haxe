package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.intellij.plugins.haxe.HaxeCodeStyleBundle;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Applies the keys of an hxformat.json on top of
 * {@link HxformatDefaultProfile}. It reads every key path it knows and marks
 * each one it maps as consumed. Every key it cannot honor is reported,
 * either as a note naming the limit or as the bare config path.
 * <p>
 * Where the formatter reproduces one of the tool's wrap rules, the mapper
 * "lifts" the rule: it copies the value of the rule's matching condition
 * into the corresponding threshold setting.
 */
public final class HxformatJsonMapper {
  // WhitespacePolicy values that put a space AFTER the token / BEFORE it
  private static final Set<String> SPACE_AFTER_POLICIES = Set.of("after", "onlyAfter", "around");
  private static final Set<String> SPACE_BEFORE_POLICIES = Set.of("before", "onlyBefore", "around");
  /** betweenImportsLevel value -> the grouping depth; "all" separates every import, which a depth past any real package reproduces. */
  private static final Map<String, Integer> IMPORT_LEVEL_DEPTHS = Map.of(
    "all", 99,
    "firstLevelPackage", 1,
    "secondLevelPackage", 2,
    "thirdLevelPackage", 3,
    "fourthLevelPackage", 4,
    "fifthLevelPackage", 5);

  private static final List<String> CONSTRUCTS_WITHOUT_WRAP_SETTING = List.of(
    "wrapping.typeParameter", "wrapping.metadataCallParameter", "wrapping.casePattern", "wrapping.anonType");
  private static final List<String> SHARP_PARENS = List.of("whitespace.parenConfig.sharpConditionParens");
  private static final List<String> MEMBER_BLANK_SECTIONS = List.of(
    "emptyLines.macroClassEmptyLines", "emptyLines.abstractEmptyLines", "emptyLines.externClassEmptyLines",
    "emptyLines.interfaceEmptyLines", "emptyLines.enumEmptyLines", "emptyLines.typedefEmptyLines",
    "emptyLines.enumAbstractEmptyLines");
  private static final List<String> BRACKET_CONSTRUCTS = List.of(
    "accessBrackets", "comprehensionBrackets", "arrayLiteralBrackets", "mapLiteralBrackets", "unknownBrackets");

  /**
   * Keys with only ONE value the formatter can honor, because it has no
   * setting for any other; any other value is reported. The keys are in the
   * config's section order, which is also the order of the reports. The
   * per-construct curly overrides are not listed, because the value they
   * accept depends on the global keys.
   */
  private static final Map<String, String> FIXED_VALUES = inSectionOrder(
    "disableFormatting", "false",
    // indentation
    "indentation.conditionalPolicy", "aligned",
    "indentation.indentCaseLabels", "true",
    "indentation.indentObjectLiteral", "true",
    "indentation.indentComplexValueExpressions", "false",
    "indentation.trailingWhitespace", "false",
    // wrapping: array matrices are never column-aligned
    "wrapping.arrayMatrixWrap", "noMatrixWrap",
    // lineEnds
    "lineEnds.rightCurly", "both",
    "lineEnds.sharp", "after",
    "lineEnds.caseColon", "after",
    "lineEnds.metadataType", "none",
    "lineEnds.metadataVar", "none",
    "lineEnds.metadataFunction", "none",
    "lineEnds.metadataOther", "none",
    // sameLine
    "sameLine.anonFunctionBody", "same",
    "sameLine.comprehensionFor", "same",
    "sameLine.untypedBody", "same",
    "sameLine.returnBody", "same",
    "sameLine.ifElseSemicolonNextLine", "true",
    "sameLine.expressionIfWithBlocks", "false",
    // whitespace
    "whitespace.dotPolicy", "none",
    "whitespace.colonPolicy", "none",
    "whitespace.caseColonPolicy", "onlyAfter",
    "whitespace.semicolonPolicy", "onlyAfter",
    "whitespace.intervalPolicy", "none",
    "whitespace.compressSuccessiveParenthesis", "true",
    "whitespace.bracesConfig.typedefBraces.openingPolicy", "before",
    "whitespace.bracesConfig.typedefBraces.closingPolicy", "onlyAfter",
    "whitespace.bracesConfig.typedefBraces.removeInnerWhenEmpty", "true",
    "whitespace.bracesConfig.anonTypeBraces.openingPolicy", "before",
    "whitespace.bracesConfig.anonTypeBraces.closingPolicy", "onlyAfter",
    "whitespace.bracesConfig.anonTypeBraces.removeInnerWhenEmpty", "true",
    "whitespace.bracesConfig.objectLiteralBraces.openingPolicy", "before",
    "whitespace.bracesConfig.objectLiteralBraces.closingPolicy", "onlyAfter",
    "whitespace.bracesConfig.objectLiteralBraces.removeInnerWhenEmpty", "true",
    "whitespace.bracesConfig.unknownBraces.openingPolicy", "before",
    "whitespace.bracesConfig.unknownBraces.closingPolicy", "onlyAfter",
    "whitespace.bracesConfig.unknownBraces.removeInnerWhenEmpty", "true",
    // emptyLines: boundaries finer than the formatter's one set of member
    // blank-line settings, so only the tool's defaults can be honored.
    // afterReturn and afterBlocks at their Remove default are covered by the
    // beforeRightCurly maximum and the rules that join keywords
    "emptyLines.classEmptyLines.betweenStaticVars", "0",
    "emptyLines.classEmptyLines.afterVars", "1",
    "emptyLines.classEmptyLines.afterStaticFunctions", "1",
    "emptyLines.classEmptyLines.betweenStaticFunctions", "1",
    "emptyLines.classEmptyLines.afterPrivateFunctions", "1",
    "emptyLines.classEmptyLines.existingBetweenFields", "keep",
    "emptyLines.conditionalsEmptyLines.afterIf", "0",
    "emptyLines.conditionalsEmptyLines.beforeElse", "0",
    "emptyLines.conditionalsEmptyLines.afterElse", "0",
    "emptyLines.conditionalsEmptyLines.beforeEnd", "0",
    "emptyLines.conditionalsEmptyLines.beforeError", "0",
    "emptyLines.conditionalsEmptyLines.afterError", "0",
    "emptyLines.afterReturn", "remove",
    "emptyLines.afterBlocks", "remove",
    "emptyLines.finalNewline", "true",
    "emptyLines.beforePackage", "0",
    "emptyLines.lineCommentsBetweenTypes", "keep",
    "emptyLines.lineCommentsBetweenFunctions", "keep",
    "emptyLines.importAndUsing.beforeUsing", "1");

  private final CodeStyleSettings settings;
  private final CommonCodeStyleSettings common;
  private final HaxeCodeStyleSettings haxe;
  private final JsonNode root;
  private final Set<String> consumed = new LinkedHashSet<>();
  private final List<String> unsupported = new ArrayList<>();
  // emptyLines.maxAnywhereInFile clamps EVERY other blank-line count
  private int blankLinesClamp = Integer.MAX_VALUE;

  /** Applies the config to the settings and returns what could not be honored: bare config paths and explained notes. */
  public static List<String> apply(@NotNull CodeStyleSettings settings, @NotNull JsonNode root) {
    HxformatJsonMapper mapper = new HxformatJsonMapper(settings, root);
    mapper.applyAll();
    return mapper.unsupported;
  }

  private HxformatJsonMapper(CodeStyleSettings settings, JsonNode root) {
    this.settings = settings;
    this.common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    this.haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    this.root = root;
  }

  private void applyAll() {
    // `excludes` holds CLI file globs - no code-style meaning in the IDE
    consumed.add("excludes");
    // editor metadata naming a JSON schema, not a formatter setting
    consumed.add("$schema");
    FIXED_VALUES.forEach(this::acceptOnly);
    applyIndentation();
    applyWrapping();
    applyLineEnds();
    applySameLine();
    applyWhitespace();
    applyEmptyLines();
    collectLeftovers(root, "");
  }

  private void applyIndentation() {
    String character = str("indentation.character");
    if (character != null) {
      settings.getIndentOptions(HaxeFileType.INSTANCE).USE_TAB_CHARACTER = "tab".equals(character);
    }
    Integer tabWidth = intVal("indentation.tabWidth");
    if (tabWidth != null) {
      CodeStyleSettings.IndentOptions indent = settings.getIndentOptions(HaxeFileType.INSTANCE);
      indent.TAB_SIZE = tabWidth;
      indent.INDENT_SIZE = tabWidth;
      indent.CONTINUATION_INDENT_SIZE = tabWidth * 2;
    }
  }

  private void applyWrapping() {
    Integer margin = intVal("wrapping.maxLineLength");
    if (margin != null) {
      settings.setRightMargin(HaxeLanguage.INSTANCE, margin);
    }
    applyLiteralRules("wrapping.arrayWrap", value -> common.ARRAY_INITIALIZER_WRAP = value, arraySetters());
    applyLiteralRules("wrapping.mapWrap", value -> common.ARRAY_INITIALIZER_WRAP = value, mapSetters());
    applyLiteralRules("wrapping.objectLiteral", value -> common.ARRAY_INITIALIZER_WRAP = value, objectSetters());
    wrapConstruct("wrapping.methodChain", value -> common.METHOD_CALL_CHAIN_WRAP = value);
    wrapConstruct("wrapping.implementsExtends", value -> common.EXTENDS_LIST_WRAP = value);
    wrapConstruct("wrapping.functionSignature", value -> common.METHOD_PARAMETERS_WRAP = value);
    wrapConstruct("wrapping.anonFunctionSignature", value -> common.METHOD_PARAMETERS_WRAP = value);
    wrapConstruct("wrapping.callParameter", value -> common.CALL_PARAMETERS_WRAP = value);
    // the &&/|| and +/- chains follow their own rules, fed from the
    // thresholds (HaxeOperatorChainRules), not a single wrap policy.
    // BINARY_OPERATION_WRAP stays off, so a margin wrap never competes with
    // a chopped method chain
    ChainSetters boolChain = new ChainSetters(
      value -> haxe.BOOL_CHAIN_SPLIT_LINE_LENGTH = value,
      value -> haxe.BOOL_CHAIN_SPLIT_ITEM_LENGTH = value,
      value -> haxe.BOOL_CHAIN_SPLIT_ITEM_COUNT = value,
      value -> haxe.BOOL_CHAIN_SPLIT_TOTAL_LENGTH = value);
    ChainSetters addChain = new ChainSetters(
      value -> haxe.ADD_CHAIN_SPLIT_LINE_LENGTH = value,
      value -> haxe.ADD_CHAIN_SPLIT_ITEM_LENGTH = value,
      value -> haxe.ADD_CHAIN_SPLIT_ITEM_COUNT = value,
      value -> haxe.ADD_CHAIN_SPLIT_TOTAL_LENGTH = value);
    applyChainRules("wrapping.opBoolChain", boolChain);
    applyChainRules("wrapping.opAddSubChain", addChain);
    reportUnsupportedSubtrees(CONSTRUCTS_WITHOUT_WRAP_SETTING, "hxformat.unsupported.no.wrap.target");
    applyMultiVar();
  }

  /**
   * Lifts the split width and the fill item length from matching rules. The
   * tool's joining of short multi-var declarations is not reproduced, so
   * the section is always reported.
   */
  private void applyMultiVar() {
    if (node("wrapping.multiVar") == null) return;
    markConsumedSubtree("wrapping.multiVar");
    JsonNode rules = node("wrapping.multiVar.rules");
    if (rules != null && rules.isArray()) {
      for (JsonNode rule : rules) {
        String type = rule.path("type").asString("");
        Integer line = conditionValue(rule, "lineLength >= n");
        if ("onePerLineAfterFirst".equals(type) && line != null) {
          haxe.MULTI_VAR_SPLIT_WIDTH = line;
        }
        Integer shortItem = conditionValue(rule, "anyItemLength <= n");
        if ("fillLine".equals(type) && shortItem != null) {
          haxe.MULTI_VAR_FILL_ITEM_LENGTH = shortItem;
        }
      }
    }
    unsupported.add(HaxeCodeStyleBundle.message("hxformat.unsupported.multi.var"));
  }

  /**
   * Maps a construct onto one wrap policy. The tool picks the FIRST rule
   * whose conditions all hold, but a construct here has only one policy.
   * That policy comes from the rule that fires on a margin overflow; the
   * guard rules before it in the tool's defaults ("itemCount <= 3 and NOT
   * exceeding -> noWrap") must not win. Without an overflow rule, the first
   * rule decides, then defaultWrap. Rules are reported, since one policy only
   * approximates them.
   */
  private void wrapConstruct(String path, IntConsumer setter) {
    JsonNode construct = node(path);
    if (construct == null) return;
    markConsumedSubtree(path);
    if (hasRules(construct)) {
      unsupported.add(HaxeCodeStyleBundle.message("hxformat.unsupported.rules", path));
    }
    applyWrapType(construct, setter);
  }

  /**
   * Maps wrapping.arrayWrap, mapWrap or objectLiteral. The overflow rule, or
   * defaultWrap, sets the array wrap policy as for any construct. The item
   * rules that HaxeLiteralItemRules reproduces are lifted into the kind's
   * thresholds. A rule of a shape the kind has no threshold for is reported.
   */
  private void applyLiteralRules(String path, IntConsumer wrapSetter, LiteralSetters setters) {
    JsonNode construct = node(path);
    if (construct == null) return;
    markConsumedSubtree(path);
    applyWrapType(construct, wrapSetter);
    if (!hasRules(construct)) return;
    boolean allLifted = true;
    for (JsonNode rule : construct.get("rules")) {
      allLifted &= liftLiteralRule(rule, setters);
    }
    if (!allLifted) {
      unsupported.add(HaxeCodeStyleBundle.message("hxformat.unsupported.array.rules", path));
    }
  }

  /**
   * Lifts one literal rule into its thresholds. These shapes are accepted:
   * <ul>
   * <li>noWrap with totalItemLength, or with itemCount (whose
   * exceedsMaxLineLength guard HaxeLiteralItemRules applies itself);</li>
   * <li>onePerLine with anyItemLength, totalItemLength or itemCount, and its
   * hasMultilineItems and exceedsMaxLineLength forms, which
   * HaxeLiteralItemRules applies itself;</li>
   * <li>fillLineWithLeadingBreak with allItemLengths and itemCount, with or
   * without equalItemLengths.</li>
   * </ul>
   * Returns false for any other shape, and for a threshold the kind lacks.
   */
  private static boolean liftLiteralRule(JsonNode rule, LiteralSetters setters) {
    String type = rule.path("type").asString("");
    boolean builtIn = conditionValue(rule, "hasMultilineItems") != null || conditionValue(rule, "exceedsMaxLineLength") != null;
    boolean equalLengths = conditionValue(rule, "equalItemLengths") != null;
    return switch (type) {
      case "noWrap" -> lift(rule, "totalItemLength <= n", setters.keepTotalLength())
                       | lift(rule, "itemCount <= n", setters.keepItemCount());
      case "onePerLine" -> builtIn
                           || lift(rule, "anyItemLength >= n", setters.chopItemLength())
                              | lift(rule, "totalItemLength >= n", setters.chopTotalLength())
                              | lift(rule, "itemCount >= n", setters.chopItemCount());
      case "fillLineWithLeadingBreak" -> equalLengths
                                         ? lift(rule, "allItemLengths <= n", setters.fillEqualItemLength())
                                           & lift(rule, "itemCount >= n", setters.fillEqualItemCount())
                                         : lift(rule, "allItemLengths <= n", setters.fillItemLength())
                                           & lift(rule, "itemCount >= n", setters.fillItemCount());
      default -> false;
    };
  }

  /** Sets the threshold from the rule's condition; false when the condition is absent or the kind lacks the threshold. */
  private static boolean lift(JsonNode rule, String condition, @Nullable IntConsumer threshold) {
    Integer value = conditionValue(rule, condition);
    if (value == null || threshold == null) return false;
    threshold.accept(value);
    return true;
  }

  /** The threshold setters of one literal kind; null for a threshold the kind lacks. */
  private record LiteralSetters(@Nullable IntConsumer keepItemCount, @Nullable IntConsumer keepTotalLength,
                                @Nullable IntConsumer fillEqualItemLength, @Nullable IntConsumer fillEqualItemCount,
                                @Nullable IntConsumer fillItemLength, @Nullable IntConsumer fillItemCount,
                                @Nullable IntConsumer chopItemLength, @Nullable IntConsumer chopTotalLength,
                                @Nullable IntConsumer chopItemCount) {
  }

  private LiteralSetters arraySetters() {
    return new LiteralSetters(null, v -> haxe.ARRAY_KEEP_TOTAL_LENGTH = v,
                              v -> haxe.ARRAY_FILL_EQUAL_ITEM_LENGTH = v, v -> haxe.ARRAY_FILL_EQUAL_ITEM_COUNT = v,
                              v -> haxe.ARRAY_FILL_ITEM_LENGTH = v, v -> haxe.ARRAY_FILL_ITEM_COUNT = v,
                              v -> haxe.ARRAY_CHOP_ITEM_LENGTH = v, null, v -> haxe.ARRAY_CHOP_ITEM_COUNT = v);
  }

  private LiteralSetters mapSetters() {
    return new LiteralSetters(null, v -> haxe.MAP_KEEP_TOTAL_LENGTH = v,
                              v -> haxe.MAP_FILL_EQUAL_ITEM_LENGTH = v, v -> haxe.MAP_FILL_EQUAL_ITEM_COUNT = v,
                              v -> haxe.MAP_FILL_ITEM_LENGTH = v, v -> haxe.MAP_FILL_ITEM_COUNT = v,
                              v -> haxe.MAP_CHOP_ITEM_LENGTH = v, null, v -> haxe.MAP_CHOP_ITEM_COUNT = v);
  }

  private LiteralSetters objectSetters() {
    return new LiteralSetters(v -> haxe.OBJECT_KEEP_ITEM_COUNT = v, null,
                              null, null,
                              null, null,
                              v -> haxe.OBJECT_CHOP_ITEM_LENGTH = v, v -> haxe.OBJECT_CHOP_TOTAL_LENGTH = v,
                              v -> haxe.OBJECT_CHOP_ITEM_COUNT = v);
  }

  private static boolean hasRules(JsonNode construct) {
    JsonNode rules = construct.get("rules");
    return rules != null && rules.isArray() && !rules.isEmpty();
  }

  /** The construct's wrap policy: its first rule firing on overflow, else its first rule, else defaultWrap. */
  private static void applyWrapType(JsonNode construct, IntConsumer setter) {
    String type = null;
    if (hasRules(construct)) {
      for (JsonNode rule : construct.get("rules")) {
        JsonNode ruleType = rule.get("type");
        if (ruleType == null) continue;
        if (type == null) type = ruleType.asString();
        if (firesOnOverflow(rule)) {
          type = ruleType.asString();
          break;
        }
      }
    }
    if (type == null && construct.get("defaultWrap") != null) {
      type = construct.get("defaultWrap").asString();
    }
    if (type == null) return;
    setter.accept(switch (type) {
      case "onePerLine", "onePerLineAfterFirst", "equalNumber" -> HxformatDefaultProfile.UI_CHOP_DOWN;
      case "fillLine", "fillLineWithLeadingBreak" -> CommonCodeStyleSettings.WRAP_AS_NEEDED;
      default -> CommonCodeStyleSettings.DO_NOT_WRAP; // noWrap, keep
    });
  }

  /**
   * Whether the rule has an exceedsMaxLineLength condition that asks for an
   * overflow: value 1, or no value. The tool reads any other value as "not
   * exceeding", the guard form.
   */
  private static boolean firesOnOverflow(JsonNode rule) {
    JsonNode conditions = rule.get("conditions");
    if (conditions == null || !conditions.isArray()) return false;
    for (JsonNode condition : conditions) {
      boolean overflow = "exceedsMaxLineLength".equals(condition.path("cond").asString(""))
                         && condition.path("value").asInt(1) == 1;
      if (overflow) return true;
    }
    return false;
  }

  private void applyLineEnds() {
    String leftCurly = str("lineEnds.leftCurly");
    if (leftCurly != null) {
      int style = "after".equals(leftCurly) ? CommonCodeStyleSettings.END_OF_LINE : CommonCodeStyleSettings.NEXT_LINE;
      common.BRACE_STYLE = style;
      common.METHOD_BRACE_STYLE = style;
    }
    String emptyCurly = str("lineEnds.emptyCurly");
    if (emptyCurly != null) {
      boolean collapse = "noBreak".equals(emptyCurly);
      common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = collapse;
      common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = collapse;
      common.KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE = collapse;
    }
    String lineEnd = str("lineEnds.lineEndCharacter");
    if (lineEnd != null) {
      settings.LINE_SEPARATOR = switch (lineEnd) {
        case "LF" -> "\n";
        case "CRLF" -> "\r\n";
        case "CR" -> "\r";
        default -> null; // auto - detect per file
      };
    }
    // per-construct curly overrides are honored only when they restate what
    // the global keys already produce; object literals always use the After
    // style here, whatever BRACE_STYLE says
    String effectiveLeft = leftCurly == null ? "after" : leftCurly;
    String effectiveEmpty = emptyCurly == null ? "noBreak" : emptyCurly;
    for (String construct : List.of("blockCurly", "anonFunctionCurly", "anonTypeCurly", "typedefCurly")) {
      curlyOverride("lineEnds." + construct, effectiveLeft, effectiveEmpty);
    }
    curlyOverride("lineEnds.objectLiteralCurly", "after", effectiveEmpty);
  }

  private void curlyOverride(String path, String supportedLeft, String supportedEmpty) {
    if (node(path) == null) return;
    acceptOnly(path + ".leftCurly", supportedLeft);
    acceptOnly(path + ".rightCurly", "both");
    acceptOnly(path + ".emptyCurly", supportedEmpty);
  }

  private void applySameLine() {
    onNewLine("sameLine.ifElse", value -> common.ELSE_ON_NEW_LINE = value);
    onNewLine("sameLine.doWhile", value -> common.WHILE_ON_NEW_LINE = value);
    onNewLine("sameLine.tryCatch", value -> common.CATCH_ON_NEW_LINE = value);
    applyEquals("sameLine.elseIf", "same", value -> common.SPECIAL_ELSE_IF_TREATMENT = value);
    bodyPlacement("sameLine.ifBody", value -> haxe.IF_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.elseBody", value -> haxe.ELSE_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.forBody", value -> haxe.FOR_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.whileBody", value -> haxe.WHILE_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.doWhileBody", value -> haxe.DO_WHILE_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.tryBody", value -> haxe.TRY_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.catchBody", value -> haxe.CATCH_BODY_PLACEMENT = value);
    // the engine reads the per-construct placements; the common checkbox
    // only mirrors them for the settings UI
    List<Integer> bodyPlacements = List.of(haxe.IF_BODY_PLACEMENT, haxe.ELSE_BODY_PLACEMENT,
                                           haxe.FOR_BODY_PLACEMENT, haxe.WHILE_BODY_PLACEMENT,
                                           haxe.DO_WHILE_BODY_PLACEMENT, haxe.TRY_BODY_PLACEMENT,
                                           haxe.CATCH_BODY_PLACEMENT);
    common.KEEP_CONTROL_STATEMENT_IN_ONE_LINE =
      !bodyPlacements.contains(HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE);
    applyEquals("sameLine.functionBody", "next", value -> haxe.FUNCTION_EXPRESSION_BODY_ON_NEXT_LINE = value);
    applyEquals("sameLine.returnBodySingleLine", "same", value -> haxe.RETURN_VALUE_ON_SAME_LINE = value);
    bodyPlacement("sameLine.caseBody", value -> haxe.CASE_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.expressionIf", value -> haxe.VALUE_IF_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.expressionTry", value -> haxe.VALUE_TRY_BODY_PLACEMENT = value);
    bodyPlacement("sameLine.expressionCase", value -> haxe.VALUE_CASE_BODY_PLACEMENT = value);
  }

  private void applyWhitespace() {
    applyKeywordAndOperatorSpacing();
    applyTypeAndArrowSpacing();
    String interpolation = str("whitespace.formatStringInterpolation");
    if (interpolation != null && !"true".equals(interpolation)) {
      unsupported.add(HaxeCodeStyleBundle.message("hxformat.unsupported.string.interpolation"));
    }
    applyEquals("whitespace.addLineCommentSpace", "true", value -> haxe.ADD_LINE_COMMENT_SPACE = value);
    applyParenConfig();
    applyBracesConfig();
    applyBracketConfig();
  }

  private void applyKeywordAndOperatorSpacing() {
    spaceAfterKeyword("whitespace.ifPolicy", value -> common.SPACE_BEFORE_IF_PARENTHESES = value);
    spaceAfterKeyword("whitespace.whilePolicy", value -> common.SPACE_BEFORE_WHILE_PARENTHESES = value);
    spaceAfterKeyword("whitespace.doPolicy", value -> common.SPACE_BEFORE_WHILE_PARENTHESES = value);
    spaceAfterKeyword("whitespace.forPolicy", value -> common.SPACE_BEFORE_FOR_PARENTHESES = value);
    spaceAfterKeyword("whitespace.switchPolicy", value -> common.SPACE_BEFORE_SWITCH_PARENTHESES = value);
    spaceAfterKeyword("whitespace.catchPolicy", value -> common.SPACE_BEFORE_CATCH_PARENTHESES = value);
    spaceAfterKeyword("whitespace.tryPolicy", value -> common.SPACE_BEFORE_TRY_LBRACE = value);
    applyEquals("whitespace.binopPolicy", "around", around -> {
      common.SPACE_AROUND_ASSIGNMENT_OPERATORS = around;
      common.SPACE_AROUND_LOGICAL_OPERATORS = around;
      common.SPACE_AROUND_EQUALITY_OPERATORS = around;
      common.SPACE_AROUND_RELATIONAL_OPERATORS = around;
      common.SPACE_AROUND_ADDITIVE_OPERATORS = around;
      common.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = around;
      common.SPACE_AROUND_BITWISE_OPERATORS = around;
      common.SPACE_AROUND_SHIFT_OPERATORS = around;
    });
    applyEquals("whitespace.ternaryPolicy", "around", around -> {
      common.SPACE_BEFORE_QUEST = around;
      common.SPACE_AFTER_QUEST = around;
      common.SPACE_BEFORE_COLON = around;
      common.SPACE_AFTER_COLON = around;
    });
    String comma = str("whitespace.commaPolicy");
    if (comma != null) {
      common.SPACE_BEFORE_COMMA = SPACE_BEFORE_POLICIES.contains(comma);
      boolean after = SPACE_AFTER_POLICIES.contains(comma);
      common.SPACE_AFTER_COMMA = after;
      common.SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS = after;
    }
  }

  private void applyTypeAndArrowSpacing() {
    String typeHint = str("whitespace.typeHintColonPolicy");
    if (typeHint != null) {
      haxe.SPACE_BEFORE_TYPE_REFERENCE_COLON = SPACE_BEFORE_POLICIES.contains(typeHint);
      haxe.SPACE_AFTER_TYPE_REFERENCE_COLON = SPACE_AFTER_POLICIES.contains(typeHint);
    }
    applyEquals("whitespace.typeCheckColonPolicy", "around", value -> haxe.SPACE_AROUND_TYPE_CHECK_COLON = value);
    String paramOpen = str("whitespace.typeParamOpenPolicy");
    String paramClose = str("whitespace.typeParamClosePolicy");
    if (paramOpen != null || paramClose != null) {
      haxe.SPACE_WITHIN_TYPE_PARAMETERS = "around".equals(paramOpen) || "around".equals(paramClose);
    }
    applyEquals("whitespace.typeExtensionPolicy", "after", value -> haxe.STRUCTURE_EXTENSION_ON_OWN_LINE = value);
    applyEquals("whitespace.arrowFunctionsPolicy", "around", value -> haxe.SPACE_AROUND_ARROW = value);
    applyEquals("whitespace.functionTypeHaxe4Policy", "around", value -> haxe.SPACE_AROUND_FUNCTION_TYPE_ARROW = value);
    applyEquals("whitespace.functionTypeHaxe3Policy", "around", value -> haxe.SPACE_AROUND_OLD_FUNCTION_TYPE_ARROW = value);
    String objectFieldColon = str("whitespace.objectFieldColonPolicy");
    if (objectFieldColon != null) {
      haxe.SPACE_BEFORE_OBJECT_FIELD_COLON = SPACE_BEFORE_POLICIES.contains(objectFieldColon);
      haxe.SPACE_AFTER_OBJECT_FIELD_COLON = SPACE_AFTER_POLICIES.contains(objectFieldColon);
    }
  }

  private void applyParenConfig() {
    openClose("whitespace.parenConfig.metadataParens",
              within -> haxe.SPACE_WITHIN_METADATA_PARENTHESES = within, null);
    openClose("whitespace.parenConfig.callParens",
              within -> common.SPACE_WITHIN_METHOD_CALL_PARENTHESES = within,
              before -> common.SPACE_BEFORE_METHOD_CALL_PARENTHESES = before);
    openClose("whitespace.parenConfig.funcParamParens",
              within -> common.SPACE_WITHIN_METHOD_PARENTHESES = within,
              before -> common.SPACE_BEFORE_METHOD_PARENTHESES = before);
    openClose("whitespace.parenConfig.anonFuncParamParens",
              within -> common.SPACE_WITHIN_METHOD_PARENTHESES = within, null);
    openClose("whitespace.parenConfig.expressionParens",
              within -> common.SPACE_WITHIN_PARENTHESES = within, null);
    openClose("whitespace.parenConfig.conditionParens", within -> {
      common.SPACE_WITHIN_IF_PARENTHESES = within;
      common.SPACE_WITHIN_WHILE_PARENTHESES = within;
      common.SPACE_WITHIN_FOR_PARENTHESES = within;
      common.SPACE_WITHIN_SWITCH_PARENTHESES = within;
      common.SPACE_WITHIN_CATCH_PARENTHESES = within;
    }, null);
    openClose("whitespace.parenConfig.ifConditionParens",
              within -> common.SPACE_WITHIN_IF_PARENTHESES = within, null);
    openClose("whitespace.parenConfig.switchConditionParens",
              within -> common.SPACE_WITHIN_SWITCH_PARENTHESES = within, null);
    openClose("whitespace.parenConfig.whileConditionParens",
              within -> common.SPACE_WITHIN_WHILE_PARENTHESES = within, null);
    openClose("whitespace.parenConfig.catchParens",
              within -> common.SPACE_WITHIN_CATCH_PARENTHESES = within, null);
    openClose("whitespace.parenConfig.forLoopParens",
              within -> common.SPACE_WITHIN_FOR_PARENTHESES = within, null);
    reportUnsupportedSubtrees(SHARP_PARENS, "hxformat.unsupported.sharp.parens");
  }

  private void applyBracesConfig() {
    openClose("whitespace.bracesConfig.blockBraces", null, before -> {
      common.SPACE_BEFORE_METHOD_LBRACE = before;
      common.SPACE_BEFORE_IF_LBRACE = before;
      common.SPACE_BEFORE_ELSE_LBRACE = before;
      common.SPACE_BEFORE_DO_LBRACE = before;
      common.SPACE_BEFORE_WHILE_LBRACE = before;
      common.SPACE_BEFORE_FOR_LBRACE = before;
      common.SPACE_BEFORE_SWITCH_LBRACE = before;
      common.SPACE_BEFORE_TRY_LBRACE = before;
      common.SPACE_BEFORE_CATCH_LBRACE = before;
    });
  }

  /** One flag stands for every bracket construct: a space within any of them puts it within all. */
  private void applyBracketConfig() {
    Boolean within = null;
    for (String construct : BRACKET_CONSTRUCTS) {
      String path = "whitespace.bracketConfig." + construct;
      if (node(path) == null) continue;
      String opening = str(path + ".openingPolicy");
      String closing = str(path + ".closingPolicy");
      acceptOnly(path + ".removeInnerWhenEmpty", "true");
      if (opening != null || closing != null) {
        boolean constructWithin = spaceWithin(opening, closing);
        within = within == null ? constructWithin : within | constructWithin;
      }
    }
    if (within != null) {
      common.SPACE_WITHIN_BRACKETS = within;
    }
  }

  private void applyEmptyLines() {
    Integer maxAnywhere = intVal("emptyLines.maxAnywhereInFile");
    if (maxAnywhere != null) {
      blankLinesClamp = maxAnywhere;
      common.KEEP_BLANK_LINES_IN_CODE = maxAnywhere;
      common.KEEP_BLANK_LINES_IN_DECLARATIONS = maxAnywhere;
    }
    applyBlankLineCount("emptyLines.afterPackage", value -> common.BLANK_LINES_AFTER_PACKAGE = value);
    // an exact count, so it is both the minimum and the maximum
    applyBlankLineCount("emptyLines.betweenTypes", value -> {
      common.BLANK_LINES_AROUND_CLASS = value;
      haxe.KEEP_BLANK_LINES_BETWEEN_TYPES = value;
    });
    applyBlankLineCount("emptyLines.betweenSingleLineTypes", value -> haxe.KEEP_BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES = value);
    applyBlankLineCount("emptyLines.afterFileHeaderComment", value -> haxe.MINIMUM_BLANK_LINES_AFTER_FILE_HEADER = value);
    applyBlankLineCount("emptyLines.classEmptyLines.betweenVars", value -> common.BLANK_LINES_AROUND_FIELD = value);
    applyBlankLineCount("emptyLines.classEmptyLines.betweenFunctions", value -> common.BLANK_LINES_AROUND_METHOD = value);
    applyBlankLineCount("emptyLines.classEmptyLines.beginType", value -> common.BLANK_LINES_AFTER_CLASS_HEADER = value);
    applyBlankLineCount("emptyLines.classEmptyLines.endType", value -> common.BLANK_LINES_BEFORE_CLASS_END = value);
    applyBlockEdgeBlankLines();
    applyImportBlankLines();
    applyBlankLineCount("emptyLines.classEmptyLines.afterStaticVars", value -> haxe.BLANK_LINES_BETWEEN_FIELD_GROUPS = value);
    applyBlankLineCount("emptyLines.classEmptyLines.afterPrivateVars", value -> haxe.BLANK_LINES_BETWEEN_FIELD_GROUPS = value);
    reportUnsupportedSubtrees(MEMBER_BLANK_SECTIONS, "hxformat.unsupported.member.blanks");
    // "ignore" keeps the written shape, which 0 also does here
    applyCommentPolicy("emptyLines.beforeDocCommentEmptyLines", value -> haxe.BLANK_LINES_BEFORE_FIELD_DOC_COMMENT = value);
    applyCommentPolicy("emptyLines.afterFieldsWithDocComments", value -> haxe.BLANK_LINES_AFTER_DOCUMENTED_FIELD = value);
  }

  /** The maximum blank lines at a block's edges: 0 for a "remove" policy, otherwise the file-wide maximum. */
  private void applyBlockEdgeBlankLines() {
    String beforeRCurly = str("emptyLines.beforeRightCurly");
    if (beforeRCurly != null) {
      common.KEEP_BLANK_LINES_BEFORE_RBRACE = "remove".equals(beforeRCurly) ? 0 : common.KEEP_BLANK_LINES_IN_DECLARATIONS;
    }
    String afterLCurly = str("emptyLines.afterLeftCurly");
    if (afterLCurly != null) {
      haxe.KEEP_BLANK_LINES_AFTER_LBRACE = "remove".equals(afterLCurly) ? 0 : common.KEEP_BLANK_LINES_IN_CODE;
    }
    // beyond the brace rules, beforeBlocks affects only the blank line
    // between a case's ':' and its body
    String beforeBlocks = str("emptyLines.beforeBlocks");
    if (beforeBlocks != null) {
      haxe.KEEP_BLANK_LINES_AFTER_CASE_COLON = "remove".equals(beforeBlocks) ? 0 : common.KEEP_BLANK_LINES_IN_CODE;
    }
    // the tool's exact count between stacked block comments becomes a maximum
    applyBlankLineCount("emptyLines.betweenMultilineComments", value -> haxe.KEEP_BLANK_LINES_BETWEEN_MULTILINE_COMMENTS = value);
  }

  private void applyImportBlankLines() {
    applyBlankLineCount("emptyLines.importAndUsing.beforeType", value -> {
      common.BLANK_LINES_AFTER_IMPORTS = value;
      haxe.MINIMUM_BLANK_LINES_AFTER_USING = value;
    });
    Integer betweenImports = intVal("emptyLines.importAndUsing.betweenImports");
    String importsLevel = str("emptyLines.importAndUsing.betweenImportsLevel");
    if (betweenImports != null || importsLevel != null) {
      applyImportGrouping(betweenImports == null ? 1 : betweenImports,
                          importsLevel == null ? "all" : importsLevel);
    }
  }

  private void applyImportGrouping(int betweenImports, String level) {
    if (betweenImports == 0) {
      haxe.KEEP_BLANK_LINES_BETWEEN_IMPORTS = 0;
      haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS = 0;
      return;
    }
    Integer depth = IMPORT_LEVEL_DEPTHS.get(level);
    if (depth == null) {
      // fullPackage compares the package WITHOUT the class name, but the
      // grouping key includes it, so imports of one package would still separate
      unsupported.add("emptyLines.importAndUsing.betweenImportsLevel=" + level);
      return;
    }
    haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS = Math.min(betweenImports, blankLinesClamp);
    haxe.IMPORT_GROUP_PACKAGE_DEPTH = depth;
  }

  /**
   * Reads an OpenClosePolicy object into two flags. "Within" is a space just
   * inside the pair: the opening policy puts one after '(', or the closing
   * policy one before ')'. "Before" is the opening policy's space before '('.
   */
  private void openClose(String path, @Nullable Consumer<Boolean> withinSetter, @Nullable Consumer<Boolean> beforeSetter) {
    if (node(path) == null) return;
    String opening = str(path + ".openingPolicy");
    String closing = str(path + ".closingPolicy");
    acceptOnly(path + ".removeInnerWhenEmpty", "true");
    if (withinSetter != null && (opening != null || closing != null)) {
      withinSetter.accept(spaceWithin(opening, closing));
    }
    if (beforeSetter != null && opening != null) {
      beforeSetter.accept(SPACE_BEFORE_POLICIES.contains(opening));
    }
  }

  /** An OpenClosePolicy pair puts a space just inside: the opening one after the opener, or the closing one before the closer. */
  private static boolean spaceWithin(@Nullable String opening, @Nullable String closing) {
    return (opening != null && SPACE_AFTER_POLICIES.contains(opening))
           || (closing != null && SPACE_BEFORE_POLICIES.contains(closing));
  }

  /** A sameLine policy for a keyword after a block (next / same) into its on-new-line flag; keep is reported. */
  private void onNewLine(String path, Consumer<Boolean> setter) {
    String value = str(path);
    if (value == null) return;
    if ("keep".equals(value)) {
      unsupported.add(path + "=keep");
      return;
    }
    setter.accept("next".equals(value));
  }

  /** A keyword's whitespace policy: a space after the keyword is the space before its parenthesis or brace. */
  private void spaceAfterKeyword(String path, Consumer<Boolean> setter) {
    String value = str(path);
    if (value == null) return;
    setter.accept(SPACE_AFTER_POLICIES.contains(value));
  }

  /** Sets the flag to whether the key holds the value; an absent key leaves the profile's flag. */
  private void applyEquals(String path, String flagValue, Consumer<Boolean> setter) {
    String value = str(path);
    if (value == null) return;
    setter.accept(flagValue.equals(value));
  }

  /** A doc-comment blank policy (one / none / ignore) into a minimum blank-line count. */
  private void applyCommentPolicy(String path, IntConsumer setter) {
    String value = str(path);
    if (value == null) return;
    setter.accept("one".equals(value) ? 1 : 0);
  }

  /**
   * Lifts an operator chain's thresholds from the matching rule shapes: an
   * onePerLineAfterFirst rule's itemCount, or its lineLength together with
   * anyItemLength, and a noWrap rule's totalItemLength guard. Other rule
   * shapes have no counterpart and are ignored.
   */
  private void applyChainRules(String path, ChainSetters setters) {
    if (node(path) == null) return;
    markConsumedSubtree(path);
    JsonNode rules = node(path + ".rules");
    if (rules == null || !rules.isArray()) return;
    for (JsonNode rule : rules) {
      String type = rule.path("type").asString("");
      if ("onePerLineAfterFirst".equals(type)) {
        Integer count = conditionValue(rule, "itemCount >= n");
        if (count != null) {
          setters.itemCount().accept(count);
        }
        Integer line = conditionValue(rule, "lineLength >= n");
        Integer item = conditionValue(rule, "anyItemLength >= n");
        if (line != null && item != null) {
          setters.lineLength().accept(line);
          setters.itemLength().accept(item);
        }
      }
      if ("noWrap".equals(type)) {
        Integer total = conditionValue(rule, "totalItemLength <= n");
        if (total != null) {
          setters.totalLength().accept(total);
        }
      }
    }
  }

  /** The four split thresholds of one operator chain kind. */
  private record ChainSetters(IntConsumer lineLength, IntConsumer itemLength, IntConsumer itemCount, IntConsumer totalLength) {
  }

  @Nullable
  private static Integer conditionValue(@NotNull JsonNode rule, @NotNull String cond) {
    JsonNode conditions = rule.get("conditions");
    if (conditions == null || !conditions.isArray()) return null;
    for (JsonNode condition : conditions) {
      if (cond.equals(condition.path("cond").asString(""))) {
        JsonNode value = condition.get("value");
        return value == null || !value.canConvertToInt() ? null : value.intValue();
      }
    }
    return null;
  }

  /** A sameLine.*Body policy (next / same / keep) into a per-construct body placement. */
  private void bodyPlacement(String path, IntConsumer setter) {
    String value = str(path);
    if (value == null) return;
    switch (value) {
      case "next" -> setter.accept(HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE);
      case "same" -> setter.accept(HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE);
      case "keep" -> setter.accept(HaxeCodeStyleSettings.BODY_PLACEMENT_KEEP);
      default -> unsupported.add(path + "=" + value);
    }
  }

  /** An emptyLines count, capped by maxAnywhereInFile. */
  private void applyBlankLineCount(String path, IntConsumer setter) {
    Integer value = intVal(path);
    if (value != null) {
      setter.accept(Math.min(value, blankLinesClamp));
    }
  }

  /** Reads the key and reports it when it holds anything but the supported value. */
  private void acceptOnly(String path, String supportedValue) {
    String value = str(path);
    if (value != null && !supportedValue.equals(value)) {
      unsupported.add(path + "=" + value);
    }
  }

  /** Consumes each present subtree whole and reports it under the bundle message taking the path. */
  private void reportUnsupportedSubtrees(List<String> paths, @PropertyKey(resourceBundle = "messages.HaxeCodeStyleBundle") String bundleKey) {
    for (String path : paths) {
      if (node(path) == null) continue;
      markConsumedSubtree(path);
      unsupported.add(HaxeCodeStyleBundle.message(bundleKey, path));
    }
  }

  private void markConsumedSubtree(String path) {
    JsonNode subtree = node(path);
    if (subtree != null) {
      markConsumed(subtree, path);
    }
  }

  private void markConsumed(JsonNode subtree, String path) {
    consumed.add(path);
    for (Map.Entry<String, JsonNode> field : subtree.properties()) {
      markConsumed(field.getValue(), path + "." + field.getKey());
    }
  }

  /** Reports every value that no mapping step read. */
  private void collectLeftovers(JsonNode subtree, String path) {
    if (subtree.isNull()) return;
    if (!subtree.isObject()) {
      if (!consumed.contains(path)) {
        unsupported.add(path);
      }
      return;
    }
    for (Map.Entry<String, JsonNode> field : subtree.properties()) {
      String childPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
      if (consumed.contains(childPath)) continue;
      collectLeftovers(field.getValue(), childPath);
    }
  }

  @Nullable
  private String str(String path) {
    JsonNode value = node(path);
    if (value == null || value.isObject() || value.isArray()) return null;
    consumed.add(path);
    return value.asString();
  }

  @Nullable
  private Integer intVal(String path) {
    JsonNode value = node(path);
    if (value == null || !value.canConvertToInt()) return null;
    consumed.add(path);
    return value.asInt();
  }

  /** The value at the dotted path; a JSON null (the tool's "unset" for per-construct overrides) counts as absent. */
  @Nullable
  private JsonNode node(String path) {
    JsonNode current = root;
    // the dotted config path's segments
    for (String part : path.split("\\.")) {
      current = current.get(part);
      if (current == null || current.isNull()) return null;
    }
    return current;
  }

  /** The pairs as an insertion-ordered map (Map.of iterates in a per-run random order). */
  private static Map<String, String> inSectionOrder(String... pathsAndValues) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < pathsAndValues.length; i += 2) {
      map.put(pathsAndValues[i], pathsAndValues[i + 1]);
    }
    return map;
  }
}
