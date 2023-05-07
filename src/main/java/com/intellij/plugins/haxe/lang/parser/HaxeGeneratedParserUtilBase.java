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
package com.intellij.plugins.haxe.lang.parser;

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
  private static boolean parseOperatorNotFollowedBy(PsiBuilder builder_, IElementType operator, IElementType token) {
    final PsiBuilder.Marker marker_ = builder_.mark();

    IElementType fistElement = builder_.lookAhead(0);
    IElementType secondElement = builder_.lookAhead(1);
    if (fistElement == operator  && secondElement != token) {
      if (consumeTokenFast(builder_, operator)) {
        marker_.collapse(operator);
        return true;
      }

    }

    marker_.rollbackTo();
    builder_.setWhitespaceSkippedCallback(null);
    return false;
  }

  public static boolean shiftRight(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OSHIFT_RIGHT, OGREATER, OGREATER);
  }

  public static boolean shiftRightAssign(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OSHIFT_RIGHT_ASSIGN, OGREATER, OGREATER, OASSIGN);
  }

  public static boolean unsignedShiftRight(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OUNSIGNED_SHIFT_RIGHT, OGREATER, OGREATER, OGREATER);
  }

  public static boolean unsignedShiftRightAssign(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OUNSIGNED_SHIFT_RIGHT_ASSIGN, OGREATER, OGREATER, OGREATER, OASSIGN);
  }

  public static boolean gtEq(PsiBuilder builder_, int level_) {
    return parseOperator(builder_, OGREATER_OR_EQUAL, OGREATER, OASSIGN);
  }

  public static boolean ternary(PsiBuilder builder_, int level_) {
    return parseOperatorNotFollowedBy(builder_, OQUEST, ODOT);
  }


  /**
   * Make a semi-colon optional in the case that it's preceded by a block statement.
   *
   */
  public static boolean semicolonUnlessPrecededByStatement(PsiBuilder builder_, int level) {
    if (consumeTokenFast(builder_, OSEMI)) {
      return true;
    }
    int i = -1;
    IElementType previousType = builder_.rawLookup(i);
    while (null != previousType && isWhitespaceOrComment(builder_, previousType)) {
      previousType = builder_.rawLookup(--i);
    }
    if (previousType == HaxeTokenTypes.PRCURLY || previousType == HaxeTokenTypes.OSEMI) {
      return true;
    }
    builder_.error(HaxeBundle.message("parsing.error.missing.semi.colon"));
    return false;
  }

}