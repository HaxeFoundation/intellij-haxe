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
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Stack;
import java.util.regex.Pattern;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeGeneratedParserUtilBase extends GeneratedParserUtilBase {
  private static boolean whiteSpaceSkipped = false;

  public static final Pattern StringIdentifier = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");

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


  public static boolean isValidStringIdentifier(PsiBuilder builder_, int level_) {
    IElementType elementType = builder_.rawLookup(0);
    if (elementType == REGULAR_STRING_PART) {
      String text = builder_.getTokenText();
        return text != null && StringIdentifier.matcher(text).matches();
    }
    return false;
  }

  public static boolean consumeXmlTextIfQuote(PsiBuilder builder_, int level_) {
    return xmlTextIs(builder_, level_, "\"", true);
  }
  public static boolean consumeXmlTextIsMoreThan(PsiBuilder builder_, int level_) {
    return xmlTextIs(builder_, level_, ">", true);
  }
  public static boolean checkXmlTextIsMoreThan(PsiBuilder builder_, int level_) {
    return xmlTextIs(builder_, level_, ">", false);
  }


  private static boolean xmlTextIs(PsiBuilder builder_, int level_, String value, boolean consume) {
    IElementType elementType = builder_.rawLookup(0);
    if (elementType == XML_TEXT) {
      String text = builder_.getTokenText();
         if(text != null && text.equals(value)) {
           if(!consume) return true;
           return consumeToken(builder_, XML_TEXT);
         }
    }
    return false;
  }

  /**
   * Make a semi-colon optional in the case that it's preceded by a block statement.
   *
   */
  public static boolean semicolonUnlessPrecededByStatement(PsiBuilder builder_, int level) {
    return semicolonUnlessPrecededByStatement(builder_, level, true);
  }
  private static boolean semicolonUnlessPrecededByStatement(PsiBuilder builder_, int level, boolean showError) {
    if (consumeTokenFast(builder_, OSEMI)) {
      return true;
    }

    // ignore rule if part of postfix (postfix is parsed using the "left" keyword and will include expression
    // to its left and so we dont want/expect a semi between postfix and expression.
    if(builder_.getTokenType() == OPLUS_PLUS || builder_.getTokenType() == OMINUS_MINUS || builder_.getTokenType() == ONOT) {
      return true;
    }

    int i = -1;
    IElementType previousType = builder_.rawLookup(i);
    while (null != previousType && isWhitespaceOrComment(builder_, previousType)) {
      previousType = builder_.rawLookup(--i);
    }
    // ignore semicolon requrement for object literals
    // and xml literals (the rule is that values structured as blocks does not need semicolon)
    if (previousType == HaxeTokenTypes.PRCURLY
            || previousType == XML_TAG_END
            || previousType == HaxeTokenTypes.OSEMI) {
      return true;
    }
    /*
      macro value expressions can be "normal" expressions but should be treated as a single value
      so the same way an string or int argument does not need a trailing ; in a method call
      a macro value like  `macro var c = "test"` should not have a ; at the ned either.
     */

    Stack<Boolean> stack = getFlagStack(builder_, SEMICOLON_RULE_STATE);
    if (!stack.isEmpty()) {
      Boolean value = stack.peek();
      if (value != null) {
        return !value;
      }
    }


    if(showError) {
      builder_.error(HaxeBundle.message("parsing.error.missing.semi.colon"));
    }
    return false;
  }

  /**
  * guarded statements followed by else or catch statement does not need semi, usually some kind of valueExpression
  * ex. `var x= if (true) 1 else 2;`
  * ex. `var x= try  someMethod()  catch someValue;`

  */
  public static boolean semicolonUnlessFollowedByElseOrCatchKeyword(PsiBuilder builder_, int level) {
    if (consumeTokenFast(builder_, OSEMI)) {
      return true;
    }
    int i = 0;
    IElementType nextType = builder_.rawLookup(i);
    while (null != nextType && isWhitespaceOrComment(builder_, nextType)) {
      nextType = builder_.rawLookup(i++);
    }

    //guarded statement does not need semicolon if followed by "else" or "catch"
    return nextType == KELSE || nextType == KCATCH;
  }
  public static boolean semicolonUnlessFollowedByElseOrCatchOrStatement(PsiBuilder builder_, int level) {
    if (semicolonUnlessPrecededByStatement(builder_, level, false)) return true;

    int i = 0;
    IElementType nextType = builder_.rawLookup(i);
    while (null != nextType && isWhitespaceOrComment(builder_, nextType)) {
      nextType = builder_.rawLookup(i++);
    }

    //guarded statement does not need semicolon if followed by "else" or "catch"
    if(nextType == KELSE || nextType == KCATCH) return true;
    builder_.error(HaxeBundle.message("parsing.error.missing.semi.colon"));
    return false;
  }




  // hopefully faster way to stop unnecessary parsing attempts when not reification
  public static boolean canBeReification(PsiBuilder builder_, int level) {
    IElementType type = builder_.rawLookup(0) ;
    return type == DOLLAR || type == MACRO_ID;
  }
  public static boolean incompleteMacroIdentifier(PsiBuilder builder_, int level) {
    IElementType type = builder_.rawLookup(0) ;
    if(type == DOLLAR) {
      builder_.error(HaxeBundle.message("parsing.error.missing.identifier"));
      consumeTokenFast(builder_, DOLLAR);
      return  true;
    }
    return false;
  }

  private static final com.intellij.openapi.util.Key<Stack<Boolean>> SEMICOLON_RULE_STATE = new Key<>("SEMICOLON_RULE_STATE");
  private static final com.intellij.openapi.util.Key<Stack<Boolean>> COLLECTION_INITIALIZER_STATE = new Key<>("COLLECTION_INITIALIZER_STATE");
  private static final com.intellij.openapi.util.Key<Stack<Boolean>> MAP_INIT_STATE = new Key<>("MAP_INIT_STATE");
  private static final com.intellij.openapi.util.Key<Integer> MAP_INIT_EXPRESSION_COUNTER = new Key<>("HAS_INIT_EXPRESSION_COUNTER");
  private static final com.intellij.openapi.util.Key<Boolean> MAP_INIT_EXPRESSION_STATE = new Key<>("MAP_INIT_EXPRESSION_STATE");

  private static @NotNull Stack<Boolean> getFlagStack(PsiBuilder builder_, Key<Stack<Boolean>> flag) {
    Stack<Boolean> stack = builder_.getUserData(flag);
    if (stack == null) {
      stack = new Stack<>();
      builder_.putUserData(flag, stack);
    }
    return stack;
  }


  public static boolean pushSemicolonRuleDisable(PsiBuilder builder, int level) {
    getFlagStack(builder, SEMICOLON_RULE_STATE).push(Boolean.FALSE);
   return true;
  }
  public static boolean pushSemicolonRuleEnable(PsiBuilder builder, int level) {
    getFlagStack(builder, SEMICOLON_RULE_STATE).push(Boolean.TRUE);
   return true;
  }
  public static boolean debugStop(PsiBuilder builder, int level) {
   return true;
  }

  public static boolean isSemicolonRuleEnabled(PsiBuilder builder, int level) {
    Stack<Boolean> stack = getFlagStack(builder, SEMICOLON_RULE_STATE);
    if (stack.isEmpty())  return true;
    Boolean peeked = stack.peek();
    if (peeked != null) {
      return peeked;
    }
    return true;
  }

  /*
   * this is a toggle to allow or deny  the parser  from parsing expressions as collections (array/map)
   *  this is to  prevent the parser from consuming array pattern matchers in Switch-case expression as array/map literals
   */
  public static boolean pushCollectionInitializersDeny(PsiBuilder builder, int level) {
    getFlagStack(builder, COLLECTION_INITIALIZER_STATE).push(Boolean.FALSE);
   return true;
  }

  /*
   * Enum extractors can contain collection initializers  so we must be able to toggle this on while inside a switch case
   */
  public static boolean pushCollectionInitializersAllow(PsiBuilder builder, int level) {
    getFlagStack(builder, COLLECTION_INITIALIZER_STATE).push(Boolean.TRUE);
   return true;
  }

  public static boolean isInitializersAllowed(PsiBuilder builder, int level) {
    Stack<Boolean> stack = getFlagStack(builder, COLLECTION_INITIALIZER_STATE);
    if (stack.isEmpty())  return true;
    Boolean peeked = stack.peek();
    if (peeked != null) {
      return peeked;
    }
    return true;
  }

  /*
   * Array and map initializers can look very similar  but are differentiated by the  use of mapInitializer Expressions
   * Since initializers can be complex with lots of haxe code  we have this state that we toggle and when active we count
   * mapInitializer expressions, if non is found then the element being parsed is not a map and probably an array.
   */
  public static boolean pushStateMapInitializerOrMacro(PsiBuilder builder, int level) {
    getFlagStack(builder, MAP_INIT_STATE).push(Boolean.TRUE);
   return true;
  }

  public static boolean startWithLowercaseOrUnderscoreCheck(PsiBuilder builder_, int level_) {
    String text = builder_.getTokenText();
    if (text != null) {
      char c = text.charAt(0);
      if (c == '_') return true;
      return Character.isLowerCase(c);
    }
    return false;
  }
  public static boolean startWithUppercaseCheck(PsiBuilder builder_, int level_) {
    String text = builder_.getTokenText();
      if (text != null) {
          char c = text.charAt(0);
          return Character.isUpperCase(c);
        }
      return false;
    }




  /**
   *   map init expressions (a => b) are only allowed used inside Map init expression and in Macros
   *   this checks if we have pushed map init state to our stack
   */
  public static boolean isInMapInitOrMacroState(PsiBuilder builder, int level) {
    Stack<Boolean> stack = getFlagStack(builder, MAP_INIT_STATE);
    if (stack.isEmpty())  return false;
    Boolean peeked = stack.peek();
    if (peeked != null) {
      return peeked;
    }
   return true;
  }

  /*
   *  since a map init expression is "expression => expression", and "expression" contains mapInitExpression
   *  we add this state to prevent recursion and stop the resolver from parsing mapInitExpression if already inside a mapInitExpression
   */
  public static boolean isInMapInitExpressionState(PsiBuilder builder, int level) {
    Boolean data = builder.getUserData(MAP_INIT_EXPRESSION_STATE);
    if (data == Boolean.TRUE)  return true;
   return false;
  }
  public static boolean pushInMapInitExpressionState(PsiBuilder builder, int level) {
    builder.putUserData(MAP_INIT_EXPRESSION_STATE, Boolean.TRUE);
   return true;
  }

  /*
   *  checks if we have consumed any MapInitExpressions (used to differentiate between arrays and maps)
   */
  public static boolean hasMapInitExpressions(PsiBuilder builder, int level) {
    Integer data = builder.getUserData(MAP_INIT_EXPRESSION_COUNTER);
    if (data != null && data > 0) return true;
   return false;
  }
  /*
   *  adds to the MapInitExpression counter (used to differentiate between arrays and maps)
   */
  public static boolean addInitExpression(PsiBuilder builder, int level) {
    Integer data = builder.getUserData(MAP_INIT_EXPRESSION_COUNTER);
    if (data == null ) data = 0;
    builder.putUserData(MAP_INIT_EXPRESSION_COUNTER, ++data);
   return true;
  }
  /*
   *  resets the counter
   */
  public static boolean resetMapInitExpressionCounter(PsiBuilder builder, int level) {
    builder.putUserData(MAP_INIT_EXPRESSION_COUNTER, 0);
   return true;
  }

  public static final Hook<Boolean> POP_COLLECTION_INITIALIZERS_RULE =
    (builder, marker, param) -> {
      if (builder != null) {
        Stack<Boolean> stack = getFlagStack(builder, COLLECTION_INITIALIZER_STATE);
        if(!stack.isEmpty()) stack.pop();
        }
      return marker;
    };

  public static final Hook<Boolean> POP_SEMICOLON_RULE =
    (builder, marker, param) -> {
      Stack<Boolean> stack = getFlagStack(builder, SEMICOLON_RULE_STATE);
      if(!stack.isEmpty()) stack.pop();
      return marker;
    };

  /**
   * hook to remove Map init flag when we leave a map init expression
   */
  public static final Hook<Boolean> POP_MAP_INIT_OR_MACRO_STATE =
    (builder, marker, param) -> {
      Stack<Boolean> stack = getFlagStack(builder, MAP_INIT_STATE);
      if(!stack.isEmpty()) stack.pop();
      return marker;
    };


  public static final Hook<Boolean> CLEAR_IN_MAP_INIT_EXPRESSION =
    (builder, marker, param) -> {
      builder.putUserData(MAP_INIT_EXPRESSION_STATE, Boolean.FALSE);
      return marker;
    };



}