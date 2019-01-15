/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019 Eric Bishton
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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.util.HaxeExpressionUtil;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.codeStyle.NameUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeNameSuggesterUtil {
  private HaxeNameSuggesterUtil() {
  }

  private static String deleteNonLetterFromString(@NotNull final String string) {
    Pattern pattern = Pattern.compile("[^a-zA-Z_]+");
    Matcher matcher = pattern.matcher(string);
    return matcher.replaceAll("_");
  }

  @NotNull
  public static String prepareNameTextForSuggestions(@NotNull String name) {
    // Note: decapitalize only changes the first letter to lower case, but won't do if the second letter is also uppercase.
    name = StringUtil.decapitalize(deleteNonLetterFromString(StringUtil.unquoteString(name.replace('.', '_'))));

    //if (name.startsWith("get") && name.length() > 3) {
    //  name = name.substring(3);
    //}
    //else if (name.startsWith("is") && name.length() > 2) {
    //  name = name.substring(2);
    //}
    //while (name.startsWith("_") && name.length() > 1) {
    //  name = name.substring(1);
    //}
    //while (name.endsWith("_") && name.length() > 1) {
    //  name = name.substring(0, name.length() - 1);
    //}
    //return name;

    StringBuilder prepped = new StringBuilder();
    int startPos = 0;
    int endPos = name.length() - 1;

    // Trim (skip past) underscores and common leading words.
    // (Leave an underscore or name if that is the entirety of the remaining text.)
    while (startPos < endPos && '_' == name.charAt(startPos)) ++startPos;
    while (endPos > startPos && '_' == name.charAt(endPos)) --endPos;
    for (String prefix : new String[]{"get", "is"}) {
      if (name.startsWith(prefix) && (endPos - startPos) > prefix.length()) {
        startPos += prefix.length();
      }
    }

    // Copy the string, removing consecutive underscores.
    char c = '_';  // Assume last char is '_' to skip any underscores after 'get' or 'is', as in get_myVar();
    for (int i = startPos; i <= endPos; i++) {
      if (c == '_' && name.charAt(i) == '_') continue;
      c = name.charAt(i);
      prepped.append(c);
    }

    return prepped.toString();
  }


  @NotNull
  public static Collection<String> generateNames(@NotNull String name, boolean useUpperCase, boolean isArray) {
    name = prepareNameTextForSuggestions(name);

    Collection<String> candidates = new LinkedHashSet<String>();
    if (null != name && !name.isEmpty()) {
      candidates.addAll(NameUtil.getSuggestionsByName(name, "", "", useUpperCase, false, isArray));
    }
    return candidates;
  }

  @NotNull
  public static String getDefaultExpressionName(HaxeExpression expression, boolean useUpperCase) {
    String lower = getDefaultExpressionName(expression);
    return useUpperCase ? lower.toUpperCase() : lower;
  }

  @NotNull
  public static String getDefaultExpressionName(HaxeExpression expression) {
    ResultHolder typeResult = HaxeTypeResolver.getPsiElementType(expression, new HaxeGenericResolver());
    SpecificTypeReference type = typeResult.getType();

    if (type.isDynamic()) { return "dyn"; }
    if (type.isVoid()) { return "v"; }
    if (type.isInt()) { return "i"; }
    if (type.isBool()) { return "b"; }
    if (type.isFloat()) { return "f"; }
    if (type.isString()) { return "str"; }
    if (type.isArray()) { return "arr"; }
    if (type.isMap()) { return "map"; }

    if (type instanceof SpecificHaxeClassReference) {
      SpecificHaxeClassReference ref = (SpecificHaxeClassReference)type;
      HaxeClass clazz = ref.getHaxeClass();
      String name = null == clazz ? null : clazz.getName();
      if (null != name) {
        return HaxeStringUtil.toLowerFirst(name);
      }
    }

    // If result typing doesn't work (e.g. it's Unknown or Invalid), then try against
    // the kind of expression.

    if (expression instanceof HaxeAssignExpression
        || expression instanceof HaxeReferenceExpression) {
      return "var"; // Should come out of expression.getType() or a resolve(),
    }
    if (expression instanceof HaxePrefixExpression) {  // Wraps statements, like HaxeIfStatement
      return "expr";
    }
    if (expression instanceof HaxeSwitchCaseExpression) {
      return "switchResult";
    }
    if (expression instanceof HaxeBitwiseExpression
        || expression instanceof HaxeShiftExpression) {
      return "bitResult";
    }
    if (expression instanceof HaxeLogicAndExpression
        || expression instanceof HaxeLogicOrExpression
        || expression instanceof HaxeCompareExpression) {
      return "logicalResult";
    }
    if (expression instanceof HaxeSuperExpression) {
      return "mysuper";
    }
    if (expression instanceof HaxeFatArrowExpression
        || expression instanceof HaxeFunctionLiteral) {
      return "lambda";
    }
    if (expression instanceof HaxeStringLiteralExpression) {
      return "str";
    }
    if (expression instanceof HaxeCallExpression) {
      return "functionResult";
    }
    if (expression instanceof HaxeThisExpression) {
      return "myself";
    }
    if (expression instanceof HaxeTernaryExpression) {
      return "ternaryResult";
    }
    if (expression instanceof HaxeIteratorExpression) {
      return "iter";
    }
    if (expression instanceof HaxeAdditiveExpression
        || expression instanceof HaxeMultiplicativeExpression) {
      return "f"; // float
    }
    if (expression instanceof HaxeMapLiteral) {
      return "map";
    }
    if (expression instanceof HaxeArrayLiteral) {
      return "arr";
    }
    if (expression instanceof HaxeArrayAccessExpression) {
      return "element";
    }
    if (expression instanceof HaxeRegularExpression
        || expression instanceof HaxeRegularExpressionLiteral) {  // These must come before HaxeLiteralExpression
      return "regex";
    }
    if (expression instanceof HaxeLiteralExpression    // Must come after RegularExpressionXXX
        || expression instanceof HaxeConstantExpression) {
      return "const";
    }
    if (expression instanceof HaxeNewExpression) {
      return "newObj";
    }
    if (expression instanceof HaxeCastExpression) {
      return "cast";
    }
    if (expression instanceof HaxeObjectLiteral) {
      return "anon";  // Anonymous structure.  Maybe should be comprised of element names??
    }
    if (expression instanceof HaxeParenthesizedExpression) {
      return "result";  // Should be typed!
    }
    if (expression instanceof HaxePropertyAccessor) {
      return "prop";
    }
    if (expression instanceof HaxeTypeCheckExpr) {
      HaxeTypeCheckExpr expr = (HaxeTypeCheckExpr) expression;
      HaxeFunctionType functionType = expr.getFunctionType();
      if (null != functionType) {
        return functionType.getName();
      } else {
        HaxeTypeOrAnonymous toa = expr.getTypeOrAnonymous();
        if (null != toa) {
          HaxeType haxeType = toa.getType();
          if (null == haxeType && null != toa.getAnonymousType()) {
            return "anon";
          }
          if (null != haxeType) {
            String name = haxeType.getName();
            if (null != name) {
              return name;
            }
          }
        }
      }
    }

    return "x";
  }

  @NotNull
  public static Collection<String> getSuggestedNames(final HaxeExpression expression, final boolean wantUpperCase) {
    String text = expression.getText();
    boolean useUpperCase = wantUpperCase;
    boolean isArray = HaxeExpressionUtil.isArrayExpression(expression);
    if (expression instanceof HaxeCallExpression) {
      final HaxeExpression callee = ((HaxeCallExpression)expression).getExpression();
      text = callee.getText();
    } else if (expression instanceof HaxeRegularExpression) {
      text = "REGEX_";
      useUpperCase = true;
    }

    Collection<String> candidates = text == null ? new LinkedHashSet<String>()
                                                 : HaxeNameSuggesterUtil.generateNames(text, useUpperCase, isArray);
    candidates.add(HaxeNameSuggesterUtil.getDefaultExpressionName(expression, useUpperCase));

    final Set<String> usedNames = HaxeRefactoringUtil.collectUsedNames(expression);
    usedNames.addAll(HaxeRefactoringUtil.collectKeywords());
    final Collection<String> result = new ArrayList<String>();

    for (String candidate : candidates) {
      int index = 0;
      String suffix = "";
      while (usedNames.contains(candidate + suffix)) {
        suffix = Integer.toString(++index);
      }
      result.add(candidate + suffix);
    }

    return result;
  }
}
