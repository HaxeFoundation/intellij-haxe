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
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.haxelib.HaxelibSemVer;
import com.intellij.plugins.haxe.lang.parser.HaxeAstFactory;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.plugins.haxe.lang.util.HaxeAstUtil.*;

/**
 * Condition that controls #if and #elseif conditional compilation (pre-processing) expressions.
 *
 * Created by ebishton on 3/23/17.
 */
@CustomLog
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

  //static {      // Take this out when finished debugging.
  //  log.setLevel(LogLevel.DEBUG);
  //}

  private final ArrayList<ASTNode> tokens = new ArrayList<ASTNode>();
  private boolean evaluated = false;    // Cleared when dirty.
  private boolean evalResult = false;   // Cleared when dirty.
  private StringBuilder builder = null;
  // diagnose(): cross-type compare failures surface instead of degrading to false
  private boolean strictComparisons = false;

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
      log.assertTrue(null == builder, "String builder is already allocated, but a string open quote token has been detected.");
      builder = new StringBuilder();
    } else if (REGULAR_STRING_PART == tokenType) {
      log.assertTrue(null != builder, "String token is parsed, but no StringBuilder has been allocated.");
      // XXX: Should we translate escape sequences to base tokens here?
      builder.append(chars);
    } else if (CLOSING_QUOTE == tokenType) {
      log.assertTrue(null != builder, "String close quote was parsed, but no StringBuilder has been allocated.");
      createToken(builder, REGULAR_STRING_PART);
      builder = null;
    } else {
      log.assertTrue(null == builder, "String builder exists, but a non-string token was encountered.");
      createToken(chars, tokenType);
    }
  }

  private boolean areTokensBalanced(IElementType leftToken, IElementType rightToken) {
    log.assertTrue(leftToken != rightToken, "Cannot balance tokens of the same type.");
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

  /**
   * The message the compiler would hard-error with for this condition, or
   * null when it evaluates cleanly. Comparisons run STRICT here (cross-type
   * failures surface instead of degrading to false); the legal null-poisoned
   * comparisons of undefined defines stay silent.
   */
  @Nullable
  public String diagnose(@Nullable Project context) {
    this.context = context;
    if (!isComplete()) return null;
    strictComparisons = true;
    try {
      Stack<ASTNode> postfix = infixToPostfix();
      evaluatePostfixTokens(postfix);
      return postfix.isEmpty() ? null : HaxeBundle.message("haxe.cc.diagnostic.invalid.condition");
    }
    catch (CalculationException e) {
      return e.getMessage();
    }
    finally {
      strictComparisons = false;
    }
  }

  /**
   * Rebuilds a condition from the raw text of its PPEXPRESSION tokens so
   * diagnostics can re-run it outside the lexer. The scanner mirrors the flex
   * COMPILER_CONDITIONAL rules; null for anything they would not produce -
   * and for hex/octal literals and strings containing escape sequences,
   * which the compiler accepts but this evaluator cannot yet compute
   * (nothing trustworthy to report there).
   */
  @Nullable
  public static HaxeConditionalExpression fromCondition(@NotNull String text) {
    HaxeConditionalExpression condition = new HaxeConditionalExpression(null);
    int i = 0;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (Character.isWhitespace(c)) {
        int start = i;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        condition.extend(text.substring(start, i), TokenType.WHITE_SPACE);
      }
      else if (Character.isLetter(c) || c == '_') {
        int start = i;
        // an identifier, dotted segments included (target.sys)
        while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_' || text.charAt(i) == '.')) i++;
        String word = text.substring(start, i);
        IElementType type = switch (word) {
          case "true" -> KTRUE;
          case "false" -> KFALSE;
          default -> ID;
        };
        condition.extend(word, type);
      }
      else if (Character.isDigit(c)) {
        int start = i;
        while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '.')) i++;
        String number = text.substring(start, i);
        boolean decimal = number.chars().allMatch(ch -> ch == '.' || Character.isDigit(ch));
        // the flex rules never yield a multi-dot number as one LITFLOAT
        // (1.2.3) - and Float.valueOf would throw on it during evaluation
        boolean multiDot = number.indexOf('.') != number.lastIndexOf('.');
        if (!decimal || multiDot) return null;
        condition.extend(number, number.contains(".") ? LITFLOAT : LITINT);
      }
      else if (c == '"' || c == '\'') {
        int end = text.indexOf(c, i + 1);
        if (end < 0) return null;
        String content = text.substring(i + 1, end);
        // an escape in the string (flex lexes it as ESCAPED_STRING_PART, and a
        // \" would mis-terminate this scan) - decline rather than mis-scan
        if (content.indexOf('\\') >= 0) return null;
        condition.extend(String.valueOf(c), OPEN_QUOTE);
        if (!content.isEmpty()) condition.extend(content, REGULAR_STRING_PART);
        condition.extend(String.valueOf(c), CLOSING_QUOTE);
        i = end + 1;
      }
      else {
        Map.Entry<String, IElementType> operator = operatorAt(text, i);
        if (operator == null) return null;
        condition.extend(operator.getKey(), operator.getValue());
        i += operator.getKey().length();
      }
    }
    return condition;
  }

  /** The operators a condition may hold, two-character ones first so they win over their one-character prefixes. */
  private static final List<Map.Entry<String, IElementType>> CONDITION_OPERATORS = List.of(
    Map.entry("==", OEQ),
    Map.entry("!=", ONOT_EQ),
    Map.entry(">=", OGREATER_OR_EQUAL),
    Map.entry("<=", OLESS_OR_EQUAL),
    Map.entry("&&", OCOND_AND),
    Map.entry("||", OCOND_OR),
    Map.entry("!", ONOT),
    Map.entry(">", OGREATER),
    Map.entry("<", OLESS),
    Map.entry("(", PLPAREN),
    Map.entry(")", PRPAREN));

  @Nullable
  private static Map.Entry<String, IElementType> operatorAt(String text, int offset) {
    for (Map.Entry<String, IElementType> operator : CONDITION_OPERATORS) {
      if (text.startsWith(operator.getKey(), offset)) return operator;
    }
    return null;
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
  private final static Set<String> SDK_DEFINES = new HashSet<String>(List.of(
    "macro"
  ));

  private static final String VERSION_FUNCTION_NAME = "version";
  /** Synthetic operand a folded {@code version("...")} call becomes; its text is the argument. */
  private static final IElementType VERSION_LITERAL = new IElementType("CC_VERSION_LITERAL", HaxeLanguage.INSTANCE);

  /** An undefined define's value (the compiler's TNull): falsy, poisons comparisons. */
  private static final Object NULL_VALUE = new Object() {
    @Override
    public String toString() {
      return "null";
    }
  };

  private static boolean isVersionLiteral(ASTNode node) {
    return node.getElementType() == VERSION_LITERAL;
  }
  /**
   * Test-only define injection: {@link #projectDefinitions} reads this map in
   * unit-test mode instead of the detection manager. Production code never
   * writes it — tests set it through {@code HaxeTestDefines}.
   */
  @TestOnly
  public static final Key<Map<String, String>> DEFINES_KEY = Key.create("haxe.test.defines");

  /** Evaluation Context */
  @Nullable
  private Project context;

  private boolean reevaluate(Project context) {
    this.context = context;
    boolean ret = false;
    if (isComplete()) {
      try {
        Stack<ASTNode> postfix = infixToPostfix();
        String postfixString = log.isDebugEnabled() ? tokensToString(postfix) : null;
        ret = objectIsTrue(evaluatePostfixTokens(postfix));
        if (log.isDebugEnabled()) {  // Don't create the strings unless we are debugging them...
          log.debug(toString() + " --> " + postfixString + " ==> " + (ret ? "true" : "false"));
        }
        if (!postfix.isEmpty()) {
          throw new CalculationException("Invalid Expression: Tokens left after calculating: " + postfix.toString());
        }
      } catch (CalculationException e) {
        // unevaluable conditions (the compiler hard-errors on them) are false;
        // a partial result computed before leftover tokens surfaced must not survive
        ret = false;
        String msg = "Error calculating conditional compiler expression '" + toString() + "'";
        // Add stack info if in debug mode.
        log.info( msg, log.isDebugEnabled() ? e : null );
      }
    }
    return ret;
  }

  /**
   * The only function the compiler accepts in a condition is
   * {@code version("literal")}. Each such call folds into ONE synthetic
   * operand so the shunting-yard sees a plain value; any other call shape
   * stays untouched and later fails evaluation to FALSE, mirroring the
   * compiler's hard error as an inactive branch.
   */
  private static ArrayList<ASTNode> foldVersionCalls(ArrayList<ASTNode> source) {
    ArrayList<ASTNode> folded = new ArrayList<>(source.size());
    int i = 0;
    while (i < source.size()) {
      int argIndex = versionCallArgumentAt(source, i);
      if (argIndex > 0) {
        folded.add(HaxeAstFactory.leaf(VERSION_LITERAL, source.get(argIndex).getText()));
        i = nextNonWhitespace(source, argIndex + 1) + 1;  // consume through ')'
      }
      else {
        folded.add(source.get(i));
        i++;
      }
    }
    return folded;
  }

  /**
   * When {@code start} opens the call shape ID "version", '(', one coalesced
   * string, ')' (whitespace between tokens allowed), returns the string
   * argument's index; -1 otherwise.
   */
  private static int versionCallArgumentAt(ArrayList<ASTNode> tokens, int start) {
    ASTNode name = tokens.get(start);
    if (!isIdentifier(name) || !VERSION_FUNCTION_NAME.equals(name.getText())) return -1;
    int open = nextNonWhitespace(tokens, start + 1);
    if (open < 0 || !isLeftParen(tokens.get(open))) return -1;
    int argument = nextNonWhitespace(tokens, open + 1);
    if (argument < 0 || !isString(tokens.get(argument))) return -1;
    int close = nextNonWhitespace(tokens, argument + 1);
    if (close < 0 || !isRightParen(tokens.get(close))) return -1;
    return argument;
  }

  private static int nextNonWhitespace(ArrayList<ASTNode> tokens, int start) {
    for (int i = start; i < tokens.size(); i++) {
      if (!isWhitespace(tokens.get(i))) return i;
    }
    return -1;
  }

  /**
   * Converts an infix expression into postfix (Reverse Polish) order.  (Re-orders and removes parenthesis.)
   * For example: !(cpp && js) -> cpp js && !
   *         and: (( cpp || js ) && (haxe-ver < 3))  -> cpp js || haxe-ver 3 < &&
   * See https://en.wikipedia.org/wiki/Reverse_Polish_notation
   */
  private Stack<ASTNode> infixToPostfix() throws CalculationException {
    // This is a simplified shunting-yard algorithm: http://https://en.wikipedia.org/wiki/Shunting-yard_algorithm
    Stack<ASTNode> postfixOutput = new Stack<ASTNode>();
    Stack<ASTNode> operatorStack = new Stack<ASTNode>();

    try {
      for (ASTNode token : foldVersionCalls(tokens)) {
        if (isWhitespace(token)) {
          continue;
        }
        if (isLiteral(token) || isStringQuote(token) || isString(token) || isVersionLiteral(token)) {
          postfixOutput.push(token);
        }
        else if (isLeftParen(token)) {
          operatorStack.push(token);
        }
        else if (isRightParen(token)) {
          boolean foundLeftParen = false;
          while (!operatorStack.isEmpty()) {
            ASTNode op = operatorStack.pop();
            if (!isLeftParen(op)) {
              postfixOutput.push(op);
            }
            else {
              foundLeftParen = true;
              break;
            }
          }
          if (operatorStack.isEmpty() && !foundLeftParen) {
            throw new CalculationException(HaxeBundle.message("haxe.cc.diagnostic.unmatched.right.paren"));
          }
        }
        else if (isCCOperator(token)) {
          while (!operatorStack.isEmpty()
                 && !isLeftParen(operatorStack.peek())  // Parens have the highest priority, but should not be considered for comparison.
                 && HaxeOperatorPrecedenceTable.shuntingYardCompare(token.getElementType(), operatorStack.peek().getElementType())) {
            postfixOutput.push(operatorStack.pop());
          }
          operatorStack.push(token);
        }
        else {
          throw new CalculationException("Couldn't process token '" + token.toString() + "' when converting to postfix.");
        }
      }
    } catch (HaxeOperatorPrecedenceTable.OperatorNotFoundException e) {
      log.warn("IntelliJ-Haxe plugin internal error: Unknown operator encountered while calculating compiler conditional exression:"
               + toString(), e);
      throw new CalculationException(e.toString());
    }

    // Anything left in the operator stack means an error.
    while(!operatorStack.isEmpty()) {
      ASTNode node = operatorStack.pop();
      if (isLeftParen(node)) {
        throw new CalculationException(HaxeBundle.message("haxe.cc.diagnostic.unmatched.left.paren"));
      } else {
        postfixOutput.push(node);
      }
    }

    return postfixOutput;
  }

  /**
   * Computes the value of a postfix expression - the condition's tokens
   * reordered so every operator FOLLOWS its operands ({@code cpp js &&} for
   * {@code cpp && js}), a shape that needs no parentheses or precedence
   * rules. (No relation to the PSI type HaxePostfixExpression - the language's
   * ++/-- suffix form.) The stack's top holds the expression's OUTERMOST operator; each
   * operator recursively evaluates its operand sub-expressions from the
   * tokens beneath it.
   */
  private Object evaluatePostfixTokens(Stack<ASTNode> postfix) throws CalculationException {
    while (!postfix.isEmpty()) {
      ASTNode node = postfix.tryPop();
      if (isCCOperator(node)) {
        switch (getArity(node)) {
          case UNARY: {
            Object rhs = evaluatePostfixTokens(postfix);
            return applyUnary(node, rhs);
          }
          case BINARY: {
            Object rhs = evaluatePostfixTokens(postfix);
            Object lhs = evaluatePostfixTokens(postfix);
            return applyBinary(node, lhs, rhs);
          }
        }
      } else if (isVersionLiteral(node)) {
        return versionValue(node);
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
  private static Object versionValue(ASTNode node) throws CalculationException {
    HaxelibSemVer version = HaxelibSemVer.parseCompilerVersion(node.getText());
    if (version == null) {
      throw new CalculationException(HaxeBundle.message("haxe.cc.diagnostic.invalid.version", node.getText()));
    }
    return version;
  }

  @NotNull
  private Object constantValue(ASTNode node) throws CalculationException {
    if (isTrueKeyword(node))        { return Boolean.TRUE; }
    if (isFalseKeyword(node))       { return Boolean.FALSE; }
    if (isString(node))             { return node.getText(); }
    if (isNumber(node))             { return Float.valueOf(node.getText()); }

    throw new CalculationException("Unrecognized value token: " + node.toString());
  }

  @NotNull
  private Object identifierValue(String s) throws CalculationException {
    if (KTRUE.toString().equals(s))   { return Boolean.TRUE; }
    if (KFALSE.toString().equals(s))  { return Boolean.FALSE; }

    FloatResult result = new FloatResult();
    if (isFloat(s,result))            { return result.result; }

    // De-quote strings and recurse...
    if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
      if (s.length() <= 2) {
        return new String();
      } else {
        return identifierValue(s.substring(1, s.length() - 1));
      }
    }

    return s;
  }

  @NotNull
  private Object lookupIdentifier(ASTNode identifier) throws CalculationException {
    if (identifier == null) {
      return NULL_VALUE;
    }
    if (context == null) {
      return SDK_DEFINES.contains(identifier.getText()) ? identifierValue(FLAG_DEFINE_VALUE) : NULL_VALUE;
    }
    String name = identifier.getText();

    Map<String, String> definitionMap = projectDefinitions(context);
    if (definitionMap.containsKey(name)) {
      String value = definitionMap.get(name);
      if (null == value || value.isEmpty()) {
        return identifierValue(FLAG_DEFINE_VALUE);
      } else {
        return identifierValue(value);
      }
    }
    return NULL_VALUE;
  }

  /**
   * The value the compiler gives a define set without one (`-D flag` ==
   * `-D flag=1`). Every define-collection site uses this for valueless
   * defines: a define is never a boolean, so flags stay comparable
   * (`#if (myVersion < "9.0.0")` must evaluate, not error).
   */
  public static final String FLAG_DEFINE_VALUE = "1";

  /**
   * The project's compiler defines exactly as conditional-compilation
   * evaluation sees them: the {@link #DEFINES_KEY} test injection in
   * unit-test mode, the detected build context otherwise. Every feature
   * keyed on "is X defined" (module-variant activation, CC) reads this one
   * source so they can never disagree.
   */
  @NotNull
  public static Map<String, String> projectDefinitions(@NotNull Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      Map<String, String> userData = project.getUserData(DEFINES_KEY);
      return userData != null ? userData : Map.of();
    }
    return HaxeDefineDetectionManager.getInstance(project).getAllDefinitions();
  }

  // Parodies Haxe parser is_true function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1596
  private boolean objectIsTrue(Object o) {
    if (o == null)            { return false; }
    if (o == NULL_VALUE)      { return false; }
    if (o instanceof Boolean) { return (Boolean)o; }
    if (o instanceof Float)   { return !((Float)o == 0.0); }
    if (o instanceof String)  { return !((String)o).isEmpty(); }
    return true;
  }

  // Parodies Haxe parser cmp function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1600
  private int objectCompare(Object lhs, Object rhs) throws CompareException, CalculationException {
    if (lhs == null && rhs == null) { return 0; }
    // a version compares with another version or a strict version string;
    // anything else the compiler rejects - unevaluable maps to false
    if (lhs instanceof HaxelibSemVer || rhs instanceof HaxelibSemVer) {
      HaxelibSemVer lhsVersion = asVersion(lhs);
      HaxelibSemVer rhsVersion = asVersion(rhs);
      if (lhsVersion == null || rhsVersion == null) {
        throw new CompareException(versionCompareError(lhsVersion == null ? lhs : rhs));
      }
      return lhsVersion.compareTo(rhsVersion);
    }
    if (lhs instanceof Boolean && rhs instanceof Boolean) { return ((Boolean)lhs).compareTo((Boolean)rhs); }
    if (lhs instanceof String && rhs instanceof String)   { return ((String)lhs).compareTo((String)rhs); }



    // For String vs Float, convert the strings to floats.  Errors converting are thrown past this function.
    if (lhs instanceof String  && rhs instanceof Float)  { lhs = identifierValue((String)lhs); }
    if (lhs instanceof Float   && rhs instanceof String) { rhs = identifierValue((String)rhs); }

    if (lhs instanceof Float && rhs instanceof Float) {
      // To get the same behavior as OCaml, NaN needs to be treated as less than all other numbers,
      // rather than larger, as Java likes to do it.
      int result = ((Float)lhs).compareTo((Float)rhs);
      if (((Float)lhs).isNaN()) { result = -result; }
      if (((Float)rhs).isNaN()) { result = -result; }
      return result;
    }

    HaxelibSemVer lhsSemVer = null;
    HaxelibSemVer rhsSemVer = null;
    if (lhs instanceof Float lhsFloat) lhsSemVer = HaxelibSemVer.create(lhsFloat);
    if (lhs instanceof String lhsString) lhsSemVer = HaxelibSemVer.create(lhsString);

    if (rhs instanceof Float rhsFloat) rhsSemVer = HaxelibSemVer.create(rhsFloat);
    if (rhs instanceof String rhsString) rhsSemVer = HaxelibSemVer.create(rhsString);

    if (lhsSemVer != null && rhsSemVer != null) {
      return lhsSemVer.compareTo(rhsSemVer);
    }


    throw new CompareException(HaxeBundle.message("haxe.cc.diagnostic.invalid.comparison", lhs, rhs));
  }

  @Nullable
  private static HaxelibSemVer asVersion(Object value) {
    if (value instanceof HaxelibSemVer version) return version;
    if (value instanceof String string) return HaxelibSemVer.parseCompilerVersion(string);
    return null;
  }

  /**
   * Why {@code bad} cannot stand in a version comparison. The usual mistake -
   * a shortened numeric version like a define set to 1.13 - gets a targeted
   * message with the completed form; everything else keeps the compiler's
   * wording.
   */
  private static String versionCompareError(Object bad) {
    String text = String.valueOf(bad);
    // a 1- or 2-part numeric version (1 / 1.13) - dots and digits only
    if (text.matches("\\d+(\\.\\d+)?")) {
      return HaxeBundle.message("haxe.cc.diagnostic.version.needs.three.parts", text, padToThreeParts(text));
    }
    if (bad instanceof String) {
      return HaxeBundle.message("haxe.cc.diagnostic.invalid.version", text);
    }
    return HaxeBundle.message("haxe.cc.diagnostic.version.compare.kind", kindName(bad));
  }

  private static String padToThreeParts(String version) {
    StringBuilder padded = new StringBuilder(version);
    // count the dots that are present; a full version core has two
    long dots = version.chars().filter(c -> c == '.').count();
    for (long i = dots; i < 2; i++) {
      padded.append(".0");
    }
    return padded.toString();
  }

  private static String kindName(Object value) {
    if (value instanceof Float) return "float";
    if (value instanceof Boolean) return "bool";
    if (value instanceof String) return "string";
    return String.valueOf(value);
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
      // an undefined define poisons comparisons: != is true, everything else
      // false (the compiler's TNull -> Exit handling) - never a compare error
      if (lhs == NULL_VALUE || rhs == NULL_VALUE) {
        return optype.equals(ONOT_EQ) ? Boolean.TRUE : Boolean.FALSE;
      }
      if (optype.equals(OEQ))                  { return objectCompare(lhs, rhs) == 0; }
      if (optype.equals(ONOT_EQ))              { return objectCompare(lhs, rhs) != 0; }
      if (optype.equals(OGREATER))             { return objectCompare(lhs, rhs) >  0; }
      if (optype.equals(OGREATER_OR_EQUAL))    { return objectCompare(lhs, rhs) >= 0; }
      if (optype.equals(OLESS_OR_EQUAL))       { return objectCompare(lhs, rhs) <= 0; }
      if (optype.equals(OLESS))                { return objectCompare(lhs, rhs) <  0; }
      throw new CalculationException("Unexpected operator when comparing '"
                                     + lhs.toString() + " " + optype.toString() + " " + rhs.toString() + "'.");
    } catch (CompareException e) {
      // the compiler hard-errors on these; evaluation degrades to false, but
      // diagnose() surfaces the message instead
      if (strictComparisons) throw new CalculationException(e.getMessage());
      return Boolean.FALSE;
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
