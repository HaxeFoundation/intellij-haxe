/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.formatter.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCodeStyleSettings extends CustomCodeStyleSettings {
  // a project's own hxformat.json (haxe-formatter config) overrides these
  // settings per file while present, like EditorConfig support does
  public boolean USE_PROJECT_HXFORMAT = true;

  // the three arrow kinds space separately, as haxe-formatter does: arrow
  // functions (x -> x), Haxe 4 function types ((Int) -> Int) and Haxe 3
  // function types (Int -> Int; the hxformat profile leaves those unspaced)
  public boolean SPACE_AROUND_ARROW = true;
  public boolean SPACE_AROUND_FUNCTION_TYPE_ARROW = true;
  public boolean SPACE_AROUND_OLD_FUNCTION_TYPE_ARROW = true;
  public boolean SPACE_BEFORE_TYPE_REFERENCE_COLON = false;
  public boolean SPACE_AFTER_TYPE_REFERENCE_COLON = false;
  public boolean SPACE_WITHIN_TYPE_PARAMETERS = false;
  public boolean SPACE_WITHIN_STRING_INTERPOLATION = false;
  // the (expr : Type) type-check colon is conventionally spaced, UNLIKE type hints
  public boolean SPACE_AROUND_TYPE_CHECK_COLON = true;
  public boolean SPACE_BEFORE_OBJECT_FIELD_COLON = false;
  public boolean SPACE_AFTER_OBJECT_FIELD_COLON = true;
  public boolean SPACE_WITHIN_METADATA_PARENTHESES = false;

  // a structure extension stays on the brace's line in a one-line body
  // ({ > Base, ... }) and takes its own line in a multi-line one; off keeps
  // it as written
  public boolean STRUCTURE_EXTENSION_ON_OWN_LINE = false;
  public boolean FORMAT_DOC_COMMENTS = true;

  // reindents the inner lines of a plain /*..*/ comment the way hxformat
  // does: the common leading whitespace goes, the lines sit one level deeper
  // than the comment, and leading asterisks align. Off leaves comment
  // interiors alone, the IntelliJ convention. A first-column comment stays
  // untouched while KEEP_FIRST_COLUMN_COMMENT keeps its opener in place.
  public boolean REINDENT_MULTILINE_COMMENTS = true;

  // reformat also aligns inactive branches that do not parse cleanly
  // (branches that parse are formatted under FORMAT_INACTIVE_BRANCHES).
  // Off by default, because the lines of an unparsable branch shift as one
  // group, so statements nested inside it do not get their own indent steps
  public boolean ALIGN_INACTIVE_CONDITIONAL_BRANCHES = false;
  public boolean FORMAT_INACTIVE_BRANCHES = true;

  // a NAMED function's non-block body (function f() return x;) moves to its
  // own line; anonymous/arrow function bodies always stay inline
  public boolean FUNCTION_EXPRESSION_BODY_ON_NEXT_LINE = false;

  // a hand-broken "return\n value;" is re-joined; off keeps the break
  public boolean RETURN_VALUE_ON_SAME_LINE = false;

  // reformat turns "//text" into "// text" (hxformat's
  // whitespace.addLineCommentSpace); divider lines and "///" keep their shape
  public boolean ADD_LINE_COMMENT_SPACE = false;

  // where a control statement's NON-BLOCK body goes, per construct
  // (hxformat's sameLine.*Body): NEXT_LINE breaks it onto its own line,
  // SAME_LINE joins it onto the header's line, KEEP leaves it as written.
  // DEFAULT defers to the common "keep control statement in one line"
  // checkbox (checked = KEEP, unchecked = NEXT_LINE)
  public static final int BODY_PLACEMENT_DEFAULT = 0;
  public static final int BODY_PLACEMENT_NEXT_LINE = 1;
  public static final int BODY_PLACEMENT_SAME_LINE = 2;
  public static final int BODY_PLACEMENT_KEEP = 3;
  public int IF_BODY_PLACEMENT = BODY_PLACEMENT_DEFAULT;
  public int ELSE_BODY_PLACEMENT = BODY_PLACEMENT_DEFAULT;
  public int FOR_BODY_PLACEMENT = BODY_PLACEMENT_DEFAULT;
  public int WHILE_BODY_PLACEMENT = BODY_PLACEMENT_DEFAULT;
  public int DO_WHILE_BODY_PLACEMENT = BODY_PLACEMENT_DEFAULT;
  public int TRY_BODY_PLACEMENT = BODY_PLACEMENT_DEFAULT;
  public int CATCH_BODY_PLACEMENT = BODY_PLACEMENT_DEFAULT;
  // a case body (hxformat's sameLine.caseBody) has no common checkbox, so
  // its default keeps the written shape
  public int CASE_BODY_PLACEMENT = BODY_PLACEMENT_KEEP;
  // the parts of an if/try used as a VALUE (var x = if (c) a else b;) and
  // the case bodies of a value switch (hxformat's sameLine.expressionIf,
  // expressionTry, expressionCase). SAME_LINE joins condition, bodies and
  // keywords onto one line. KEEP breaks exactly where the source did, so an
  // else or catch written on its own line stays there. NEXT_LINE applies
  // the statement placements above
  public int VALUE_IF_BODY_PLACEMENT = BODY_PLACEMENT_KEEP;
  public int VALUE_TRY_BODY_PLACEMENT = BODY_PLACEMENT_KEEP;
  public int VALUE_CASE_BODY_PLACEMENT = BODY_PLACEMENT_KEEP;

  // counts BLANK LINES (like the platform's BLANK_LINES_* options)
  public int MINIMUM_BLANK_LINES_AFTER_USING = 1;
  // gap after a block comment that OPENS the file (a license header);
  // 0 keeps whatever was written
  public int MINIMUM_BLANK_LINES_AFTER_FILE_HEADER = 0;
  // the most blank lines kept between two type declarations, independent of
  // the in-code maximum; the common BLANK_LINES_AROUND_CLASS is the minimum
  public int KEEP_BLANK_LINES_BETWEEN_TYPES = 2;
  // the most blank lines kept between ADJACENT one-line type declarations
  // (interface One {}); 0 removes them, as hxformat does
  public int KEEP_BLANK_LINES_BETWEEN_SINGLE_LINE_TYPES = 2;
  // the most blank lines kept WITHIN the import/using section; 0 makes it one solid block
  public int KEEP_BLANK_LINES_BETWEEN_IMPORTS = 2;
  // the most blank lines kept directly after a block's '{' (class bodies use
  // the exact BLANK_LINES_AFTER_CLASS_HEADER count instead); 0 puts the
  // first statement right under the brace, as hxformat does
  public int KEEP_BLANK_LINES_AFTER_LBRACE = 2;
  // the most blank lines kept between a case's ':' and its first statement
  // (hxformat's emptyLines.beforeBlocks); blank lines BETWEEN cases follow
  // the in-code maximum
  public int KEEP_BLANK_LINES_AFTER_CASE_COLON = 2;
  // the most blank lines kept between two block comments stacked on their
  // own lines (hxformat's emptyLines.betweenMultilineComments); 0 removes them
  public int KEEP_BLANK_LINES_BETWEEN_MULTILINE_COMMENTS = 2;
  // blank lines where consecutive vars change group, by staticness or by
  // visibility (hxformat's classEmptyLines.afterStaticVars and
  // afterPrivateVars); 0 keeps the plain BLANK_LINES_AROUND_FIELD gap
  public int BLANK_LINES_BETWEEN_FIELD_GROUPS = 0;
  // minimum blank lines before a FIELD's doc comment (hxformat's
  // beforeDocCommentEmptyLines) and after a documented field
  // (afterFieldsWithDocComments); 0 keeps the written shape
  public int BLANK_LINES_BEFORE_FIELD_DOC_COMMENT = 0;
  public int BLANK_LINES_AFTER_DOCUMENTED_FIELD = 0;
  // wrapped operator chains (&&/||, +/-) continue ONE step from the line
  // the chain starts on, the way haxe-formatter indents its wraps; off
  // keeps the platform's alignment-based continuation
  public boolean INDENT_WRAPPED_OPERATOR_CHAINS = false;
  // &&/|| and +/- chains follow hxformat's wrapping.opBoolChain and
  // opAddSubChain rules (see HaxeOperatorChainRules). When the joined line
  // reaches SPLIT_LINE_LENGTH, the chain breaks before every operator if an
  // operand reaches SPLIT_ITEM_LENGTH, and otherwise only where it
  // overflows. A fitting line breaks before every operator once
  // SPLIT_ITEM_COUNT operands total more than SPLIT_TOTAL_LENGTH. A
  // threshold of 0 removes its rule; all four at 0 keep the written shape
  public int BOOL_CHAIN_SPLIT_LINE_LENGTH = 0;
  public int BOOL_CHAIN_SPLIT_ITEM_LENGTH = 0;
  public int BOOL_CHAIN_SPLIT_ITEM_COUNT = 0;
  public int BOOL_CHAIN_SPLIT_TOTAL_LENGTH = 0;
  public int ADD_CHAIN_SPLIT_LINE_LENGTH = 0;
  public int ADD_CHAIN_SPLIT_ITEM_LENGTH = 0;
  public int ADD_CHAIN_SPLIT_ITEM_COUNT = 0;
  public int ADD_CHAIN_SPLIT_TOTAL_LENGTH = 0;
  // call arguments and declared parameters fill the JOINED line the way
  // hxformat's callParameter/functionSignature fillLine does. Written breaks
  // in the list and around its parens are removed, and an argument that
  // reaches the margin on the joined line starts a new line. Off keeps
  // written breaks and leaves wrapping at the margin to the platform
  public boolean FILL_CALL_ARGUMENTS_ON_JOINED_LINE = false;
  // a multi-var declaration whose JOINED line reaches this many columns
  // splits one declarator per line (hxformat's wrapping.multiVar lineLength
  // rule); 0 keeps the written shape. A declarator of at most
  // FILL_ITEM_LENGTH keeps the list filling instead, because the tool's
  // anyItemLength rule comes first
  public int MULTI_VAR_SPLIT_WIDTH = 0;
  public int MULTI_VAR_FILL_ITEM_LENGTH = 0;
  // array and map literals follow hxformat's wrapping.arrayWrap / mapWrap
  // rules (see HaxeLiteralItemRules). Items totalling at most
  // KEEP_TOTAL_LENGTH stay on one line. The items fill the line after a
  // leading break when there are FILL_EQUAL_ITEM_COUNT or more of one length
  // up to FILL_EQUAL_ITEM_LENGTH, or FILL_ITEM_COUNT or more of at most
  // FILL_ITEM_LENGTH each. An item reaching CHOP_ITEM_LENGTH, or
  // CHOP_ITEM_COUNT items or more, go one per line. A threshold of 0 removes
  // its rule; with all seven of a kind at 0, the array wrap setting alone
  // decides that kind's literals
  public int ARRAY_KEEP_TOTAL_LENGTH = 0;
  public int ARRAY_FILL_EQUAL_ITEM_LENGTH = 0;
  public int ARRAY_FILL_EQUAL_ITEM_COUNT = 0;
  public int ARRAY_FILL_ITEM_LENGTH = 0;
  public int ARRAY_FILL_ITEM_COUNT = 0;
  public int ARRAY_CHOP_ITEM_LENGTH = 0;
  public int ARRAY_CHOP_ITEM_COUNT = 0;
  public int MAP_KEEP_TOTAL_LENGTH = 0;
  public int MAP_FILL_EQUAL_ITEM_LENGTH = 0;
  public int MAP_FILL_EQUAL_ITEM_COUNT = 0;
  public int MAP_FILL_ITEM_LENGTH = 0;
  public int MAP_FILL_ITEM_COUNT = 0;
  public int MAP_CHOP_ITEM_LENGTH = 0;
  public int MAP_CHOP_ITEM_COUNT = 0;
  // object literals follow hxformat's wrapping.objectLiteral rules. An
  // object written over several lines goes one field per line. Up to
  // KEEP_ITEM_COUNT fields on a line within the margin stay. A field
  // reaching CHOP_ITEM_LENGTH, fields totalling CHOP_TOTAL_LENGTH or more,
  // or CHOP_ITEM_COUNT fields or more go one per line. A threshold of 0
  // removes its rule; with all four at 0, the array wrap setting alone
  // decides object literals
  public int OBJECT_KEEP_ITEM_COUNT = 0;
  public int OBJECT_CHOP_ITEM_LENGTH = 0;
  public int OBJECT_CHOP_TOTAL_LENGTH = 0;
  public int OBJECT_CHOP_ITEM_COUNT = 0;
  // 0 turns grouping off, and KEEP_BLANK_LINES_BETWEEN_IMPORTS applies.
  // Above 0, imports that differ in their first IMPORT_GROUP_PACKAGE_DEPTH
  // package segments get exactly this many blank lines between them, and
  // imports of one group get none
  public int BLANK_LINES_BETWEEN_IMPORT_GROUPS = 0;
  public int IMPORT_GROUP_PACKAGE_DEPTH = 1;

  protected HaxeCodeStyleSettings(CodeStyleSettings container) {
    super("HaxeCodeStyleSettings", container);
  }
}
