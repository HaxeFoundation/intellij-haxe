package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * The complete settings that a DEFAULT hxformat.json (haxe-formatter 1.18)
 * stands for. The per-file override ({@link HxformatSettingsModifier}) and
 * the scheme importer install them first and then apply the config's own
 * keys on top ({@link HxformatJsonMapper}). There is one method per config
 * section, in the mapper's order, so each section reads side by side with
 * its mapping.
 */
public final class HxformatDefaultProfile {

  /** The settings UI's encoding of "chop down if long". */
  static final int UI_CHOP_DOWN =
    CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM | CommonCodeStyleSettings.WRAP_AS_NEEDED;

  // every wrap policy of the common settings, reset before the profile is applied
  private static final List<Field> WRAP_FIELDS = Arrays.stream(CommonCodeStyleSettings.class.getFields())
    .filter(field -> field.getType() == int.class && field.getName().endsWith("_WRAP"))
    .toList();

  private HxformatDefaultProfile() {
  }

  public static void apply(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings common = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    resetWrapFields(common);
    applyIndentation(settings, common, haxe);
    applyWrapping(settings, common, haxe);
    applyLineEnds(common);
    applySameLine(common, haxe);
    applyWhitespace(common, haxe);
    applyEmptyLines(common, haxe);
  }

  private static void applyIndentation(CodeStyleSettings settings, CommonCodeStyleSettings common, HaxeCodeStyleSettings haxe) {
    CodeStyleSettings.IndentOptions indent = settings.getIndentOptions(HaxeFileType.INSTANCE);
    // indentation.character="tab", tabWidth
    indent.USE_TAB_CHARACTER = true;
    indent.TAB_SIZE = HxformatDefaults.TAB_WIDTH;
    indent.INDENT_SIZE = HxformatDefaults.TAB_WIDTH;
    // a wrapped declaration header (implementsExtends) continues TWO steps in
    indent.CONTINUATION_INDENT_SIZE = HxformatDefaults.CONTINUATION_STEPS * HxformatDefaults.TAB_WIDTH;
    // indentation.conditionalPolicy=Aligned, applied to unparsable inactive branches too
    haxe.ALIGN_INACTIVE_CONDITIONAL_BRANCHES = true;
    // haxe-formatter indents every comment to its scope, first-column ones
    // included, and always reindents the inner lines of plain /*..*/ comments
    common.KEEP_FIRST_COLUMN_COMMENT = false;
    haxe.REINDENT_MULTILINE_COMMENTS = true;
  }

  private static void applyWrapping(CodeStyleSettings settings, CommonCodeStyleSettings common, HaxeCodeStyleSettings haxe) {
    // wrapping.maxLineLength
    settings.setRightMargin(HaxeLanguage.INSTANCE, HxformatDefaults.MAX_LINE_LENGTH);
    // wrapping.arrayWrap/mapWrap/objectLiteral/methodChain: one item per line
    // when the line exceeds maxLineLength. The settings UI stores "chop down
    // if long" as WRAP_ON_EVERY_ITEM | WRAP_AS_NEEDED; a bare
    // WRAP_ON_EVERY_ITEM shows as "invalid option value" in the combo box
    common.ARRAY_INITIALIZER_WRAP = UI_CHOP_DOWN;
    common.METHOD_CALL_CHAIN_WRAP = UI_CHOP_DOWN;
    // wrapping.implementsExtends: FillLine, which breaks only past maxLineLength
    common.EXTENDS_LIST_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    // wrapping.functionSignature: FillLine. The parameters re-pack up to the
    // margin whatever breaks were written (see the joined-line fill below),
    // and a line past the margin breaks where the tool would break it
    common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    // wrapping.callParameter: NoWrap below its item-count and length
    // thresholds, FillLine past the margin, so a line joined by a second
    // reformat breaks again where it overflows. The operator chains use their
    // own split rules instead: a margin wrap on them would let an operand's
    // break win over a chopped method chain
    common.CALL_PARAMETERS_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
    // haxe-formatter indents wrapped parameters and arguments (one step for
    // arguments, two for a signature); it never aligns them under the first
    common.ALIGN_MULTILINE_PARAMETERS = false;
    common.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = false;
    // haxe-formatter indents every wrapped operator chain one step from
    // the chain's line (no operand alignment)
    haxe.INDENT_WRAPPED_OPERATOR_CHAINS = true;
    // wrapping.opBoolChain / opAddSubChain rule thresholds
    haxe.BOOL_CHAIN_SPLIT_LINE_LENGTH = HxformatDefaults.BOOL_CHAIN_LINE_LENGTH;
    haxe.BOOL_CHAIN_SPLIT_ITEM_LENGTH = HxformatDefaults.BOOL_CHAIN_ITEM_LENGTH;
    haxe.BOOL_CHAIN_SPLIT_ITEM_COUNT = HxformatDefaults.BOOL_CHAIN_ITEM_COUNT;
    haxe.BOOL_CHAIN_SPLIT_TOTAL_LENGTH = HxformatDefaults.BOOL_CHAIN_TOTAL_LENGTH;
    haxe.ADD_CHAIN_SPLIT_LINE_LENGTH = HxformatDefaults.ADD_CHAIN_LINE_LENGTH;
    haxe.ADD_CHAIN_SPLIT_ITEM_LENGTH = HxformatDefaults.ADD_CHAIN_ITEM_LENGTH;
    haxe.ADD_CHAIN_SPLIT_ITEM_COUNT = HxformatDefaults.ADD_CHAIN_ITEM_COUNT;
    haxe.ADD_CHAIN_SPLIT_TOTAL_LENGTH = HxformatDefaults.ADD_CHAIN_TOTAL_LENGTH;
    // wrapping.callParameter/functionSignature/anonFunctionSignature fillLine
    // re-packs parameters and arguments on the JOINED line: written breaks
    // in a list and around its parens are removed, and only the fill's own
    // breaks remain
    haxe.FILL_CALL_ARGUMENTS_ON_JOINED_LINE = true;
    // wrapping.multiVar: lineLength -> onePerLineAfterFirst, preceded by
    // anyItemLength -> fillLine (the length-based JOIN of short multi-vars
    // is not reproduced)
    haxe.MULTI_VAR_SPLIT_WIDTH = HxformatDefaults.MULTI_VAR_LINE_LENGTH;
    haxe.MULTI_VAR_FILL_ITEM_LENGTH = HxformatDefaults.MULTI_VAR_FILL_ITEM_LENGTH;
    // wrapping.arrayWrap rule thresholds
    haxe.ARRAY_KEEP_TOTAL_LENGTH = HxformatDefaults.ARRAY_KEEP_TOTAL_LENGTH;
    haxe.ARRAY_FILL_EQUAL_ITEM_LENGTH = HxformatDefaults.ARRAY_FILL_EQUAL_ITEM_LENGTH;
    haxe.ARRAY_FILL_EQUAL_ITEM_COUNT = HxformatDefaults.ARRAY_FILL_EQUAL_ITEM_COUNT;
    haxe.ARRAY_FILL_ITEM_LENGTH = HxformatDefaults.ARRAY_FILL_ITEM_LENGTH;
    haxe.ARRAY_FILL_ITEM_COUNT = HxformatDefaults.ARRAY_FILL_ITEM_COUNT;
    haxe.ARRAY_CHOP_ITEM_LENGTH = HxformatDefaults.ARRAY_CHOP_ITEM_LENGTH;
    haxe.ARRAY_CHOP_ITEM_COUNT = HxformatDefaults.ARRAY_CHOP_ITEM_COUNT;
    // wrapping.mapWrap rule thresholds
    haxe.MAP_KEEP_TOTAL_LENGTH = HxformatDefaults.MAP_KEEP_TOTAL_LENGTH;
    haxe.MAP_FILL_EQUAL_ITEM_LENGTH = HxformatDefaults.MAP_FILL_EQUAL_ITEM_LENGTH;
    haxe.MAP_FILL_EQUAL_ITEM_COUNT = HxformatDefaults.MAP_FILL_EQUAL_ITEM_COUNT;
    haxe.MAP_FILL_ITEM_LENGTH = HxformatDefaults.MAP_FILL_ITEM_LENGTH;
    haxe.MAP_FILL_ITEM_COUNT = HxformatDefaults.MAP_FILL_ITEM_COUNT;
    haxe.MAP_CHOP_ITEM_LENGTH = HxformatDefaults.MAP_CHOP_ITEM_LENGTH;
    haxe.MAP_CHOP_ITEM_COUNT = HxformatDefaults.MAP_CHOP_ITEM_COUNT;
    // wrapping.objectLiteral rule thresholds
    haxe.OBJECT_KEEP_ITEM_COUNT = HxformatDefaults.OBJECT_KEEP_ITEM_COUNT;
    haxe.OBJECT_CHOP_ITEM_LENGTH = HxformatDefaults.OBJECT_CHOP_ITEM_LENGTH;
    haxe.OBJECT_CHOP_TOTAL_LENGTH = HxformatDefaults.OBJECT_CHOP_TOTAL_LENGTH;
    haxe.OBJECT_CHOP_ITEM_COUNT = HxformatDefaults.OBJECT_CHOP_ITEM_COUNT;
  }

  private static void applyLineEnds(CommonCodeStyleSettings common) {
    // lineEnds.leftCurly=After / rightCurly=Both
    common.BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE;
    common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.END_OF_LINE;
    // lineEnds.emptyCurly=NoBreak ({} collapses)
    common.KEEP_SIMPLE_BLOCKS_IN_ONE_LINE = true;
    common.KEEP_SIMPLE_METHODS_IN_ONE_LINE = true;
    common.KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE = true;
  }

  private static void applySameLine(CommonCodeStyleSettings common, HaxeCodeStyleSettings haxe) {
    // sameLine.ifElse/elseIf/doWhile/tryCatch=Same
    common.ELSE_ON_NEW_LINE = false;
    common.WHILE_ON_NEW_LINE = false;
    common.CATCH_ON_NEW_LINE = false;
    common.SPECIAL_ELSE_IF_TREATMENT = true;
    // sameLine.ifBody/elseBody/forBody/whileBody/doWhileBody/tryBody/
    // catchBody=Next: every non-block statement body breaks onto its own
    // line. The per-construct placements carry the policy; the common flag
    // only mirrors it for the settings UI
    common.KEEP_CONTROL_STATEMENT_IN_ONE_LINE = false;
    haxe.IF_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    haxe.ELSE_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    haxe.FOR_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    haxe.WHILE_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    haxe.DO_WHILE_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    haxe.TRY_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    haxe.CATCH_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    // sameLine.caseBody=Next
    haxe.CASE_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    // sameLine.expressionIf/expressionTry=Same, expressionCase=Keep
    haxe.VALUE_IF_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE;
    haxe.VALUE_TRY_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE;
    haxe.VALUE_CASE_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_KEEP;
    // sameLine.functionBody=Next; anonFunctionBody=Same needs no flag, since
    // anonymous function bodies always stay inline
    haxe.FUNCTION_EXPRESSION_BODY_ON_NEXT_LINE = true;
    // sameLine.returnBodySingleLine: a return broken before its value is re-joined
    haxe.RETURN_VALUE_ON_SAME_LINE = true;
  }

  private static void applyWhitespace(CommonCodeStyleSettings common, HaxeCodeStyleSettings haxe) {
    // whitespace keyword policies (After) and paren policies (none within)
    common.SPACE_BEFORE_IF_PARENTHESES = true;
    common.SPACE_BEFORE_WHILE_PARENTHESES = true;
    common.SPACE_BEFORE_FOR_PARENTHESES = true;
    common.SPACE_BEFORE_SWITCH_PARENTHESES = true;
    common.SPACE_BEFORE_CATCH_PARENTHESES = true;
    common.SPACE_BEFORE_METHOD_PARENTHESES = false;
    common.SPACE_BEFORE_METHOD_CALL_PARENTHESES = false;
    common.SPACE_WITHIN_METHOD_CALL_PARENTHESES = false;
    common.SPACE_WITHIN_METHOD_PARENTHESES = false;
    common.SPACE_WITHIN_IF_PARENTHESES = false;
    common.SPACE_WITHIN_WHILE_PARENTHESES = false;
    common.SPACE_WITHIN_FOR_PARENTHESES = false;
    common.SPACE_WITHIN_SWITCH_PARENTHESES = false;
    common.SPACE_WITHIN_CATCH_PARENTHESES = false;
    common.SPACE_WITHIN_PARENTHESES = false;
    common.SPACE_WITHIN_BRACKETS = false;
    // whitespace.parenConfig.metadataParens=NoSpace
    haxe.SPACE_WITHIN_METADATA_PARENTHESES = false;
    // whitespace.binopPolicy=Around (one flag per operator class here)
    common.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
    common.SPACE_AROUND_LOGICAL_OPERATORS = true;
    common.SPACE_AROUND_EQUALITY_OPERATORS = true;
    common.SPACE_AROUND_RELATIONAL_OPERATORS = true;
    common.SPACE_AROUND_ADDITIVE_OPERATORS = true;
    common.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = true;
    common.SPACE_AROUND_BITWISE_OPERATORS = true;
    common.SPACE_AROUND_SHIFT_OPERATORS = true;
    // whitespace.ternaryPolicy=Around
    common.SPACE_BEFORE_QUEST = true;
    common.SPACE_AFTER_QUEST = true;
    common.SPACE_BEFORE_COLON = true;
    common.SPACE_AFTER_COLON = true;
    // whitespace.commaPolicy=OnlyAfter
    common.SPACE_BEFORE_COMMA = false;
    common.SPACE_AFTER_COMMA = true;
    common.SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS = true;
    // whitespace.bracesConfig openingPolicy=Before
    common.SPACE_BEFORE_METHOD_LBRACE = true;
    common.SPACE_BEFORE_IF_LBRACE = true;
    common.SPACE_BEFORE_ELSE_LBRACE = true;
    common.SPACE_BEFORE_DO_LBRACE = true;
    common.SPACE_BEFORE_WHILE_LBRACE = true;
    common.SPACE_BEFORE_FOR_LBRACE = true;
    common.SPACE_BEFORE_SWITCH_LBRACE = true;
    common.SPACE_BEFORE_TRY_LBRACE = true;
    common.SPACE_BEFORE_CATCH_LBRACE = true;
    common.SPACE_BEFORE_ELSE_KEYWORD = true;
    common.SPACE_BEFORE_WHILE_KEYWORD = true;
    common.SPACE_BEFORE_CATCH_KEYWORD = true;
    // whitespace.arrowFunctionsPolicy/functionTypeHaxe4Policy=Around,
    // functionTypeHaxe3Policy=None
    haxe.SPACE_AROUND_ARROW = true;
    haxe.SPACE_AROUND_FUNCTION_TYPE_ARROW = true;
    haxe.SPACE_AROUND_OLD_FUNCTION_TYPE_ARROW = false;
    // whitespace.typeHintColonPolicy=None
    haxe.SPACE_BEFORE_TYPE_REFERENCE_COLON = false;
    haxe.SPACE_AFTER_TYPE_REFERENCE_COLON = false;
    // whitespace.typeParamOpenPolicy/typeParamClosePolicy=None
    haxe.SPACE_WITHIN_TYPE_PARAMETERS = false;
    // whitespace.typeCheckColonPolicy=Around
    haxe.SPACE_AROUND_TYPE_CHECK_COLON = true;
    // whitespace.objectFieldColonPolicy=After
    haxe.SPACE_BEFORE_OBJECT_FIELD_COLON = false;
    haxe.SPACE_AFTER_OBJECT_FIELD_COLON = true;
    // whitespace.formatStringInterpolation=true
    haxe.SPACE_WITHIN_STRING_INTERPOLATION = false;
    // whitespace.typeExtensionPolicy=After
    haxe.STRUCTURE_EXTENSION_ON_OWN_LINE = true;
    // whitespace.addLineCommentSpace=true: "//text" becomes "// text"
    haxe.ADD_LINE_COMMENT_SPACE = true;
  }

  private static void applyEmptyLines(CommonCodeStyleSettings common, HaxeCodeStyleSettings haxe) {
    // emptyLines (the tool's defaults, see HxformatDefaults)
    common.KEEP_LINE_BREAKS = true;
    common.KEEP_BLANK_LINES_IN_CODE = HxformatDefaults.MAX_BLANK_LINES;
    common.KEEP_BLANK_LINES_IN_DECLARATIONS = HxformatDefaults.MAX_BLANK_LINES;
    common.KEEP_BLANK_LINES_BEFORE_RBRACE = HxformatDefaults.BLANK_LINES_AT_BLOCK_EDGES;
    common.BLANK_LINES_AFTER_PACKAGE = HxformatDefaults.BLANK_LINES_AFTER_PACKAGE;
    common.BLANK_LINES_AFTER_IMPORTS = HxformatDefaults.BLANK_LINES_AFTER_IMPORTS;
    // betweenTypes is an exact count: the same value bounds the gap both ways
    common.BLANK_LINES_AROUND_CLASS = HxformatDefaults.BLANK_LINES_BETWEEN_TYPES;
    haxe.KEEP_BLANK_LINES_BETWEEN_TYPES = HxformatDefaults.BLANK_LINES_BETWEEN_TYPES;
    common.BLANK_LINES_AFTER_CLASS_HEADER = HxformatDefaults.BLANK_LINES_BEGIN_TYPE;
    common.BLANK_LINES_AROUND_FIELD = HxformatDefaults.BLANK_LINES_BETWEEN_VARS;
    common.BLANK_LINES_AROUND_METHOD = HxformatDefaults.BLANK_LINES_BETWEEN_FUNCTIONS;
    common.BLANK_LINES_BEFORE_CLASS_END = HxformatDefaults.BLANK_LINES_END_TYPE;
    // classEmptyLines.afterStaticVars/afterPrivateVars: a change of
    // staticness or visibility splits the var block
    haxe.BLANK_LINES_BETWEEN_FIELD_GROUPS = HxformatDefaults.BLANK_LINES_BETWEEN_VAR_GROUPS;
    // beforeDocCommentEmptyLines/afterFieldsWithDocComments: a blank line
    // separates a documented field from both neighbors
    haxe.BLANK_LINES_BEFORE_FIELD_DOC_COMMENT = HxformatDefaults.BLANK_LINES_AROUND_DOCUMENTED_FIELD;
    haxe.BLANK_LINES_AFTER_DOCUMENTED_FIELD = HxformatDefaults.BLANK_LINES_AROUND_DOCUMENTED_FIELD;
    // emptyLines.afterLeftCurly=Remove, beforeBlocks=Remove (the case-body edge)
    haxe.KEEP_BLANK_LINES_AFTER_LBRACE = HxformatDefaults.BLANK_LINES_AT_BLOCK_EDGES;
    haxe.KEEP_BLANK_LINES_AFTER_CASE_COLON = HxformatDefaults.BLANK_LINES_AT_BLOCK_EDGES;
    // emptyLines.betweenMultilineComments
    haxe.KEEP_BLANK_LINES_BETWEEN_MULTILINE_COMMENTS = HxformatDefaults.BLANK_LINES_BETWEEN_MULTILINE_COMMENTS;
    // emptyLines.importAndUsing.beforeType, betweenSingleLineTypes,
    // importAndUsing.betweenImports, afterFileHeaderComment
    haxe.MINIMUM_BLANK_LINES_AFTER_USING = HxformatDefaults.BLANK_LINES_AFTER_IMPORTS;
    haxe.KEEP_BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES = HxformatDefaults.BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES;
    haxe.KEEP_BLANK_LINES_BETWEEN_IMPORTS = HxformatDefaults.BLANK_LINES_BETWEEN_IMPORTS;
    haxe.MINIMUM_BLANK_LINES_AFTER_FILE_HEADER = HxformatDefaults.BLANK_LINES_AFTER_FILE_HEADER;
    haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS = 0;
    haxe.IMPORT_GROUP_PACKAGE_DEPTH = 1;
  }

  /**
   * Resets every wrap policy to DO_NOT_WRAP. A scheme created in the
   * settings UI CLONES the selected scheme, so wrap fields the profile does
   * not set would otherwise inherit arbitrary, possibly invalid, values. The
   * reset also repairs a legacy bare WRAP_ON_EVERY_ITEM, which the settings
   * combo boxes reject ("chop down if long" is stored as
   * WRAP_ON_EVERY_ITEM | WRAP_AS_NEEDED).
   */
  private static void resetWrapFields(CommonCodeStyleSettings common) {
    for (Field field : WRAP_FIELDS) {
      try {
        field.setInt(common, CommonCodeStyleSettings.DO_NOT_WRAP);
      }
      catch (IllegalAccessException ignored) {
      }
    }
  }
}
