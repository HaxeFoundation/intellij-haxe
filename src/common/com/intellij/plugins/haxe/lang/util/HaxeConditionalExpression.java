/*
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
package com.intellij.plugins.haxe.lang.util;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.lang.parser.HaxeAstFactory;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import gnu.trove.THashSet;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.plugins.haxe.lang.util.HaxeAstUtil.*;

/**
 * Condition that controls #if and #elseif conditional compilation (pre-processing) expressions.
 *
 * Created by ebishton on 3/23/17.
 */
public class HaxeConditionalExpression {

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

  static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  //static {      // Take this out when finished debugging.
  //  LOG.setLevel(org.apache.log4j.Level.DEBUG);
  //}

  private final ArrayList<ASTNode> tokens = new ArrayList<ASTNode>();
  private boolean evaluated = false;    // Cleared when dirty.
  private boolean evalResult = false;   // Cleared when dirty.
  private StringBuilder builder = null;

  public HaxeConditionalExpression(@Nullable ArrayList<ASTNode> startTokens) {
    if (startTokens != null) {
      tokens.addAll(startTokens);
    }
  }

  public boolean isTrue(Project context) {
    return tokens.isEmpty() ? false : evaluate(context);
  }

  private void createToken(@NotNull CharSequence chars, @NotNull IElementType tokenType) {
    tokens.add(HaxeAstFactory.leaf(tokenType, chars));
    evaluated = false;
  }

  public void extend(@NotNull CharSequence chars, @NotNull IElementType tokenType) {
    // The parser will break strings up based upon their content.  This is more of an aspect of dealing
    // with escape characters than
    if (OPEN_QUOTE == tokenType) {
      LOG.assertLog(null == builder, "String builder is already allocated, but a string open quote token has been detected.");
      builder = new StringBuilder();
    } else if (REGULAR_STRING_PART == tokenType) {
      LOG.assertLog(null != builder, "String token is parsed, but no StringBuilder has been allocated.");
      // XXX: Should we translate escape sequences to base tokens here?
      builder.append(chars);
    } else if (CLOSING_QUOTE == tokenType) {
      LOG.assertLog(null != builder, "String close quote was parsed, but no StringBuilder has been allocated.");
      createToken(builder, REGULAR_STRING_PART);
      builder = null;
    } else {
      LOG.assertLog(null == builder, "String builder exists, but a non-string token was encountered.");
      createToken(chars, tokenType);
    }
  }

  private boolean areTokensBalanced(IElementType leftToken, IElementType rightToken) {
    LOG.assertLog(leftToken != rightToken, "Cannot balance tokens of the same type.");
    int tokenCount = 0;
    for (ASTNode t : tokens) {
      IElementType type = t.getElementType();
      if (type.equals(leftToken)) {
        tokenCount++;
      }
      else if (type.equals(rightToken)) {
        tokenCount--;
      }
    }
    return tokenCount == 0;
  }

  private boolean areParensBalanced() {
    return areTokensBalanced(PLPAREN, PRPAREN);
  }

  private boolean areStringQuotesBalanced() {
    return areTokensBalanced(OPEN_QUOTE, CLOSING_QUOTE);
  }

  public boolean isComplete() {
    if (tokens.isEmpty()) {
      return false;
    }

    // Ignore whitespace for completion testing.
    ArrayList<ASTNode> nonWhitespaceTokens = new ArrayList<ASTNode>(tokens.size());
    for (ASTNode token : tokens) {
      if (!isWhitespace(token)) {
        nonWhitespaceTokens.add(token);
      }
    }
    if (nonWhitespaceTokens.isEmpty()) {
      return false;
    }

    ASTNode first = nonWhitespaceTokens.get(0);
    if (nonWhitespaceTokens.size() == 1) {
      return isLiteral(first);
    }
    if (nonWhitespaceTokens.size() == 2) {
      ASTNode second = nonWhitespaceTokens.get(1);
      if (isLeftParen(first) && isRightParen(second)) {
        return true;
      }
      boolean secondIsStandalone = isLiteral(second);
      return isNegation(first) && secondIsStandalone;
    }
    return areParensBalanced() && areStringQuotesBalanced();
  }

  public boolean evaluate(Project project) {
    // Evaluation can be expensive, so we cache the result in order to speed parsing.
    if (!evaluated) {
      evalResult = reevaluate(project);
      evaluated = true;
    }
    return evalResult;
  }

  public String tokensToString(List<ASTNode> nodes) {
    StringBuilder s = new StringBuilder();
    boolean first = true;
    for (ASTNode t : nodes) {
      if (!first) s.append(" ");
      s.append(t.getText());
      first = false;
    }
    return s.toString();
  }

  public String toString() {
    StringBuilder s = new StringBuilder();
    for (ASTNode token : tokens) {
      s.append(token.getChars());
    }
    return s.toString();
  }

  /* =================================================================================================
   * Beyond this point are members and methods for evaluation.  At some point, they should become a
   * HaxeConditionalExpressionEvaluator class.
   * =================================================================================================
   */

  /** Defines that we want in place if there is no Project context. */
  private final static Set<String> SDK_DEFINES = new THashSet<String>(Arrays.asList(
    "macro"
  ));
  /** Used for setting defines in the test bed. */
  public static Key<Object> DEFINES_KEY = Key.create("haxe.test.defines");

  /** Evaluation Context */
  @Nullable
  private Project context;

  private boolean reevaluate(Project context) {
    this.context = context;
    boolean ret = false;
    if (isComplete()) {
      try {
        Stack<ASTNode> rpn = infixToRPN();
        String rpnString = LOG.isDebugEnabled() ? tokensToString(rpn) : null;
        ret = objectIsTrue(calculateRPN(rpn));
        if (LOG.isDebugEnabled()) {  // Don't create the strings unless we are debugging them...
          LOG.debug(toString() + " --> " + rpnString + " ==> " + (ret ? "true" : "false"));
        }
        if (!rpn.isEmpty()) {
          throw new CalculationException("Invalid Expression: Tokens left after calculating: " + rpn.toString());
        }
      } catch (CalculationException e) {
        String msg = "Error calculating conditional compiler expression '" + toString() + "'";
        // Add stack info if in debug mode.
        LOG.info( msg, LOG.isDebugEnabled() ? e : null );
      }
    }
    return ret;
  }

  /**
   * Converts an infix expression into an RPN expression.  (Re-orders and removes parenthesis.)
   * For example: !(cpp && js) -> cpp js && !
   *         and: (( cpp || js ) && (haxe-ver < 3))  -> cpp js || haxe-ver 3 < &&
   * See https://en.wikipedia.org/wiki/Reverse_Polish_notation
   * @return
   * @throws CalculationException
   */
  private Stack<ASTNode> infixToRPN() throws CalculationException {
    // This is a simplified shunting-yard algorithm: http://https://en.wikipedia.org/wiki/Shunting-yard_algorithm
    Stack<ASTNode> rpnOutput = new Stack<ASTNode>();
    Stack<ASTNode> operatorStack = new Stack<ASTNode>();

    try {
      for (ASTNode token : tokens) {
        if (isWhitespace(token)) {
          continue;
        }
        if (isLiteral(token) || isStringQuote(token) || isString(token)) {
          rpnOutput.push(token);
        }
        else if (isLeftParen(token)) {
          operatorStack.push(token);
        }
        else if (isRightParen(token)) {
          boolean foundLeftParen = false;
          while (!operatorStack.isEmpty()) {
            ASTNode op = operatorStack.pop();
            if (!isLeftParen(op)) {
              rpnOutput.push(op);
            }
            else {
              foundLeftParen = true;
              break;
            }
          }
          if (operatorStack.isEmpty() && !foundLeftParen) {
            // mismatched parens.
            // TODO: Report errors back through a reporter class.
            throw new CalculationException("Mismatched right parenthesis.");
          }
        }
        else if (isCCOperator(token)) {
          while (!operatorStack.isEmpty()
                 && !isLeftParen(operatorStack.peek())  // Parens have the highest priority, but should not be considered for comparison.
                 && HaxeOperatorPrecedenceTable.shuntingYardCompare(token.getElementType(), operatorStack.peek().getElementType())) {
            rpnOutput.push(operatorStack.pop());
          }
          operatorStack.push(token);
        }
        else {
          throw new CalculationException("Couldn't process token '" + token.toString() + "' when converting to RPN.");
        }
      }
    } catch (HaxeOperatorPrecedenceTable.OperatorNotFoundException e) {
      LOG.warn("IntelliJ-Haxe plugin internal error: Unknown operator encountered while calculating compiler conditional exression:"
               + toString(), e);
      throw new CalculationException(e.toString());
    }

    // Anything left in the operator stack means an error.
    while(!operatorStack.isEmpty()) {
      ASTNode node = operatorStack.pop();
      if (isLeftParen(node)) {
        // Mismatched parens.
        // TODO: Report errors back through a reporter class.
        throw new CalculationException("Mismatched left parenthesis.");
      } else {
        rpnOutput.push(node);
      }
    }

    return rpnOutput;
  }

  private Object calculateRPN(Stack<ASTNode> rpn) throws CalculationException {
    while (!rpn.isEmpty()) {
      ASTNode node = rpn.tryPop();
      if (isCCOperator(node)) {
        switch (getArity(node)) {
          case UNARY: {
            Object rhs = calculateRPN(rpn);
            return applyUnary(node, rhs);
          }
          case BINARY: {
            Object rhs = calculateRPN(rpn);
            Object lhs = calculateRPN(rpn);
            return applyBinary(node, lhs, rhs);
          }
        }
      } else if (isConstant(node)) {
        return constantValue(node);
      } else if (isIdentifier(node)) {
        return lookupIdentifier(node);
      } else {
        String typename = node.getElementType() != null ? node.getElementType().toString() : "<null>";
        throw new CalculationException("Unexpected AST Node type " + typename);
      }
    }
    return false;
  }

  @NotNull
  private HaxeOperatorPrecedenceTable.Arity getArity(@NotNull ASTNode node)
    throws CalculationException {
    HaxeOperatorPrecedenceTable.Arity arity = HaxeOperatorPrecedenceTable.getArity(node.getElementType());
    if (null == arity) {
      throw new CalculationException("NULL arity from node: '" + node.toString() + "'.");
    }

    // This could just as well be done in the routine above... Doing it here makes that one more understandable.
    switch(arity) {
      case UNARY:
      case BINARY:
        break;
      default:
        String msg = "Unexpected arity of " + arity.toString() + " from operator '" + node.toString() + "'.";
        throw new CalculationException(msg);
    }
    return arity;
  }

  @NotNull
  private Object constantValue(ASTNode node) throws CalculationException {
    if (isTrueKeyword(node))        { return new Boolean(true); }
    if (isFalseKeyword(node))       { return new Boolean(false); }
    if (isString(node))             { return new String(node.getText()); }
    if (isNumber(node))             { return new Float(node.getText()); }

    throw new CalculationException("Unrecognized value token: " + node.toString());
  }

  @NotNull
  private Object identifierValue(String s) throws CalculationException {
    if (KTRUE.toString().equals(s))   { return new Boolean(true); }
    if (KFALSE.toString().equals(s))  { return new Boolean(false); }

    Float result = new Float(0);
    if (isFloat(s,result))            { return result; }

    // XXX: Need to de-quote strings? (and recurse?)

    return s;
  }

  @NotNull
  private Object lookupIdentifier(ASTNode identifier) throws CalculationException {
    if (identifier == null) {
      return new Boolean(false);
    }
    if (context == null) {
      return SDK_DEFINES.contains(identifier);
    }
    String[] definitions = null;
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final Object userData = context.getUserData(DEFINES_KEY);
      if (userData instanceof String) {
        definitions = ((String)userData).split(",");
      }
    }
    else {
      definitions = HaxeProjectSettings.getInstance(context).getUserCompilerDefinitions();
    }
    String name = identifier.getText();
    if (definitions != null) {
      for (String possible : definitions) {
        // Dashes are subtraction operators, so definitions (on the command line) that
        // contain dashes are mapped to an equivalent using underscores (when looking up definitions).
        possible.replaceAll("-", "_");
        if (possible.startsWith(name)) {
          if (possible.length() == name.length()) {
            return new Boolean(true);
          } else if (possible.charAt(name.length()) == '=') {
            return identifierValue(possible.substring(name.length()+1));
          }
        }
      }
    }
    return new Boolean(false);
  }


  // Parodies Haxe parser is_true function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1596
  private boolean objectIsTrue(Object o) {
    if (o == null)            { return false; }
    if (o instanceof Boolean) { return (Boolean)o; }
    if (o instanceof Float)   { return !((Float)o == 0.0); }
    if (o instanceof String)  { return !((String)o).isEmpty(); }
    return true;
  }

  // Parodies Haxe parser cmp function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1600
  private int objectCompare(Object lhs, Object rhs) throws CompareException {
    if (lhs == null && rhs == null) { return 0; }
    if (lhs instanceof Boolean && rhs instanceof Boolean) { return ((Boolean)lhs).compareTo((Boolean)rhs); }
    if (lhs instanceof String && rhs instanceof String)   { return ((String)lhs).compareTo((String)rhs); }

    // For String vs Float, convert the strings to floats.  Errors converting are thrown past this function.
    if (lhs instanceof String  && rhs instanceof Float)  { lhs = Float.valueOf((String)lhs); }
    if (lhs instanceof Float   && rhs instanceof String) { rhs = Float.valueOf((String)rhs); }

    if (lhs instanceof Float && rhs instanceof Float) {
      // To get the same behavior as OCaml, NaN needs to be treated as less than all other numbers,
      // rather than larger, as Java likes to do it.
      int result = ((Float)lhs).compareTo((Float)rhs);
      if (((Float)lhs).isNaN()) { result = -result; }
      if (((Float)rhs).isNaN()) { result = -result; }
      return result;
    }

    throw new CompareException("Invalid value comparison between '"
                                   + lhs.toString() + "' and '" + rhs.toString() + "'.");
  }

  // Parodies Haxe parser eval function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1619
  @NotNull
  private Object applyUnary(ASTNode op, Object value) throws CalculationException {
    IElementType optype = op.getElementType();
    if (optype.equals(ONOT))  { return !objectIsTrue(value); }
    throw new CalculationException("Unexpected unary operator encountered: " + op.toString());
  }

  // Parodies Haxe parser eval function at lines 1617, 1618, and 1621-1634
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1617
  @NotNull
  private Object applyBinary(ASTNode op, Object lhs, Object rhs) throws CalculationException {
    IElementType optype = op.getElementType();
    try {
      if (optype.equals(OCOND_AND))            { return objectIsTrue(lhs) && objectIsTrue(rhs); }
      if (optype.equals(OCOND_OR))             { return objectIsTrue(lhs) || objectIsTrue(rhs); }
      if (optype.equals(OEQ))                  { return objectCompare(lhs, rhs) == 0; }
      if (optype.equals(ONOT_EQ))              { return objectCompare(lhs, rhs) != 0; }
      if (optype.equals(OGREATER))             { return objectCompare(lhs, rhs) >  0; }
      if (optype.equals(OGREATER_OR_EQUAL))    { return objectCompare(lhs, rhs) >= 0; }
      if (optype.equals(OLESS_OR_EQUAL))       { return objectCompare(lhs, rhs) <= 0; }
      if (optype.equals(OLESS))                { return objectCompare(lhs, rhs) <  0; }
      throw new CalculationException("Unexpected operator when comparing '"
                                     + lhs.toString() + " " + optype.toString() + " " + rhs.toString() + "'.");
    } catch (CompareException e) {
      // parser eval#1625 maps any calculation failures to false.
      return new Boolean(false);
    }
  }


  public static class CalculationException extends Exception {
    public CalculationException(String message) {
      super(message);
    }
  }

  public static class CompareException extends Exception {
    public CompareException(String message) {
      super(message);
    }
  }

}
