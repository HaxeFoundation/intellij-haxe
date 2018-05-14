/*
 * Copyright 2018-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.HaxePsiToken;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.PsiElement;
import com.intellij.util.SmartList;

import javax.annotation.Nullable;
import java.util.List;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.REGULAR_STRING_PART;
import static com.intellij.plugins.haxe.util.HaxeStringUtil.unescapeString;

public class HaxeStringLiteralUtil {
  @Nullable
  public static Character getCharacter(@Nullable HaxeStringLiteralExpression string) {
    if (string == null) return null;
    if (!string.getShortTemplateEntryList().isEmpty() || !string.getLongTemplateEntryList().isEmpty()) {
      return null;
    }

    List<HaxePsiToken> parts = getRegularStringParts(string);
    if (parts.size() == 1) {
      String text = unescapeString(parts.get(0).getText());
      if (text != null && text.length() == 1) {
        return text.charAt(0);
      }
    }
    return null;
  }

  public static boolean isCharacter(@Nullable HaxeStringLiteralExpression string) {
    if (string == null) return false;

    if (string.getShortTemplateEntryList().isEmpty() && string.getLongTemplateEntryList().isEmpty()) {
      List<HaxePsiToken> parts = getRegularStringParts(string);
      if (parts.size() == 1) {
        String text = parts.get(0).getText();
        return text != null && text.length() == 1;
      }
    }
    return false;
  }

  private static List<HaxePsiToken> getRegularStringParts(HaxeStringLiteralExpression string) {
    SmartList<HaxePsiToken> result = new SmartList<>();
    for (PsiElement child = string.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof HaxePsiToken && ((HaxePsiToken)child).getTokenType() == REGULAR_STRING_PART) {
        result.add((HaxePsiToken)child);
      }
    }
    return result;
  }
}
