/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Condition that controls #if and #elseif segments
 *
 * Created by ebishton on 3/23/17.
 */
public class HaxeConditionalCompilationCondition {

  /**
   * Small class to manage the String,Type tuple.
   * XXX: This can be replaced by an ASTNode or similar, except that's got a lot of unnecessary functionality.
   *      See HaxeASTFactory.createLeaf() for something usable.
   */
  public static final class Token {
    private String text;

    private IElementType tokenType;

    public Token(@NotNull String s, @NotNull IElementType type) {
      text = s;
      tokenType = type;
    }

    public String getText() {
      return text;
    }

    public IElementType getTokenType() {
      return tokenType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Token token = (Token)o;

      if (!text.equals(token.text)) return false;
      return tokenType.equals(token.tokenType);
    }

    @Override
    public int hashCode() {
      int result = text.hashCode();
      result = 31 * result + tokenType.hashCode();
      return result;
    }
  }

  private final ArrayList<Token> tokens = new ArrayList<Token>();
  private boolean evaluated = false;
  private boolean evalResult = false;

  // Might be better to use predefined token types instead of redefining them here...
  static final String TRUE = "true";
  static final String FALSE = "false";
  static final String LPAREN = "(";
  static final String RPAREN = ")";
  static final String NEGATION = "!";
  static final String OR = "||";
  static final String AND = "&&";
  static final String EQ = "==";
  static final String LT = "<";
  static final String LE = "<=";
  static final String GE = ">=";
  static final String GT = ">";
  static final String NE = "!=";

  public HaxeConditionalCompilationCondition(@Nullable ArrayList<Token> startTokens) {
    if (startTokens != null) {
      tokens.addAll(startTokens);
    }
  }

  private boolean isOperator(Token t) {
    if (null != t) {
      IElementType type = t.getTokenType();
      if (t.equals(HaxeTokenTypes.OCOND_OR)
          || t.equals(HaxeTokenTypes.OCOND_AND)
          || t.equals(HaxeTokenTypes.OEQ)
          || t.equals(HaxeTokenTypes.OLESS)
          || t.equals(HaxeTokenTypes.OLESS_OR_EQUAL)
          || t.equals(HaxeTokenTypes.OGREATER)
          || t.equals(HaxeTokenTypes.OGREATER_OR_EQUAL)
          || t.equals(HaxeTokenTypes.ONOT_EQ)) {
        return true;
      }
    }
    return false;
  }

  private boolean isNegation(Token t) {
    return (t != null) ? t.getTokenType().equals(HaxeTokenTypes.ONOT) : false;
  }

  private boolean isParen(Token t) {
    if (t != null) {
      IElementType type = t.getTokenType();
      return type.equals(HaxeTokenTypes.PRPAREN) || type.equals(HaxeTokenTypes.PLPAREN);
    }
    return false;
  }

  // Use these if we want to use regexes instead of the Float class to parse floating point values.
  //private final static String exponent = "[eE][+-]?[0-9]+";
  //private final static String digit = "[0-9]";
  //private final static Pattern numFloat = Pattern.compile("((([0-9]*\\.[0-9]+)|([0-9]+\\.[0-9]*))("+exponent+")?)|([0-9]+"+exponent+")");
  private boolean isFloat(String s) {
    try {
      Float f = Float.valueOf(s);
      return true;
    }
    catch (NumberFormatException e) {
    }
    return false;
  }

  private final static Pattern hexInteger = Pattern.compile("0[xX][0-9A-Fa-f]+");
  private final static Pattern octInteger = Pattern.compile("0[0-7]+");  // Haxe doesn't understand the unary MINUS in PP
  private final static Pattern decInteger = Pattern.compile("0|[1-9][0-9]*");

  private boolean isInteger(String s) {
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

  private boolean isNumber(Token t) {
    if (t != null) {
      String text = t.getText();
      return isInteger(text) || isFloat(text);
    }
    return false;
  }

  private boolean isTrueKeyword(Token t) {
    return null == t ? false : t.getTokenType().equals(HaxeTokenTypes.KTRUE);
  }

  private boolean isFalseKeyword(Token t) {
    return null == t ? false : t.getTokenType().equals(HaxeTokenTypes.KFALSE);
  }

  private boolean isIdentifier(Token t) {
    String text = t != null ? t.getText() : null;
    return !(isOperator(t) || isNegation(t) || isParen(t) || isNumber(t) || isParen(t));
  }

  public boolean isTrue() {
    return tokens.isEmpty() ? false : evaluate();
  }

  public void extend(@NotNull String s, @NotNull IElementType tokenType) {
    tokens.add(new Token(s, tokenType));
    evaluated = false;
  }

  private boolean areParensBalanced() {
    int parenCount = 0;
    for (Token t : tokens) {
      IElementType type = t.getTokenType();
      if (type.equals(HaxeTokenTypes.PLPAREN)) {
        parenCount++;
      }
      else if (type.equals(HaxeTokenTypes.PRPAREN)) {
        parenCount--;
      }
    }
    return parenCount == 0;
  }

  public boolean isComplete() {
    if (tokens.isEmpty()) {
      return false;
    }

    Token first = tokens.get(0);
    if (tokens.size() == 1) {
      return isIdentifier(first);
    }
    if (tokens.size() == 2) {
      Token second = tokens.get(1);
      return isNegation(first) && isIdentifier(second);
    }
    return areParensBalanced();
  }

  public boolean evaluate() {
    // Evaluation can be expensive, so we cache the result in order to speed parsing.
    if (!evaluated) {
      evalResult = reevaluate();
      evaluated = true;
    }
    return evalResult;
  }

  private boolean reevaluate() {
    // TODO: implement evaluate
    if (!isComplete()) {
      return false;
    }
    return true;
  }

  public String toString() {
    StringBuilder s = new StringBuilder();
    for (Token t : tokens) {
      s.append(t.getText());
    }
    return s.toString();
  }

}
