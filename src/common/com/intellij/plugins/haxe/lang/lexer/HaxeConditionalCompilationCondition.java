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

import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.parser.HaxeAstFactory;
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

  // Here is a set of rules to parse conditions, if we ever want to run them
  // through a parser:
  //    ppIdentifier ::= identifier
  //    private ppNumberPrefix ::= "-"
  //    ppNumber ::= ppNumberPrefix? (LITINT | LITHEX | LITOCT | LITFLOAT)
  //    private ppNegation ::= "!"
  //    private ppComparisonOperator ::= ("=="|"!="|">"|">="|"<"|"<=")
  //    left ppOperator ::= ("||" | "&&" | ppComparisonOperator)
  //    ppLiteral ::= ppIdentifier | ppNumber | KTRUE | KFALSE
  //    private ppSimpleExpression ::= ppNegation? (ppParenthesizedExpression | ppLiteral)
  //    left ppExpression ::=  ppSimpleExpression (ppOperator ppSimpleExpression)*
  //    private ppParenthesizedExpression ::= '(' ppExpression ')'
  //
  //    private pp_statement_recover ::= !(ppStatement | ';' | '[' | ']' | '{' | '}' | assignOperation | bitOperation)
  //    private ppStatementWithCondition ::= ("#if" | "#elseif") ppExpression {pin=1 recoverWhile="pp_statement_recover"}
  //    private ppStatementWithoutCondition ::= "#else" | "#end"  {recoverWhile="pp_statement_recover"}
  //    private ppStatementWithComment ::= "#line" | "#error"  {pin=1 recoverWhile="!\n"}
  //    ppStatement ::= ppStatementWithCondition | ppStatementWithoutCondition | ppStatementWithComment {extends="com.intellij.psi.PsiComment"}


  private final ArrayList<ASTNode> tokens = new ArrayList<ASTNode>();
  private boolean evaluated = false;
  private boolean evalResult = false;

  public HaxeConditionalCompilationCondition(@Nullable ArrayList<ASTNode> startTokens) {
    if (startTokens != null) {
      tokens.addAll(startTokens);
    }
  }

  private boolean isOperator(ASTNode t) {
    if (null != t) {
      IElementType type = t.getElementType();
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

  private boolean isNegation(ASTNode t) {
    return (t != null) ? t.getElementType().equals(HaxeTokenTypes.ONOT) : false;
  }

  private boolean isParen(ASTNode t) {
    if (t != null) {
      IElementType type = t.getElementType();
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

  private boolean isNumber(ASTNode t) {
    if (t != null) {
      String text = t.getText();
      return isInteger(text) || isFloat(text);
    }
    return false;
  }

  private boolean isTrueKeyword(ASTNode t) {
    return null == t ? false : t.getElementType().equals(HaxeTokenTypes.KTRUE);
  }

  private boolean isFalseKeyword(ASTNode t) {
    return null == t ? false : t.getElementType().equals(HaxeTokenTypes.KFALSE);
  }

  private boolean isIdentifier(ASTNode t) {
    // return !(isOperator(t) || isNegation(t) || isParen(t) || isNumber(t) || isTrueKeyword(t) || isFalseKeyword(t));
    return t.getElementType().equals(HaxeTokenTypes.ID);
  }

  public boolean isTrue() {
    return tokens.isEmpty() ? false : evaluate();
  }

  public void extend(@NotNull CharSequence chars, @NotNull IElementType tokenType) {
    tokens.add(HaxeAstFactory.leaf(tokenType, chars));
    evaluated = false;
  }

  private boolean areParensBalanced() {
    int parenCount = 0;
    for (ASTNode t : tokens) {
      IElementType type = t.getElementType();
      if (type.equals(HaxeTokenTypes.PLPAREN)) {
        parenCount++;
      }
      else if (type.equals(HaxeTokenTypes.PRPAREN)) {
        parenCount--;
      }
    }
    return parenCount == 0;
  }

  private boolean tokenCanStandAlone(ASTNode t) {
    return isIdentifier(t) || isTrueKeyword(t) || isFalseKeyword(t) || isNumber(t);
  }

  public boolean isComplete() {
    if (tokens.isEmpty()) {
      return false;
    }

    ASTNode first = tokens.get(0);
    if (tokens.size() == 1) {
      return tokenCanStandAlone(first);
    }
    if (tokens.size() == 2) {
      ASTNode second = tokens.get(1);
      boolean secondIsStandalone = tokenCanStandAlone(second);
      return isNegation(first) && secondIsStandalone;
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
    for (ASTNode t : tokens) {
      s.append(t.getText());
    }
    return s.toString();
  }

}
