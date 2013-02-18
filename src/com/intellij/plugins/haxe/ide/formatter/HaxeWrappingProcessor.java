package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.WrappingUtil;
import com.intellij.psi.tree.IElementType;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;


/**
 * @author: Fedor.Korotkov
 */
public class HaxeWrappingProcessor {
  private final ASTNode myNode;
  private final CommonCodeStyleSettings mySettings;

  public HaxeWrappingProcessor(ASTNode node, CommonCodeStyleSettings settings) {
    myNode = node;
    mySettings = settings;
  }

  Wrap createChildWrap(ASTNode child, Wrap defaultWrap, Wrap childWrap) {
    final IElementType childType = child.getElementType();
    final IElementType elementType = myNode.getElementType();
    if (childType == OCOMMA || childType == OSEMI) return defaultWrap;

    //
    // Function definition/call
    //
    if (elementType == PARAMETER_LIST || elementType == EXPRESSION_LIST) {
      final ASTNode parent = myNode.getTreeParent();
      if (parent == null) {
        return defaultWrap;
      }
      final IElementType parentType = parent.getElementType();
      if (parentType == CALL_EXPRESSION &&
          mySettings.CALL_PARAMETERS_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP) {
        if (myNode.getFirstChildNode() == child) {
          return createWrap(mySettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE);
        }
        if (!mySettings.PREFER_PARAMETERS_WRAP && childWrap != null) {
          return Wrap.createChildWrap(childWrap, WrappingUtil.getWrapType(mySettings.CALL_PARAMETERS_WRAP), true);
        }
        return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.CALL_PARAMETERS_WRAP), true);
      }
      if (FUNCTION_DEFINITION.contains(parentType) &&
          mySettings.METHOD_PARAMETERS_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP) {
        if (myNode.getFirstChildNode() == child) {
          return createWrap(mySettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE);
        }
        if (childType == PRPAREN) {
          return createWrap(mySettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE);
        }
        return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.METHOD_PARAMETERS_WRAP), true);
      }
    }

    if (elementType == CALL_EXPRESSION) {
      if (mySettings.CALL_PARAMETERS_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP) {
        if (childType == PRPAREN) {
          return createWrap(mySettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE);
        }
      }
    }

    //
    // If
    //
    if (elementType == IF_STATEMENT && childType == KELSE) {
      return createWrap(mySettings.ELSE_ON_NEW_LINE);
    }

    //
    //Binary expressions
    //
    if (BINARY_EXPRESSIONS.contains(elementType) && mySettings.BINARY_OPERATION_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP) {
      if ((mySettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE && BINARY_OPERATORS.contains(childType)) ||
          (!mySettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE && isRightOperand(child))) {
        return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.BINARY_OPERATION_WRAP), true);
      }
    }

    //
    // Assignment
    //
    if (elementType == ASSIGN_EXPRESSION && mySettings.ASSIGNMENT_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP) {
      if (childType != ASSIGN_OPERATION) {
        if (FormatterUtil.isPrecededBy(child, ASSIGN_OPERATION) &&
            mySettings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE) {
          return Wrap.createWrap(WrapType.NONE, true);
        }
        return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.ASSIGNMENT_WRAP), true);
      }
      else if (mySettings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE) {
        return Wrap.createWrap(WrapType.NORMAL, true);
      }
    }

    //
    // Ternary expressions
    //
    if (elementType == TERNARY_EXPRESSION) {
      if (myNode.getFirstChildNode() != child) {
        if (mySettings.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE) {
          if (!FormatterUtil.isPrecededBy(child, OQUEST) &&
              !FormatterUtil.isPrecededBy(child, OCOLON)) {
            return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.TERNARY_OPERATION_WRAP), true);
          }
        }
        else if (childType != OQUEST && childType != OCOLON) {
          return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.TERNARY_OPERATION_WRAP), true);
        }
      }
      return Wrap.createWrap(WrapType.NONE, true);
    }
    return defaultWrap;
  }

  private boolean isRightOperand(ASTNode child) {
    return myNode.getLastChildNode() == child;
  }

  private static Wrap createWrap(boolean isNormal) {
    return Wrap.createWrap(isNormal ? WrapType.NORMAL : WrapType.NONE, true);
  }

  private static Wrap createChildWrap(ASTNode child, int parentWrap, boolean newLineAfterLBrace, boolean newLineBeforeRBrace) {
    IElementType childType = child.getElementType();
    if (childType != PLPAREN && childType != PRPAREN) {
      if (FormatterUtil.isPrecededBy(child, PLBRACK)) {
        if (newLineAfterLBrace) {
          return Wrap.createChildWrap(Wrap.createWrap(parentWrap, true), WrapType.ALWAYS, true);
        }
        else {
          return Wrap.createWrap(WrapType.NONE, true);
        }
      }
      return Wrap.createWrap(WrappingUtil.getWrapType(parentWrap), true);
    }
    if (childType == PRBRACK && newLineBeforeRBrace) {
      return Wrap.createWrap(WrapType.ALWAYS, true);
    }
    return Wrap.createWrap(WrapType.NONE, true);
  }
}
