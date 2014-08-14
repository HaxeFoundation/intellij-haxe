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

import com.intellij.formatting.Alignment;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.BINARY_EXPRESSIONS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.FUNCTION_DEFINITION;
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

  @Nullable
  public Alignment createChildAlignment() {
    IElementType elementType = myNode.getElementType();
    ASTNode parent = myNode.getTreeParent();
    IElementType parentType = parent == null ? null : parent.getElementType();

    if (BINARY_EXPRESSIONS.contains(elementType) && mySettings.ALIGN_MULTILINE_BINARY_OPERATION) {
      return myBaseAlignment;
    }

    if (elementType == TERNARY_EXPRESSION && mySettings.ALIGN_MULTILINE_TERNARY_OPERATION) {
      return myBaseAlignment;
    }

    if (elementType == PARAMETER_LIST || elementType == EXPRESSION_LIST) {
      boolean doAlign = false;
      if (FUNCTION_DEFINITION.contains(parentType)) {
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
}
