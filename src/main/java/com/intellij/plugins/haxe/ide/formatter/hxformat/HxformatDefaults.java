package com.intellij.plugins.haxe.ide.formatter.hxformat;

import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;

/**
 * haxe-formatter's built-in defaults (formatter 1.18.0). They make up the
 * profile that {@link HxformatDefaultProfile#apply} installs, which an
 * imported hxformat.json then overrides. They apply only under that profile;
 * the plain scheme keeps the plugin's own defaults in
 * {@link HaxeCodeStyleSettings}.
 * <p>
 * The tool prints its complete default configuration with
 * {@code haxelib run formatter --default-config <file>}, where the file must
 * already exist. {@code HxformatDefaultsTest} checks these values against
 * that output.
 */
public final class HxformatDefaults {

  // indentation: character="tab", tabWidth=4; a wrapped declaration header continues two steps in
  public static final int TAB_WIDTH = 4;
  public static final int CONTINUATION_STEPS = 2;

  // wrapping.maxLineLength
  public static final int MAX_LINE_LENGTH = 160;

  // wrapping.opBoolChain rules: lineLength >= 140 (+ anyItemLength >= 40 -> one per line, else fill);
  // itemCount >= 4 -> one per line unless totalItemLength <= 120. The fixed "up to 3 operands on a
  // fitting line" rule is HaxeOperatorChainRules.KEEP_ITEM_COUNT
  public static final int BOOL_CHAIN_LINE_LENGTH = 140;
  public static final int BOOL_CHAIN_ITEM_LENGTH = 40;
  public static final int BOOL_CHAIN_ITEM_COUNT = 4;
  public static final int BOOL_CHAIN_TOTAL_LENGTH = 120;

  // wrapping.opAddSubChain rules, the same shape
  public static final int ADD_CHAIN_LINE_LENGTH = 160;
  public static final int ADD_CHAIN_ITEM_LENGTH = 60;
  public static final int ADD_CHAIN_ITEM_COUNT = 4;
  public static final int ADD_CHAIN_TOTAL_LENGTH = 120;

  // wrapping.multiVar rules: lineLength >= 80 -> one per line, unless anyItemLength <= 15 -> fill
  public static final int MULTI_VAR_LINE_LENGTH = 80;
  public static final int MULTI_VAR_FILL_ITEM_LENGTH = 15;

  // wrapping.arrayWrap rules: totalItemLength <= 80 -> no wrap; equalItemLengths + allItemLengths <= 30
  // + itemCount >= 10, or allItemLengths <= 10 + itemCount >= 10 -> fill after a leading break;
  // anyItemLength >= 30 or itemCount >= 4 -> one per line
  public static final int ARRAY_KEEP_TOTAL_LENGTH = 80;
  public static final int ARRAY_FILL_EQUAL_ITEM_LENGTH = 30;
  public static final int ARRAY_FILL_EQUAL_ITEM_COUNT = 10;
  public static final int ARRAY_FILL_ITEM_LENGTH = 10;
  public static final int ARRAY_FILL_ITEM_COUNT = 10;
  public static final int ARRAY_CHOP_ITEM_LENGTH = 30;
  public static final int ARRAY_CHOP_ITEM_COUNT = 4;

  // wrapping.mapWrap rules: the same list as arrayWrap
  public static final int MAP_KEEP_TOTAL_LENGTH = 80;
  public static final int MAP_FILL_EQUAL_ITEM_LENGTH = 30;
  public static final int MAP_FILL_EQUAL_ITEM_COUNT = 10;
  public static final int MAP_FILL_ITEM_LENGTH = 10;
  public static final int MAP_FILL_ITEM_COUNT = 10;
  public static final int MAP_CHOP_ITEM_LENGTH = 30;
  public static final int MAP_CHOP_ITEM_COUNT = 4;

  // wrapping.objectLiteral rules: itemCount <= 3 within the margin -> no wrap; anyItemLength >= 30,
  // totalItemLength >= 60 or itemCount >= 4 -> one per line
  public static final int OBJECT_KEEP_ITEM_COUNT = 3;
  public static final int OBJECT_CHOP_ITEM_LENGTH = 30;
  public static final int OBJECT_CHOP_TOTAL_LENGTH = 60;
  public static final int OBJECT_CHOP_ITEM_COUNT = 4;

  // emptyLines: maxAnywhereInFile, afterPackage, importAndUsing.beforeType, betweenTypes,
  // betweenSingleLineTypes, importAndUsing.betweenImports, afterFileHeaderComment
  public static final int MAX_BLANK_LINES = 1;
  public static final int BLANK_LINES_AFTER_PACKAGE = 1;
  public static final int BLANK_LINES_AFTER_IMPORTS = 1;
  public static final int BLANK_LINES_BETWEEN_TYPES = 1;
  public static final int BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES = 0;
  public static final int BLANK_LINES_BETWEEN_IMPORTS = 0;
  public static final int BLANK_LINES_AFTER_FILE_HEADER = 1;

  // emptyLines.classEmptyLines: beginType, endType, betweenVars, betweenFunctions,
  // afterStaticVars/afterPrivateVars; beforeDocCommentEmptyLines/afterFieldsWithDocComments=One
  public static final int BLANK_LINES_BEGIN_TYPE = 0;
  public static final int BLANK_LINES_END_TYPE = 0;
  public static final int BLANK_LINES_BETWEEN_VARS = 0;
  public static final int BLANK_LINES_BETWEEN_FUNCTIONS = 1;
  public static final int BLANK_LINES_BETWEEN_VAR_GROUPS = 1;
  public static final int BLANK_LINES_AROUND_DOCUMENTED_FIELD = 1;

  // emptyLines.afterLeftCurly / beforeRightCurly / beforeBlocks = Remove
  public static final int BLANK_LINES_AT_BLOCK_EDGES = 0;

  // emptyLines.betweenMultilineComments
  public static final int BLANK_LINES_BETWEEN_MULTILINE_COMMENTS = 0;

  private HxformatDefaults() {
  }
}
