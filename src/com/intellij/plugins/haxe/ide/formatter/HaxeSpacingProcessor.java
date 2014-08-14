/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSpacingProcessor {
  private final ASTNode myNode;
  private final CommonCodeStyleSettings mySettings;
  private final HaxeCodeStyleSettings myHaxeCodeStyleSettings;

  public HaxeSpacingProcessor(ASTNode node, CommonCodeStyleSettings settings, HaxeCodeStyleSettings haxeCodeStyleSettings) {
    myNode = node;
    mySettings = settings;
    myHaxeCodeStyleSettings = haxeCodeStyleSettings;
  }

  public Spacing getSpacing(Block child1, Block child2) {
    if (!(child1 instanceof AbstractBlock) || !(child2 instanceof AbstractBlock)) {
      return null;
    }

    final IElementType elementType = myNode.getElementType();
    final IElementType parentType = myNode.getTreeParent() == null ? null : myNode.getTreeParent().getElementType();
    final ASTNode node1 = ((AbstractBlock)child1).getNode();
    final IElementType type1 = node1.getElementType();
    final ASTNode node2 = ((AbstractBlock)child2).getNode();
    final IElementType type2 = node2.getElementType();
    final ASTNode nodeNode1 = node1 == null ? null : node1.getFirstChildNode();
    final IElementType typeType1 = nodeNode1 == null ? null : nodeNode1.getElementType();
    final ASTNode nodeNode2 = node2 == null ? null : node2.getFirstChildNode();
    final IElementType typeType2 = nodeNode2 == null ? null : nodeNode2.getElementType();

    if (type1 == IMPORT_STATEMENT ||
        type1 == PACKAGE_STATEMENT ||
        type1 == USING_STATEMENT) {
      return addSingleSpaceIf(false, true);
    }

    if (type1 == CLASS_BODY || type1 == EXTERN_CLASS_DECLARATION_BODY || type1 == ENUM_BODY || type1 == INTERFACE_BODY) {
      return Spacing.createSpacing(0, 0, 1, false, mySettings.KEEP_BLANK_LINES_IN_CODE);
    }

    if (type2 == FUNCTION_DECLARATION_WITH_ATTRIBUTES) {
      return Spacing.createSpacing(0, 0, 2, false, mySettings.KEEP_BLANK_LINES_IN_CODE);
    }

    if (type2 == PLPAREN) {
      if (elementType == IF_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_IF_PARENTHESES);
      }
      else if (elementType == WHILE_STATEMENT || elementType == DO_WHILE_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_WHILE_PARENTHESES);
      }
      else if (elementType == FOR_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_FOR_PARENTHESES);
      }
      else if (elementType == SWITCH_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_SWITCH_PARENTHESES);
      }
      else if (elementType == TRY_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_TRY_PARENTHESES);
      }
      else if (elementType == CATCH_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_CATCH_PARENTHESES);
      }
      else if (FUNCTION_DEFINITION.contains(elementType)) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_PARENTHESES);
      }
      else if (elementType == CALL_EXPRESSION) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES);
      }
    }

    //
    //Spacing before left braces
    //
    if (type2 == BLOCK_STATEMENT) {
      if (elementType == IF_STATEMENT && type1 != KELSE) {
        return setBraceSpace(mySettings.SPACE_BEFORE_IF_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == IF_STATEMENT && type1 == KELSE) {
        return setBraceSpace(mySettings.SPACE_BEFORE_ELSE_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == WHILE_STATEMENT || elementType == DO_WHILE_STATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_WHILE_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == FOR_STATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_FOR_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == SWITCH_STATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_SWITCH_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == TRY_STATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_TRY_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == CATCH_STATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_CATCH_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (FUNCTION_DEFINITION.contains(elementType)) {
        return setBraceSpace(mySettings.SPACE_BEFORE_METHOD_LBRACE, mySettings.METHOD_BRACE_STYLE, child1.getTextRange());
      }
    }

    if ((elementType == CLASS_DECLARATION || elementType == ENUM_DECLARATION || elementType == INTERFACE_DECLARATION) &&
        type2 == PLCURLY) {
      return setBraceSpace(mySettings.SPACE_BEFORE_CLASS_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
    }

    if (type1 == PLPAREN || type2 == PRPAREN) {
      if (elementType == IF_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_IF_PARENTHESES);
      }
      else if (elementType == WHILE_STATEMENT || elementType == DO_WHILE_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_WHILE_PARENTHESES);
      }
      else if (elementType == FOR_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_FOR_PARENTHESES);
      }
      else if (elementType == SWITCH_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_SWITCH_PARENTHESES);
      }
      else if (elementType == TRY_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_TRY_PARENTHESES);
      }
      else if (elementType == CATCH_STATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_CATCH_PARENTHESES);
      }
      else if (FUNCTION_DEFINITION.contains(elementType)) {
        final boolean newLineNeeded = type1 == PLPAREN ?
                                      mySettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE :
                                      mySettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE;
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_METHOD_PARENTHESES, newLineNeeded);
      }
      else if (elementType == CALL_EXPRESSION) {
        final boolean newLineNeeded = type1 == PLPAREN ?
                                      mySettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE :
                                      mySettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE;
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_METHOD_CALL_PARENTHESES, newLineNeeded);
      }
      else if (mySettings.BINARY_OPERATION_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP && elementType == PARENTHESIZED_EXPRESSION) {
        final boolean newLineNeeded = type1 == PLPAREN ?
                                      mySettings.PARENTHESES_EXPRESSION_LPAREN_WRAP :
                                      mySettings.PARENTHESES_EXPRESSION_RPAREN_WRAP;
        return addSingleSpaceIf(false, newLineNeeded);
      }
    }

    if (elementType == TERNARY_EXPRESSION) {
      if (type2 == OQUEST) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_QUEST);
      }
      else if (type2 == OCOLON) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_COLON);
      }
      else if (type1 == OQUEST) {
        return addSingleSpaceIf(mySettings.SPACE_AFTER_QUEST);
      }
      else if (type1 == OCOLON) {
        return addSingleSpaceIf(mySettings.SPACE_AFTER_COLON);
      }
    }

    //
    // Spacing around assignment operators (=, -=, etc.)
    //

    if (ASSIGN_OPERATORS.contains(type1) || ASSIGN_OPERATORS.contains(type2) ||
        type2 == VAR_INIT) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_ASSIGNMENT_OPERATORS);
    }

    //
    // Spacing around  logical operators (&&, OR, etc.)
    //
    if (LOGIC_OPERATORS.contains(type1) || LOGIC_OPERATORS.contains(type2)) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_LOGICAL_OPERATORS);
    }
    //
    // Spacing around  equality operators (==, != etc.)
    //
    if ((type1 == COMPARE_OPERATION && EQUALITY_OPERATORS.contains(typeType1)) ||
        (type2 == COMPARE_OPERATION && EQUALITY_OPERATORS.contains(typeType2))) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_EQUALITY_OPERATORS);
    }
    //
    // Spacing around  relational operators (<, <= etc.)
    //
    if ((type1 == COMPARE_OPERATION && RELATIONAL_OPERATORS.contains(typeType1)) ||
        (type2 == COMPARE_OPERATION && RELATIONAL_OPERATORS.contains(typeType2))) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_RELATIONAL_OPERATORS);
    }
    //
    // Spacing around  additive operators ( &, |, ^, etc.)
    //
    if (BITWISE_OPERATORS.contains(type1) || BITWISE_OPERATORS.contains(type2)) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_BITWISE_OPERATORS);
    }
    //
    // Spacing around  additive operators ( +, -, etc.)
    //
    if ((ADDITIVE_OPERATORS.contains(type1) || ADDITIVE_OPERATORS.contains(type2)) &&
        elementType != PREFIX_EXPRESSION) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_ADDITIVE_OPERATORS);
    }
    //
    // Spacing around  multiplicative operators ( *, /, %, etc.)
    //
    if (MULTIPLICATIVE_OPERATORS.contains(type1) || MULTIPLICATIVE_OPERATORS.contains(type2)) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS);
    }
    //
    // Spacing around  unary operators ( NOT, ++, etc.)
    //
    if ((UNARY_OPERATORS.contains(type1) || UNARY_OPERATORS.contains(type2)) &&
        elementType == PREFIX_EXPRESSION) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_UNARY_OPERATOR);
    }
    //
    // Spacing around  shift operators ( <<, >>, >>>, etc.)
    //
    if (SHIFT_OPERATORS.contains(type1) || SHIFT_OPERATORS.contains(type2)) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_SHIFT_OPERATORS);
    }

    //
    //Spacing before keyword (else, catch, etc)
    //
    if (type2 == KELSE) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_ELSE_KEYWORD, mySettings.ELSE_ON_NEW_LINE);
    }
    if (type2 == KWHILE) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_WHILE_KEYWORD, mySettings.WHILE_ON_NEW_LINE);
    }
    if (type2 == CATCH_STATEMENT) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_CATCH_KEYWORD, mySettings.CATCH_ON_NEW_LINE);
    }

    //
    //Other
    //

    if (type1 == KELSE && type2 == IF_STATEMENT) {
      return Spacing.createSpacing(1, 1, mySettings.SPECIAL_ELSE_IF_TREATMENT ? 0 : 1, false, mySettings.KEEP_BLANK_LINES_IN_CODE);
    }

    if (type1 == OCOMMA && (elementType == PARAMETER_LIST || elementType == EXPRESSION_LIST) &&
        (parentType == CALL_EXPRESSION ||
         parentType == NEW_EXPRESSION ||
         FUNCTION_DEFINITION.contains(parentType))) {
      return addSingleSpaceIf(mySettings.SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS);
    }

    if (type1 == OCOMMA) {
      return addSingleSpaceIf(mySettings.SPACE_AFTER_COMMA);
    }

    if (type2 == OCOMMA) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_COMMA);
    }

    if (type1 == OCOLON && elementType == TYPE_TAG) {
      return addSingleSpaceIf(myHaxeCodeStyleSettings.SPACE_AFTER_TYPE_REFERENCE_COLON);
    }

    if (type2 == TYPE_TAG) {
      return addSingleSpaceIf(myHaxeCodeStyleSettings.SPACE_BEFORE_TYPE_REFERENCE_COLON);
    }

    if (type1 == OARROW || type2 == OARROW) {
      return addSingleSpaceIf(myHaxeCodeStyleSettings.SPACE_AROUND_ARROW);
    }

    return Spacing.createSpacing(0, 1, 0, true, mySettings.KEEP_BLANK_LINES_IN_CODE);
  }

  private Spacing addSingleSpaceIf(boolean condition) {
    return addSingleSpaceIf(condition, false);
  }

  private Spacing addSingleSpaceIf(boolean condition, boolean linesFeed) {
    final int spaces = condition ? 1 : 0;
    final int lines = linesFeed ? 1 : 0;
    return Spacing.createSpacing(spaces, spaces, lines, mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
  }

  private Spacing setBraceSpace(boolean needSpaceSetting,
                                @CommonCodeStyleSettings.BraceStyleConstant int braceStyleSetting,
                                TextRange textRange) {
    final int spaces = needSpaceSetting ? 1 : 0;
    if (braceStyleSetting == CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED && textRange != null) {
      return Spacing.createDependentLFSpacing(spaces, spaces, textRange, mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
    }
    else {
      final int lineBreaks = braceStyleSetting == CommonCodeStyleSettings.END_OF_LINE ||
                             braceStyleSetting == CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED ? 0 : 1;
      return Spacing.createSpacing(spaces, spaces, lineBreaks, false, 0);
    }
  }
}
