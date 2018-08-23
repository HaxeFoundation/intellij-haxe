/*
 * Copyright 2017-2018 Eric Bishton
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
package com.intellij.plugins.haxe.lang.util;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * Static routines to classify language tokens.
 *
 * Created by ebishton on 3/31/17.
 */
public class HaxeAstUtil {
  private HaxeAstUtil() {}

  /**
   * Determine if the node is an operator allowed in conditional compilation expressions.
   *
   * @param t token to check
   * @return true, if the operator is allowed in conditional compilation expressions; false, otherwise.
   */
  public static boolean isCCOperator(@Nullable ASTNode t) {
    return isOfType(t, OCOND_OR,
                       OCOND_AND,
                       OEQ,
                       OLESS,
                       OLESS_OR_EQUAL,
                       OGREATER,
                       OGREATER_OR_EQUAL,
                       ONOT_EQ,
                       ONOT
    );
  }

  public static boolean isNegation(@Nullable ASTNode t) {
    return t != null && t.getElementType().equals(ONOT);
  }

  public static boolean isLeftParen(@Nullable ASTNode t) {
    return t != null && t.getElementType().equals(PLPAREN);
  }

  public static boolean isRightParen(@Nullable ASTNode t) {
    return t != null && t.getElementType().equals(PRPAREN);
  }

  public static boolean isParen(@Nullable ASTNode t) {
    return isOfType(t, PRPAREN, PLPAREN);
  }

  public static boolean isStringOpenQuote(@Nullable ASTNode t) {
    return t != null && t.getElementType().equals(OPEN_QUOTE);
  }

  public static boolean isStringClosingQuote(@Nullable ASTNode t) {
    return t != null && t.getElementType().equals(CLOSING_QUOTE);
  }

  public static boolean isStringQuote(@Nullable ASTNode t) {
    return isOfType(t, OPEN_QUOTE, CLOSING_QUOTE);
  }

  public static boolean isString(@Nullable ASTNode t) {
    return null != t && t.getElementType().equals(REGULAR_STRING_PART);
  }


  // Use these if we want to use regexes instead of the Float class to parse floating point values.
  //private final static String exponent = "[eE][+-]?[0-9]+";
  //private final static String digit = "[0-9]";
  //private final static Pattern numFloat = Pattern.compile("((([0-9]*\\.[0-9]+)|([0-9]+\\.[0-9]*))("+exponent+")?)|([0-9]+"+exponent+")");
  public static boolean isFloat(@Nullable String s) {
    return isFloat(s, null);
  }
  public static boolean isFloat(@Nullable String s, @Nullable FloatResult result) {
    try {
      Float f = Float.valueOf(s);
      if (null != result) {
        result.result = f;
      }
      return true;
    }
    catch (NumberFormatException e) {
      if (null != result)
        result.result = Float.NaN;
    }
    return false;
  }

  private final static Pattern hexInteger = Pattern.compile("0[xX][0-9A-Fa-f]+");
  private final static Pattern octInteger = Pattern.compile("0[0-7]+");  // Haxe doesn't understand the unary MINUS in PP
  private final static Pattern decInteger = Pattern.compile("0|[1-9][0-9]*");

  public static boolean isInteger(@Nullable String s) {
    // Can't use the same trick as a float because we may have various radixes.
    if (null == s) {
      return false;
    }
    // Optimize by checking whether the first char is a digit.
    char first = s.charAt(0);
    if (first < '0' || first > '9') {
      return false;
    }

    return decInteger.matcher(s).matches() || hexInteger.matcher(s).matches() || octInteger.matcher(s).matches();
  }

  public static boolean isNumber(@Nullable ASTNode t) {
    if (t != null) {
      String text = t.getText();
      // Optimization: If it doesn't start with '+', '-', '.', or digit, it's not a number (ignoring 'Nan' and Infinity).
      // Using the Float class to detect numbers is fairly expensive.  Before this optimization, about 2/3 of the
      // time we spent evaluating a conditional expression was spent in isFloat().
      char firstChar = text.charAt(0);
      if (Character.isDigit(firstChar) || '.' == firstChar || '-' == firstChar || '+' == firstChar) {
        return isInteger(text) || isFloat(text);
      }
    }
    return false;
  }

  public static boolean isTrueKeyword(@Nullable ASTNode t) {
    return null != t && t.getElementType().equals(KTRUE);
  }

  public static boolean isFalseKeyword(@Nullable ASTNode t) {
    return null != t && t.getElementType().equals(KFALSE);
  }

  public static boolean isIdentifier(@Nullable ASTNode t) {
    return null != t && t.getElementType().equals(ID);
  }

  public static boolean isConstant(@Nullable ASTNode t) {
    // return isTrueKeyword(t) || isFalseKeyword(t) || isString(t) || isNumber(t);
    return isOfType(t, KTRUE, KFALSE, REGULAR_STRING_PART) || isNumber(t);
  }

  public static boolean isLiteral(@Nullable ASTNode t) {
    return isIdentifier(t) || isConstant(t);
  }

  public static boolean isWhitespace(@Nullable ASTNode t) {
    return null != t && HaxeTokenTypeSets.WHITESPACES.contains(t.getElementType());
  }

  public static boolean isOfType(@Nullable ASTNode t, IElementType... types) {
    if (t != null) {
      IElementType nodetype = t.getElementType();
      for (IElementType type : types) {
        if (type.equals(nodetype)) {
          return true;
        }
      }
    }
    return false;
  }

  public static class FloatResult {
    public Float result = Float.NaN;
  }
}
