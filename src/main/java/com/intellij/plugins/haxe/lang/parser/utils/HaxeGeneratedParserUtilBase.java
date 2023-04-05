/*
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.lang.parser.utils;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespaceSkippedCallback;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.tree.IElementType;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeGeneratedParserUtilBase extends GeneratedParserUtilBase {
  private static boolean whiteSpaceSkipped = false;

  private static boolean parseOperator(PsiBuilder builder_, IElementType operator, IElementType... tokens) {
    final PsiBuilder.Marker marker_ = builder_.mark();

    whiteSpaceSkipped = false;

    builder_.setWhitespaceSkippedCallback(new WhitespaceSkippedCallback() {
      @Override
      public void onSkip(IElementType type, int i, int i1) {
        whiteSpaceSkipped = true;
      }
    });

    for (IElementType token : tokens) {
      if (!consumeTokenFast(builder_, token) || whiteSpaceSkipped) {
        marker_.rollbackTo();
        builder_.setWhitespaceSkippedCallback(null);
        return false;
      }
    }

    builder_.setWhitespaceSkippedCallback(null);
    marker_.collapse(operator);
    return true;
  }

  public static boolean shiftRight(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OPERATOR_SHIFT_RIGHT, OPERATOR_GREATER, OPERATOR_GREATER);
  }

  public static boolean shiftRightAssign(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OPERATOR_SHIFT_RIGHT_ASSIGN, OPERATOR_GREATER, OPERATOR_GREATER, OPERATOR_ASSIGN);
  }

  public static boolean unsignedShiftRight(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OPERATOR_UNSIGNED_SHIFT_RIGHT, OPERATOR_GREATER, OPERATOR_GREATER, OPERATOR_GREATER);
  }

  public static boolean unsignedShiftRightAssign(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OPERATOR_UNSIGNED_SHIFT_RIGHT_ASSIGN, OPERATOR_GREATER, OPERATOR_GREATER, OPERATOR_GREATER,
                         OPERATOR_ASSIGN);
  }

  public static boolean gtEq(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OPERATOR_GREATER_OR_EQUAL, OPERATOR_GREATER, OPERATOR_ASSIGN);
  }


  /**
   * Make a semi-colon optional in the case that it's preceded by a block statement.
   *
   */
  public static boolean semicolonUnlessPrecededByStatement(PsiBuilder builder_, int level) {
    if (consumeTokenFast(builder_, OPERATOR_SEMICOLON)) {
      return true;
    }
    int i = -1;
    IElementType previousType = builder_.rawLookup(i);
    while (null != previousType && isWhitespaceOrComment(builder_, previousType)) {
      previousType = builder_.rawLookup(--i);
    }
    if (previousType == HaxeTokenTypes.ENCLOSURE_CURLY_BRACKET_RIGHT || previousType == HaxeTokenTypes.OPERATOR_SEMICOLON) {
      return true;
    }
    builder_.error(HaxeBundle.message("parsing.error.missing.semi.colon"));
    return false;
  }

}