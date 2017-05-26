/*
 * Copyright 2017-2017 Ilya Malanin
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
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;

public class HaxeGeneratedParserUtilBase extends GeneratedParserUtilBase {
  public static boolean shiftRight(PsiBuilder builder_, int level_) {
    final PsiBuilder.Marker marker_ = builder_.mark();
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    marker_.collapse(HaxeTokenTypes.OSHIFT_RIGHT);
    return true;
  }

  public static boolean shiftRightAssign(PsiBuilder builder_, int level_) {
    final PsiBuilder.Marker marker_ = builder_.mark();
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OASSIGN)) {
      marker_.rollbackTo();
      return false;
    }
    marker_.collapse(HaxeTokenTypes.OSHIFT_RIGHT_ASSIGN);
    return true;
  }

  public static boolean unsignedShiftRight(PsiBuilder builder_, int level_) {
    final PsiBuilder.Marker marker_ = builder_.mark();
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    marker_.collapse(HaxeTokenTypes.OUNSIGNED_SHIFT_RIGHT);
    return true;
  }

  public static boolean unsignedShiftRightAssign(PsiBuilder builder_, int level_) {
    final PsiBuilder.Marker marker_ = builder_.mark();
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OASSIGN)) {
      marker_.rollbackTo();
      return false;
    }
    marker_.collapse(HaxeTokenTypes.OUNSIGNED_SHIFT_RIGHT_ASSIGN);
    return true;
  }

  public static boolean gtEq(PsiBuilder builder_, int level_) {
    final PsiBuilder.Marker marker_ = builder_.mark();
    if (!consumeToken(builder_, HaxeTokenTypes.OGREATER)) {
      marker_.rollbackTo();
      return false;
    }
    if (!consumeToken(builder_, HaxeTokenTypes.OASSIGN)) {
      marker_.rollbackTo();
      return false;
    }
    marker_.collapse(HaxeTokenTypes.OGREATER_OR_EQUAL);
    return true;
  }
}