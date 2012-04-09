package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.codeStyle.CodeStyleSettings;
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
  private final CodeStyleSettings mySettings;

  public HaxeSpacingProcessor(ASTNode node, CodeStyleSettings settings) {
    myNode = node;
    mySettings = settings;
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

    if (type1 == HAXE_IMPORTSTATEMENT ||
        type1 == HAXE_PACKAGESTATEMENT ||
        type1 == HAXE_USINGSTATEMENT) {
      return addSingleSpaceIf(false, true);
    }

    if(type2 == HAXE_FUNCTIONDECLARATIONWITHATTRIBUTES){
      return Spacing.createSpacing(0, 0, 2, false, mySettings.KEEP_BLANK_LINES_IN_CODE);
    }

    if (type2 == PLPAREN) {
      if ((elementType == HAXE_IFEXPRESSION || elementType == HAXE_IFSTATEMENT)) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_IF_PARENTHESES);
      }
      else if (elementType == HAXE_WHILESTATEMENT || elementType == HAXE_DOWHILESTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_WHILE_PARENTHESES);
      }
      else if (elementType == HAXE_FORSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_FOR_PARENTHESES);
      }
      else if (elementType == HAXE_SWITCHSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_SWITCH_PARENTHESES);
      }
      else if (elementType == HAXE_TRYSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_TRY_PARENTHESES);
      }
      else if (elementType == HAXE_CATCHSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_CATCH_PARENTHESES);
      }
      else if (FUNCTION_DEFINITION.contains(elementType)) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_PARENTHESES);
      }
      else if (elementType == HAXE_CALLEXPRESSION) {
        return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES);
      }
    }

    //
    //Spacing before left braces
    //
    if (type2 == HAXE_BLOCKSTATEMENT) {
      if (elementType == HAXE_IFSTATEMENT && type1 != KELSE) {
        return setBraceSpace(mySettings.SPACE_BEFORE_IF_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == HAXE_IFSTATEMENT && type1 == KELSE) {
        return setBraceSpace(mySettings.SPACE_BEFORE_ELSE_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == HAXE_WHILESTATEMENT || elementType == HAXE_DOWHILESTATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_WHILE_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == HAXE_FORSTATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_FOR_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == HAXE_SWITCHSTATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_SWITCH_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == HAXE_TRYSTATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_TRY_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (elementType == HAXE_CATCHSTATEMENT) {
        return setBraceSpace(mySettings.SPACE_BEFORE_CATCH_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
      }
      else if (FUNCTION_DEFINITION.contains(elementType)) {
        return setBraceSpace(mySettings.SPACE_BEFORE_METHOD_LBRACE, mySettings.METHOD_BRACE_STYLE, child1.getTextRange());
      }
    }

    if ((elementType == HAXE_CLASSDECLARATION || elementType == HAXE_ENUMDECLARATION || elementType == HAXE_INTERFACEDECLARATION) &&
        type2 == PLCURLY) {
      return setBraceSpace(mySettings.SPACE_BEFORE_CLASS_LBRACE, mySettings.BRACE_STYLE, child1.getTextRange());
    }

    if (type1 == PLPAREN || type2 == PRPAREN) {
      if ((elementType == HAXE_IFEXPRESSION || elementType == HAXE_IFSTATEMENT)) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_IF_PARENTHESES);
      }
      else if (elementType == HAXE_WHILESTATEMENT || elementType == HAXE_DOWHILESTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_WHILE_PARENTHESES);
      }
      else if (elementType == HAXE_FORSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_FOR_PARENTHESES);
      }
      else if (elementType == HAXE_SWITCHSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_SWITCH_PARENTHESES);
      }
      else if (elementType == HAXE_TRYSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_TRY_PARENTHESES);
      }
      else if (elementType == HAXE_CATCHSTATEMENT) {
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_CATCH_PARENTHESES);
      }
      else if (FUNCTION_DEFINITION.contains(elementType)) {
        final boolean newLineNeeded = type1 == PLPAREN ?
                                      mySettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE :
                                      mySettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE;
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_METHOD_PARENTHESES, newLineNeeded);
      }
      else if (elementType == HAXE_CALLEXPRESSION) {
        final boolean newLineNeeded = type1 == PLPAREN ?
                                      mySettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE :
                                      mySettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE;
        return addSingleSpaceIf(mySettings.SPACE_WITHIN_METHOD_CALL_PARENTHESES, newLineNeeded);
      }
      else if (mySettings.BINARY_OPERATION_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP && elementType == HAXE_PARENTHESIZEDEXPRESSION) {
        final boolean newLineNeeded = type1 == PLPAREN ?
                                      mySettings.PARENTHESES_EXPRESSION_LPAREN_WRAP :
                                      mySettings.PARENTHESES_EXPRESSION_RPAREN_WRAP;
        return addSingleSpaceIf(false, newLineNeeded);
      }
    }

    if (elementType == HAXE_TERNARYEXPRESSION) {
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
        type2 == HAXE_VARINIT) {
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
    if ((type1 == HAXE_COMPAREOPERATION && EQUALITY_OPERATORS.contains(typeType1)) ||
        (type2 == HAXE_COMPAREOPERATION && EQUALITY_OPERATORS.contains(typeType2))) {
      return addSingleSpaceIf(mySettings.SPACE_AROUND_EQUALITY_OPERATORS);
    }
    //
    // Spacing around  relational operators (<, <= etc.)
    //
    if ((type1 == HAXE_COMPAREOPERATION && RELATIONAL_OPERATORS.contains(typeType1)) ||
        (type2 == HAXE_COMPAREOPERATION && RELATIONAL_OPERATORS.contains(typeType2))) {
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
        elementType != HAXE_PREFIXEXPRESSION) {
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
        elementType == HAXE_PREFIXEXPRESSION) {
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
    if (type2 == HAXE_CATCHSTATEMENT) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_CATCH_KEYWORD, mySettings.CATCH_ON_NEW_LINE);
    }

    //
    //Other
    //

    if (type1 == KELSE && (type2 == HAXE_IFEXPRESSION || type2 == HAXE_IFSTATEMENT)) {
      return Spacing.createSpacing(1, 1, mySettings.SPECIAL_ELSE_IF_TREATMENT ? 0 : 1, false, mySettings.KEEP_BLANK_LINES_IN_CODE);
    }

    if (type1 == OCOMMA && (elementType == HAXE_PARAMETERLIST || elementType == HAXE_EXPRESSIONLIST) &&
        (parentType == HAXE_CALLEXPRESSION ||
         parentType == HAXE_NEWEXPRESSION ||
         FUNCTION_DEFINITION.contains(parentType))) {
      return addSingleSpaceIf(mySettings.SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS);
    }

    if (type1 == OCOMMA) {
      return addSingleSpaceIf(mySettings.SPACE_AFTER_COMMA);
    }

    if (type2 == OCOMMA) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_COMMA);
    }

    //todo: customize in settings
    if (type1 == OCOLON && elementType == HAXE_TYPETAG) {
      return addSingleSpaceIf(false);
    }

    if (type2 == HAXE_TYPETAG) {
      return addSingleSpaceIf(false);
    }

    if (type1 == OARROW || type2 == OARROW) {
      return addSingleSpaceIf(true);
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
