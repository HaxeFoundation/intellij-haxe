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
package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.FUNCTION_LIKE_OWNERS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.BINARY_EXPRESSIONS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;


/**
 * @author: Fedor.Korotkov
 */
public class HaxeAlignmentProcessor {
  private final ASTNode myNode;
  private final Alignment myBaseAlignment;
  private final CommonCodeStyleSettings mySettings;

  public HaxeAlignmentProcessor(ASTNode node, CommonCodeStyleSettings settings) {
    myNode = node;
    mySettings = settings;
    myBaseAlignment = Alignment.createAlignment();
  }

  /**
   * The one alignment all children of the node share when its kind aligns
   * under the ALIGN_MULTILINE_* settings: binary operands, ternary parts, and
   * parameter or call argument lists. Null when the children do not align.
   */
  @Nullable
  public Alignment createChildAlignment() {
    IElementType elementType = myNode.getElementType();
    ASTNode parent = myNode.getTreeParent();
    IElementType parentType = parent == null ? null : parent.getElementType();

    if (BINARY_EXPRESSIONS.contains(elementType) && mySettings.ALIGN_MULTILINE_BINARY_OPERATION
        && !insideArgumentList(myNode)) {
      return myBaseAlignment;
    }

    if (elementType == TERNARY_EXPRESSION && mySettings.ALIGN_MULTILINE_TERNARY_OPERATION) {
      return myBaseAlignment;
    }

    if (elementType == PARAMETER_LIST || elementType == EXPRESSION_LIST || elementType == CALL_EXPRESSION_LIST) {
      boolean doAlign = false;
      if (FUNCTION_LIKE_OWNERS.contains(parentType)) {
        doAlign = mySettings.ALIGN_MULTILINE_PARAMETERS;
      }
      else if (parentType == CALL_EXPRESSION) {
        doAlign = mySettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS;
      }
      if (doAlign) {
        return myBaseAlignment;
      }
    }

    return null;
  }

  /**
   * Whether the node sits inside a parameter or argument list, with no block
   * or class body in between. A binary expression used as an argument starts
   * mid-line, so aligning its operands to that arbitrary column indents each
   * argument deeper than the last. Operand alignment only makes sense where
   * the expression starts its line.
   */
  private static boolean insideArgumentList(ASTNode node) {
    for (ASTNode parent = node.getTreeParent(); parent != null; parent = parent.getTreeParent()) {
      IElementType type = parent.getElementType();
      if (type == EXPRESSION_LIST || type == CALL_EXPRESSION_LIST || type == PARAMETER_LIST) {
        return true;
      }
      if (type == BLOCK_STATEMENT || type == SWITCH_CASE_BLOCK || type == CLASS_BODY) {
        return false;
      }
    }
    return false;
  }
}
