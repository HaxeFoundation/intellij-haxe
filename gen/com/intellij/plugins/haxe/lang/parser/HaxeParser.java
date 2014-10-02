/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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

// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.openapi.diagnostic.Logger;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HaxeParser implements PsiParser {

  public static final Logger LOG_ = Logger.getInstance("com.intellij.plugins.haxe.lang.parser.HaxeParser");

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, EXTENDS_SETS_);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    if (root_ == ABSTRACT_CLASS_DECLARATION) {
      result_ = abstractClassDeclaration(builder_, 0);
    }
    else if (root_ == ACCESS) {
      result_ = access(builder_, 0);
    }
    else if (root_ == ADDITIVE_EXPRESSION) {
      result_ = additiveExpression(builder_, 0);
    }
    else if (root_ == ANONYMOUS_FUNCTION_DECLARATION) {
      result_ = anonymousFunctionDeclaration(builder_, 0);
    }
    else if (root_ == ANONYMOUS_TYPE) {
      result_ = anonymousType(builder_, 0);
    }
    else if (root_ == ANONYMOUS_TYPE_BODY) {
      result_ = anonymousTypeBody(builder_, 0);
    }
    else if (root_ == ANONYMOUS_TYPE_FIELD) {
      result_ = anonymousTypeField(builder_, 0);
    }
    else if (root_ == ANONYMOUS_TYPE_FIELD_LIST) {
      result_ = anonymousTypeFieldList(builder_, 0);
    }
    else if (root_ == ARRAY_ACCESS_EXPRESSION) {
      result_ = arrayAccessExpression(builder_, 0);
    }
    else if (root_ == ARRAY_LITERAL) {
      result_ = arrayLiteral(builder_, 0);
    }
    else if (root_ == ASSIGN_EXPRESSION) {
      result_ = assignExpression(builder_, 0);
    }
    else if (root_ == ASSIGN_OPERATION) {
      result_ = assignOperation(builder_, 0);
    }
    else if (root_ == AUTO_BUILD_MACRO) {
      result_ = autoBuildMacro(builder_, 0);
    }
    else if (root_ == BIND_META) {
      result_ = bindMeta(builder_, 0);
    }
    else if (root_ == BIT_OPERATION) {
      result_ = bitOperation(builder_, 0);
    }
    else if (root_ == BITMAP_META) {
      result_ = bitmapMeta(builder_, 0);
    }
    else if (root_ == BITWISE_EXPRESSION) {
      result_ = bitwiseExpression(builder_, 0);
    }
    else if (root_ == BLOCK_STATEMENT) {
      result_ = blockStatement(builder_, 0);
    }
    else if (root_ == BREAK_STATEMENT) {
      result_ = breakStatement(builder_, 0);
    }
    else if (root_ == BUILD_MACRO) {
      result_ = buildMacro(builder_, 0);
    }
    else if (root_ == CALL_EXPRESSION) {
      result_ = callExpression(builder_, 0);
    }
    else if (root_ == CAST_EXPRESSION) {
      result_ = castExpression(builder_, 0);
    }
    else if (root_ == CATCH_STATEMENT) {
      result_ = catchStatement(builder_, 0);
    }
    else if (root_ == CLASS_BODY) {
      result_ = classBody(builder_, 0);
    }
    else if (root_ == CLASS_DECLARATION) {
      result_ = classDeclaration(builder_, 0);
    }
    else if (root_ == COMPARE_EXPRESSION) {
      result_ = compareExpression(builder_, 0);
    }
    else if (root_ == COMPARE_OPERATION) {
      result_ = compareOperation(builder_, 0);
    }
    else if (root_ == COMPONENT_NAME) {
      result_ = componentName(builder_, 0);
    }
    else if (root_ == CONTINUE_STATEMENT) {
      result_ = continueStatement(builder_, 0);
    }
    else if (root_ == CORE_API_META) {
      result_ = coreApiMeta(builder_, 0);
    }
    else if (root_ == CUSTOM_META) {
      result_ = customMeta(builder_, 0);
    }
    else if (root_ == DEBUG_META) {
      result_ = debugMeta(builder_, 0);
    }
    else if (root_ == DECLARATION_ATTRIBUTE) {
      result_ = declarationAttribute(builder_, 0);
    }
    else if (root_ == DEFAULT_CASE) {
      result_ = defaultCase(builder_, 0);
    }
    else if (root_ == DO_WHILE_STATEMENT) {
      result_ = doWhileStatement(builder_, 0);
    }
    else if (root_ == ENUM_BODY) {
      result_ = enumBody(builder_, 0);
    }
    else if (root_ == ENUM_CONSTRUCTOR_PARAMETERS) {
      result_ = enumConstructorParameters(builder_, 0);
    }
    else if (root_ == ENUM_DECLARATION) {
      result_ = enumDeclaration(builder_, 0);
    }
    else if (root_ == ENUM_VALUE_DECLARATION) {
      result_ = enumValueDeclaration(builder_, 0);
    }
    else if (root_ == EXPRESSION) {
      result_ = expression(builder_, 0);
    }
    else if (root_ == EXPRESSION_LIST) {
      result_ = expressionList(builder_, 0);
    }
    else if (root_ == EXTERN_CLASS_DECLARATION) {
      result_ = externClassDeclaration(builder_, 0);
    }
    else if (root_ == EXTERN_CLASS_DECLARATION_BODY) {
      result_ = externClassDeclarationBody(builder_, 0);
    }
    else if (root_ == EXTERN_FUNCTION_DECLARATION) {
      result_ = externFunctionDeclaration(builder_, 0);
    }
    else if (root_ == EXTERN_OR_PRIVATE) {
      result_ = externOrPrivate(builder_, 0);
    }
    else if (root_ == FAKE_ENUM_META) {
      result_ = fakeEnumMeta(builder_, 0);
    }
    else if (root_ == FINAL_META) {
      result_ = finalMeta(builder_, 0);
    }
    else if (root_ == FOR_STATEMENT) {
      result_ = forStatement(builder_, 0);
    }
    else if (root_ == FUNCTION_DECLARATION_WITH_ATTRIBUTES) {
      result_ = functionDeclarationWithAttributes(builder_, 0);
    }
    else if (root_ == FUNCTION_LITERAL) {
      result_ = functionLiteral(builder_, 0);
    }
    else if (root_ == FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES) {
      result_ = functionPrototypeDeclarationWithAttributes(builder_, 0);
    }
    else if (root_ == FUNCTION_TYPE) {
      result_ = functionType(builder_, 0);
    }
    else if (root_ == GENERIC_LIST_PART) {
      result_ = genericListPart(builder_, 0);
    }
    else if (root_ == GENERIC_PARAM) {
      result_ = genericParam(builder_, 0);
    }
    else if (root_ == GETTER_META) {
      result_ = getterMeta(builder_, 0);
    }
    else if (root_ == HACK_META) {
      result_ = hackMeta(builder_, 0);
    }
    else if (root_ == IDENTIFIER) {
      result_ = identifier(builder_, 0);
    }
    else if (root_ == IF_STATEMENT) {
      result_ = ifStatement(builder_, 0);
    }
    else if (root_ == IMPORT_STATEMENT_REGULAR) {
      result_ = importStatementRegular(builder_, 0);
    }
    else if (root_ == IMPORT_STATEMENT_WITH_IN_SUPPORT) {
      result_ = importStatementWithInSupport(builder_, 0);
    }
    else if (root_ == IMPORT_STATEMENT_WITH_WILDCARD) {
      result_ = importStatementWithWildcard(builder_, 0);
    }
    else if (root_ == INHERIT) {
      result_ = inherit(builder_, 0);
    }
    else if (root_ == INHERIT_LIST) {
      result_ = inheritList(builder_, 0);
    }
    else if (root_ == INTERFACE_BODY) {
      result_ = interfaceBody(builder_, 0);
    }
    else if (root_ == INTERFACE_DECLARATION) {
      result_ = interfaceDeclaration(builder_, 0);
    }
    else if (root_ == ITERABLE) {
      result_ = iterable(builder_, 0);
    }
    else if (root_ == ITERATOR_EXPRESSION) {
      result_ = iteratorExpression(builder_, 0);
    }
    else if (root_ == JS_REQUIRE_META) {
      result_ = jsRequireMeta(builder_, 0);
    }
    else if (root_ == KEEP_META) {
      result_ = keepMeta(builder_, 0);
    }
    else if (root_ == LITERAL_EXPRESSION) {
      result_ = literalExpression(builder_, 0);
    }
    else if (root_ == LOCAL_FUNCTION_DECLARATION) {
      result_ = localFunctionDeclaration(builder_, 0);
    }
    else if (root_ == LOCAL_VAR_DECLARATION) {
      result_ = localVarDeclaration(builder_, 0);
    }
    else if (root_ == LOCAL_VAR_DECLARATION_PART) {
      result_ = localVarDeclarationPart(builder_, 0);
    }
    else if (root_ == LOGIC_AND_EXPRESSION) {
      result_ = logicAndExpression(builder_, 0);
    }
    else if (root_ == LOGIC_OR_EXPRESSION) {
      result_ = logicOrExpression(builder_, 0);
    }
    else if (root_ == LONG_TEMPLATE_ENTRY) {
      result_ = longTemplateEntry(builder_, 0);
    }
    else if (root_ == MACRO_CLASS) {
      result_ = macroClass(builder_, 0);
    }
    else if (root_ == MACRO_CLASS_LIST) {
      result_ = macroClassList(builder_, 0);
    }
    else if (root_ == MACRO_META) {
      result_ = macroMeta(builder_, 0);
    }
    else if (root_ == META_KEY_VALUE) {
      result_ = metaKeyValue(builder_, 0);
    }
    else if (root_ == META_META) {
      result_ = metaMeta(builder_, 0);
    }
    else if (root_ == MULTIPLICATIVE_EXPRESSION) {
      result_ = multiplicativeExpression(builder_, 0);
    }
    else if (root_ == NATIVE_META) {
      result_ = nativeMeta(builder_, 0);
    }
    else if (root_ == NEW_EXPRESSION) {
      result_ = newExpression(builder_, 0);
    }
    else if (root_ == NO_DEBUG_META) {
      result_ = noDebugMeta(builder_, 0);
    }
    else if (root_ == NS_META) {
      result_ = nsMeta(builder_, 0);
    }
    else if (root_ == OBJECT_LITERAL) {
      result_ = objectLiteral(builder_, 0);
    }
    else if (root_ == OBJECT_LITERAL_ELEMENT) {
      result_ = objectLiteralElement(builder_, 0);
    }
    else if (root_ == OVERLOAD_META) {
      result_ = overloadMeta(builder_, 0);
    }
    else if (root_ == PACKAGE_STATEMENT) {
      result_ = packageStatement(builder_, 0);
    }
    else if (root_ == PARAMETER) {
      result_ = parameter(builder_, 0);
    }
    else if (root_ == PARAMETER_LIST) {
      result_ = parameterList(builder_, 0);
    }
    else if (root_ == PARENTHESIZED_EXPRESSION) {
      result_ = parenthesizedExpression(builder_, 0);
    }
    else if (root_ == PREFIX_EXPRESSION) {
      result_ = prefixExpression(builder_, 0);
    }
    else if (root_ == PRIVATE_KEY_WORD) {
      result_ = privateKeyWord(builder_, 0);
    }
    else if (root_ == PROPERTY_ACCESSOR) {
      result_ = propertyAccessor(builder_, 0);
    }
    else if (root_ == PROPERTY_DECLARATION) {
      result_ = propertyDeclaration(builder_, 0);
    }
    else if (root_ == PROTECTED_META) {
      result_ = protectedMeta(builder_, 0);
    }
    else if (root_ == REFERENCE_EXPRESSION) {
      result_ = referenceExpression(builder_, 0);
    }
    else if (root_ == REGULAR_EXPRESSION_LITERAL) {
      result_ = regularExpressionLiteral(builder_, 0);
    }
    else if (root_ == REQUIRE_META) {
      result_ = requireMeta(builder_, 0);
    }
    else if (root_ == RETURN_STATEMENT) {
      result_ = returnStatement(builder_, 0);
    }
    else if (root_ == RETURN_STATEMENT_WITHOUT_SEMICOLON) {
      result_ = returnStatementWithoutSemicolon(builder_, 0);
    }
    else if (root_ == SETTER_META) {
      result_ = setterMeta(builder_, 0);
    }
    else if (root_ == SHIFT_EXPRESSION) {
      result_ = shiftExpression(builder_, 0);
    }
    else if (root_ == SHIFT_OPERATOR) {
      result_ = shiftOperator(builder_, 0);
    }
    else if (root_ == SHIFT_RIGHT_OPERATOR) {
      result_ = shiftRightOperator(builder_, 0);
    }
    else if (root_ == SHORT_TEMPLATE_ENTRY) {
      result_ = shortTemplateEntry(builder_, 0);
    }
    else if (root_ == SIMPLE_META) {
      result_ = simpleMeta(builder_, 0);
    }
    else if (root_ == STRING_LITERAL_EXPRESSION) {
      result_ = stringLiteralExpression(builder_, 0);
    }
    else if (root_ == SUFFIX_EXPRESSION) {
      result_ = suffixExpression(builder_, 0);
    }
    else if (root_ == SUPER_EXPRESSION) {
      result_ = superExpression(builder_, 0);
    }
    else if (root_ == SWITCH_BLOCK) {
      result_ = switchBlock(builder_, 0);
    }
    else if (root_ == SWITCH_CASE) {
      result_ = switchCase(builder_, 0);
    }
    else if (root_ == SWITCH_CASE_BLOCK) {
      result_ = switchCaseBlock(builder_, 0);
    }
    else if (root_ == SWITCH_CASE_EXPRESSION) {
      result_ = switchCaseExpression(builder_, 0);
    }
    else if (root_ == SWITCH_STATEMENT) {
      result_ = switchStatement(builder_, 0);
    }
    else if (root_ == TERNARY_EXPRESSION) {
      result_ = ternaryExpression(builder_, 0);
    }
    else if (root_ == THIS_EXPRESSION) {
      result_ = thisExpression(builder_, 0);
    }
    else if (root_ == THROW_STATEMENT) {
      result_ = throwStatement(builder_, 0);
    }
    else if (root_ == TRY_STATEMENT) {
      result_ = tryStatement(builder_, 0);
    }
    else if (root_ == TYPE) {
      result_ = type(builder_, 0);
    }
    else if (root_ == TYPE_EXTENDS) {
      result_ = typeExtends(builder_, 0);
    }
    else if (root_ == TYPE_LIST) {
      result_ = typeList(builder_, 0);
    }
    else if (root_ == TYPE_LIST_PART) {
      result_ = typeListPart(builder_, 0);
    }
    else if (root_ == TYPE_OR_ANONYMOUS) {
      result_ = typeOrAnonymous(builder_, 0);
    }
    else if (root_ == TYPE_PARAM) {
      result_ = typeParam(builder_, 0);
    }
    else if (root_ == TYPE_TAG) {
      result_ = typeTag(builder_, 0);
    }
    else if (root_ == TYPEDEF_DECLARATION) {
      result_ = typedefDeclaration(builder_, 0);
    }
    else if (root_ == UNREFLECTIVE_META) {
      result_ = unreflectiveMeta(builder_, 0);
    }
    else if (root_ == UNSIGNED_SHIFT_RIGHT_OPERATOR) {
      result_ = unsignedShiftRightOperator(builder_, 0);
    }
    else if (root_ == USING_STATEMENT) {
      result_ = usingStatement(builder_, 0);
    }
    else if (root_ == VAR_DECLARATION) {
      result_ = varDeclaration(builder_, 0);
    }
    else if (root_ == VAR_DECLARATION_PART) {
      result_ = varDeclarationPart(builder_, 0);
    }
    else if (root_ == VAR_INIT) {
      result_ = varInit(builder_, 0);
    }
    else if (root_ == WHILE_STATEMENT) {
      result_ = whileStatement(builder_, 0);
    }
    else if (root_ == WILDCARD) {
      result_ = wildcard(builder_, 0);
    }
    else {
      result_ = parse_root_(root_, builder_, 0);
    }
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
    return builder_.getTreeBuilt();
  }

  protected boolean parse_root_(final IElementType root_, final PsiBuilder builder_, final int level_) {
    return haxeFile(builder_, level_ + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(ADDITIVE_EXPRESSION, ARRAY_ACCESS_EXPRESSION, ARRAY_LITERAL, ASSIGN_EXPRESSION,
      BITWISE_EXPRESSION, CALL_EXPRESSION, CAST_EXPRESSION, COMPARE_EXPRESSION,
      EXPRESSION, FUNCTION_LITERAL, ITERATOR_EXPRESSION, LITERAL_EXPRESSION,
      LOGIC_AND_EXPRESSION, LOGIC_OR_EXPRESSION, MULTIPLICATIVE_EXPRESSION, NEW_EXPRESSION,
      OBJECT_LITERAL, PARENTHESIZED_EXPRESSION, PREFIX_EXPRESSION, REFERENCE_EXPRESSION,
      REGULAR_EXPRESSION_LITERAL, SHIFT_EXPRESSION, STRING_LITERAL_EXPRESSION, SUFFIX_EXPRESSION,
      SUPER_EXPRESSION, SWITCH_CASE_EXPRESSION, TERNARY_EXPRESSION, THIS_EXPRESSION),
    create_token_set_(LITERAL_EXPRESSION, REGULAR_EXPRESSION_LITERAL),
  };

  /* ********************************************************** */
  // macroClassList? privateKeyWord? 'abstract' componentName genericParam? ('(' type ')')? (('from' | 'to') type)* '{' classBody '}'
  public static boolean abstractClassDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<abstract class declaration>");
    result_ = abstractClassDeclaration_0(builder_, level_ + 1);
    result_ = result_ && abstractClassDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KABSTRACT);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, abstractClassDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, abstractClassDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, abstractClassDeclaration_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && report_error_(builder_, classBody(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, ABSTRACT_CLASS_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // macroClassList?
  private static boolean abstractClassDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_0")) return false;
    macroClassList(builder_, level_ + 1);
    return true;
  }

  // privateKeyWord?
  private static boolean abstractClassDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_1")) return false;
    privateKeyWord(builder_, level_ + 1);
    return true;
  }

  // genericParam?
  private static boolean abstractClassDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_4")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // ('(' type ')')?
  private static boolean abstractClassDeclaration_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_5")) return false;
    abstractClassDeclaration_5_0(builder_, level_ + 1);
    return true;
  }

  // '(' type ')'
  private static boolean abstractClassDeclaration_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_5_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && type(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (('from' | 'to') type)*
  private static boolean abstractClassDeclaration_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_6")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!abstractClassDeclaration_6_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "abstractClassDeclaration_6", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ('from' | 'to') type
  private static boolean abstractClassDeclaration_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_6_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = abstractClassDeclaration_6_0_0(builder_, level_ + 1);
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'from' | 'to'
  private static boolean abstractClassDeclaration_6_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "abstractClassDeclaration_6_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KFROM);
    if (!result_) result_ = consumeToken(builder_, KTO);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'public' | 'private'
  public static boolean access(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "access")) return false;
    if (!nextTokenIs(builder_, "<access>", KPRIVATE, KPUBLIC)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<access>");
    result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    exit_section_(builder_, level_, marker_, ACCESS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // ('+' | '-') multiplicativeExpressionWrapper
  public static boolean additiveExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression")) return false;
    if (!nextTokenIs(builder_, "<additive expression>", OPLUS, OMINUS)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<additive expression>");
    result_ = additiveExpression_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && multiplicativeExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ADDITIVE_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // '+' | '-'
  private static boolean additiveExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OPLUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // multiplicativeExpressionWrapper additiveExpression*
  static boolean additiveExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = multiplicativeExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && additiveExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // additiveExpression*
  private static boolean additiveExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!additiveExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "additiveExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // 'function' '(' parameterList? ')' typeTag? 'untyped'? '{' '}'
  public static boolean anonymousFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, KFUNCTION)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KFUNCTION);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, anonymousFunctionDeclaration_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, anonymousFunctionDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, anonymousFunctionDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, ANONYMOUS_FUNCTION_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // parameterList?
  private static boolean anonymousFunctionDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousFunctionDeclaration_2")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean anonymousFunctionDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousFunctionDeclaration_4")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean anonymousFunctionDeclaration_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousFunctionDeclaration_5")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // '{' anonymousTypeBody '}'
  public static boolean anonymousType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousType")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLCURLY);
    result_ = result_ && anonymousTypeBody(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, ANONYMOUS_TYPE, result_);
    return result_;
  }

  /* ********************************************************** */
  // extendedAnonymousTypeBody | simpleAnonymousTypeBody | interfaceBody
  public static boolean anonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeBody")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<anonymous type body>");
    result_ = extendedAnonymousTypeBody(builder_, level_ + 1);
    if (!result_) result_ = simpleAnonymousTypeBody(builder_, level_ + 1);
    if (!result_) result_ = interfaceBody(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ANONYMOUS_TYPE_BODY, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // componentName typeTag
  public static boolean anonymousTypeField(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeField")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && typeTag(builder_, level_ + 1);
    exit_section_(builder_, marker_, ANONYMOUS_TYPE_FIELD, result_);
    return result_;
  }

  /* ********************************************************** */
  // anonymousTypeField (',' anonymousTypeField)*
  public static boolean anonymousTypeFieldList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeFieldList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = anonymousTypeField(builder_, level_ + 1);
    result_ = result_ && anonymousTypeFieldList_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, ANONYMOUS_TYPE_FIELD_LIST, result_);
    return result_;
  }

  // (',' anonymousTypeField)*
  private static boolean anonymousTypeFieldList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeFieldList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!anonymousTypeFieldList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "anonymousTypeFieldList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' anonymousTypeField
  private static boolean anonymousTypeFieldList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeFieldList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && anonymousTypeField(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '[' expression? ']'
  public static boolean arrayAccessExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "arrayAccessExpression")) return false;
    if (!nextTokenIs(builder_, PLBRACK)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, PLBRACK);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, arrayAccessExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRBRACK) && result_;
    exit_section_(builder_, level_, marker_, ARRAY_ACCESS_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // expression?
  private static boolean arrayAccessExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "arrayAccessExpression_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // '[' expressionList? ']'
  public static boolean arrayLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "arrayLiteral")) return false;
    if (!nextTokenIs(builder_, PLBRACK)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLBRACK);
    result_ = result_ && arrayLiteral_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRBRACK);
    exit_section_(builder_, marker_, ARRAY_LITERAL, result_);
    return result_;
  }

  // expressionList?
  private static boolean arrayLiteral_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "arrayLiteral_1")) return false;
    expressionList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // assignOperation iteratorExpressionWrapper
  public static boolean assignExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignExpression")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<assign expression>");
    result_ = assignOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && iteratorExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ASSIGN_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // iteratorExpressionWrapper assignExpression*
  static boolean assignExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = iteratorExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && assignExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // assignExpression*
  private static boolean assignExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!assignExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "assignExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // '=' | '+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' | '<<=' | '>>=' | '>>>='
  public static boolean assignOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignOperation")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<assign operation>");
    result_ = consumeToken(builder_, OASSIGN);
    if (!result_) result_ = consumeToken(builder_, OPLUS_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OMINUS_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OMUL_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OQUOTIENT_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OREMAINDER_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OBIT_AND_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OBIT_OR_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OBIT_XOR_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_LEFT_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_RIGHT_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, ">>>=");
    exit_section_(builder_, level_, marker_, ASSIGN_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '@:autoBuild' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean autoBuildMacro(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KAUTOBUILD);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, referenceExpression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, autoBuildMacro_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, AUTO_BUILD_MACRO, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean autoBuildMacro_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro_3")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!autoBuildMacro_3_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "autoBuildMacro_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean autoBuildMacro_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:bind'
  public static boolean bindMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bindMeta")) return false;
    if (!nextTokenIs(builder_, KBIND)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KBIND);
    exit_section_(builder_, marker_, BIND_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // '|' | '&' | '^'
  public static boolean bitOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitOperation")) return false;
    if (!nextTokenIs(builder_, "<bit operation>", OBIT_AND, OBIT_XOR, OBIT_OR)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<bit operation>");
    result_ = consumeToken(builder_, OBIT_OR);
    if (!result_) result_ = consumeToken(builder_, OBIT_AND);
    if (!result_) result_ = consumeToken(builder_, OBIT_XOR);
    exit_section_(builder_, level_, marker_, BIT_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '@:bitmap' '(' stringLiteralExpression ')'
  public static boolean bitmapMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitmapMeta")) return false;
    if (!nextTokenIs(builder_, KBITMAP)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KBITMAP);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, stringLiteralExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, BITMAP_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // bitOperation shiftExpressionWrapper
  public static boolean bitwiseExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpression")) return false;
    if (!nextTokenIs(builder_, "<bitwise expression>", OBIT_AND, OBIT_XOR, OBIT_OR)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<bitwise expression>");
    result_ = bitOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && shiftExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, BITWISE_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // shiftExpressionWrapper bitwiseExpression*
  static boolean bitwiseExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = shiftExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && bitwiseExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // bitwiseExpression*
  private static boolean bitwiseExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!bitwiseExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "bitwiseExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // '{' statementList? '}'
  public static boolean blockStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "blockStatement")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, PLCURLY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, blockStatement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, BLOCK_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // statementList?
  private static boolean blockStatement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "blockStatement_1")) return false;
    statementList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // 'break' ';'
  public static boolean breakStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "breakStatement")) return false;
    if (!nextTokenIs(builder_, KBREAK)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KBREAK);
    pinned_ = result_; // pin = 1
    result_ = result_ && consumeToken(builder_, OSEMI);
    exit_section_(builder_, level_, marker_, BREAK_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:build' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean buildMacro(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro")) return false;
    if (!nextTokenIs(builder_, KBUILD)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KBUILD);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, referenceExpression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, buildMacro_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, BUILD_MACRO, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean buildMacro_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro_3")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!buildMacro_3_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "buildMacro_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean buildMacro_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro_3_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '(' expressionList? ')'
  public static boolean callExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callExpression")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, callExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, CALL_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // expressionList?
  private static boolean callExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callExpression_1")) return false;
    expressionList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // (referenceExpression | thisExpression | superExpression) (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  static boolean callOrArrayAccess(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess")) return false;
    if (!nextTokenIs(builder_, "", KSUPER, KTHIS, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callOrArrayAccess_0(builder_, level_ + 1);
    result_ = result_ && callOrArrayAccess_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // referenceExpression | thisExpression | superExpression
  private static boolean callOrArrayAccess_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    if (!result_) result_ = thisExpression(builder_, level_ + 1);
    if (!result_) result_ = superExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean callOrArrayAccess_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!callOrArrayAccess_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "callOrArrayAccess_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean callOrArrayAccess_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'cast' (('(' expression ',' functionTypeWrapper ')')  | expression)
  public static boolean castExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression")) return false;
    if (!nextTokenIs(builder_, KCAST)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KCAST);
    pinned_ = result_; // pin = 1
    result_ = result_ && castExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CAST_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ('(' expression ',' functionTypeWrapper ')')  | expression
  private static boolean castExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = castExpression_1_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '(' expression ',' functionTypeWrapper ')'
  private static boolean castExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOMMA);
    result_ = result_ && functionTypeWrapper(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'catch' '(' parameter ')' statement ';'?
  public static boolean catchStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "catchStatement")) return false;
    if (!nextTokenIs(builder_, KCATCH)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KCATCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, parameter(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && catchStatement_5(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, CATCH_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean catchStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "catchStatement_5")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // classBodyPart*
  public static boolean classBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classBody")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<class body>");
    int pos_ = current_position_(builder_);
    while (true) {
      if (!classBodyPart(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "classBody", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, CLASS_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionDeclarationWithAttributes
  static boolean classBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classBodyPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = functionDeclarationWithAttributes(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, class_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // macroClassList? privateKeyWord? 'class' componentName genericParam? inheritList? '{' classBody '}'
  public static boolean classDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<class declaration>");
    result_ = classDeclaration_0(builder_, level_ + 1);
    result_ = result_ && classDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KCLASS);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, classDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, classDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && report_error_(builder_, classBody(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, CLASS_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // macroClassList?
  private static boolean classDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_0")) return false;
    macroClassList(builder_, level_ + 1);
    return true;
  }

  // privateKeyWord?
  private static boolean classDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_1")) return false;
    privateKeyWord(builder_, level_ + 1);
    return true;
  }

  // genericParam?
  private static boolean classDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_4")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // inheritList?
  private static boolean classDeclaration_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_5")) return false;
    inheritList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}')
  static boolean class_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !class_body_part_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}'
  private static boolean class_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = metaKeyWord(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // compareOperation bitwiseExpressionWrapper
  public static boolean compareExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpression")) return false;
    if (!nextTokenIs(builder_, "<compare expression>", ONOT_EQ, OLESS,
      OLESS_OR_EQUAL, OEQ, OGREATER, OGREATER_OR_EQUAL)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<compare expression>");
    result_ = compareOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && bitwiseExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, COMPARE_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // bitwiseExpressionWrapper compareExpression*
  static boolean compareExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = bitwiseExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && compareExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // compareExpression*
  private static boolean compareExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!compareExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "compareExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // '==' | '!=' | '<=' | '<' | '>' | '>='
  public static boolean compareOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareOperation")) return false;
    if (!nextTokenIs(builder_, "<compare operation>", ONOT_EQ, OLESS,
      OLESS_OR_EQUAL, OEQ, OGREATER, OGREATER_OR_EQUAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<compare operation>");
    result_ = consumeToken(builder_, OEQ);
    if (!result_) result_ = consumeToken(builder_, ONOT_EQ);
    if (!result_) result_ = consumeToken(builder_, OLESS_OR_EQUAL);
    if (!result_) result_ = consumeToken(builder_, OLESS);
    if (!result_) result_ = consumeToken(builder_, OGREATER);
    if (!result_) result_ = consumeToken(builder_, OGREATER_OR_EQUAL);
    exit_section_(builder_, level_, marker_, COMPARE_OPERATION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean componentName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "componentName")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, COMPONENT_NAME, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'continue' ';'
  public static boolean continueStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "continueStatement")) return false;
    if (!nextTokenIs(builder_, KCONTINUE)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KCONTINUE);
    pinned_ = result_; // pin = 1
    result_ = result_ && consumeToken(builder_, OSEMI);
    exit_section_(builder_, level_, marker_, CONTINUE_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:coreApi'
  public static boolean coreApiMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "coreApiMeta")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<core api meta>");
    result_ = consumeToken(builder_, "@:coreApi");
    exit_section_(builder_, level_, marker_, CORE_API_META, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // MACRO_ID ('(' expressionList ')')?
  public static boolean customMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta")) return false;
    if (!nextTokenIs(builder_, MACRO_ID)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, MACRO_ID);
    pinned_ = result_; // pin = 1
    result_ = result_ && customMeta_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, CUSTOM_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ('(' expressionList ')')?
  private static boolean customMeta_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta_1")) return false;
    customMeta_1_0(builder_, level_ + 1);
    return true;
  }

  // '(' expressionList ')'
  private static boolean customMeta_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && expressionList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:debug'
  public static boolean debugMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "debugMeta")) return false;
    if (!nextTokenIs(builder_, KDEBUG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KDEBUG);
    exit_section_(builder_, marker_, DEBUG_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'static' | 'inline' | 'dynamic' | 'override' | 'macro' | access
  public static boolean declarationAttribute(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "declarationAttribute")) return false;
    if (!nextTokenIs(builder_, "<declaration attribute>", KDYNAMIC, KINLINE,
      KMACRO2, KOVERRIDE, KPRIVATE, KPUBLIC, KSTATIC)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<declaration attribute>");
    result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KMACRO2);
    if (!result_) result_ = access(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, DECLARATION_ATTRIBUTE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // 'default' ':' switchCaseBlock?
  public static boolean defaultCase(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "defaultCase")) return false;
    if (!nextTokenIs(builder_, KDEFAULT)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KDEFAULT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, OCOLON));
    result_ = pinned_ && defaultCase_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, DEFAULT_CASE, result_, pinned_, null);
    return result_ || pinned_;
  }

  // switchCaseBlock?
  private static boolean defaultCase_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "defaultCase_2")) return false;
    switchCaseBlock(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // 'do' statement 'while' '(' expression ')' ';'
  public static boolean doWhileStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doWhileStatement")) return false;
    if (!nextTokenIs(builder_, KDO)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KDO);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, statement(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, KWHILE)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    exit_section_(builder_, level_, marker_, DO_WHILE_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // enumValueDeclaration*
  public static boolean enumBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumBody")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<enum body>");
    int pos_ = current_position_(builder_);
    while (true) {
      if (!enumValueDeclaration(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "enumBody", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, ENUM_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // '(' parameterList? ')'
  public static boolean enumConstructorParameters(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumConstructorParameters")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, enumConstructorParameters_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, ENUM_CONSTRUCTOR_PARAMETERS, result_, pinned_, null);
    return result_ || pinned_;
  }

  // parameterList?
  private static boolean enumConstructorParameters_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumConstructorParameters_1")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // macroClassList? externOrPrivate? 'enum' componentName genericParam? '{' enumBody '}'
  public static boolean enumDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumDeclaration")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<enum declaration>");
    result_ = enumDeclaration_0(builder_, level_ + 1);
    result_ = result_ && enumDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KENUM);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, enumDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && report_error_(builder_, enumBody(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, ENUM_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // macroClassList?
  private static boolean enumDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumDeclaration_0")) return false;
    macroClassList(builder_, level_ + 1);
    return true;
  }

  // externOrPrivate?
  private static boolean enumDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumDeclaration_1")) return false;
    externOrPrivate(builder_, level_ + 1);
    return true;
  }

  // genericParam?
  private static boolean enumDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumDeclaration_4")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // componentName enumConstructorParameters? ';'
  public static boolean enumValueDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumValueDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<enum value declaration>");
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && enumValueDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OSEMI);
    exit_section_(builder_, level_, marker_, ENUM_VALUE_DECLARATION, result_, false, enum_value_declaration_recovery_parser_);
    return result_;
  }

  // enumConstructorParameters?
  private static boolean enumValueDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumValueDeclaration_1")) return false;
    enumConstructorParameters(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // !(ID | '}')
  static boolean enum_value_declaration_recovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enum_value_declaration_recovery")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !enum_value_declaration_recovery_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // ID | '}'
  private static boolean enum_value_declaration_recovery_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enum_value_declaration_recovery_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ID);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // assignExpressionWrapper
  public static boolean expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, "<expression>");
    result_ = assignExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, EXPRESSION, result_, false, expression_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // forStatement | whileStatement | (expression (',' expression)*)
  public static boolean expressionList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<expression list>");
    result_ = forStatement(builder_, level_ + 1);
    if (!result_) result_ = whileStatement(builder_, level_ + 1);
    if (!result_) result_ = expressionList_2(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, EXPRESSION_LIST, result_, false, expression_list_recover_parser_);
    return result_;
  }

  // expression (',' expression)*
  private static boolean expressionList_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && expressionList_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' expression)*
  private static boolean expressionList_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList_2_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!expressionList_2_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "expressionList_2_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' expression
  private static boolean expressionList_2_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList_2_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !(')' | ']')
  static boolean expression_list_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_list_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !expression_list_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // ')' | ']'
  private static boolean expression_list_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_list_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PRPAREN);
    if (!result_) result_ = consumeToken(builder_, PRBRACK);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !('!' | '!=' | '%' | '%=' | '&&' | '&' | '&=' | '(' | ')' | '*' | '*=' | '+' | '++' | '+=' | ',' | '-' | '--' | '-=' | '.' | '...' | '/' | '/=' | ':' | ';' | '<' | '<<' | '<<=' | '<=' | '=' | '==' | '>' | '>=' | '>>=' | '>>>=' | '?' | metaKeyWord | '[' | ']' | '^' | '^=' | 'break' | 'case' | 'cast' | 'catch' | 'continue' | 'default' | 'do' | 'dynamic' | 'else' | 'false' | 'for' | 'function' | 'if' | 'inline' | 'new' | 'null' | 'override' | 'private' | 'public' | 'return' | 'static' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '|' | '|=' | '||' | '}' | '~' | ID | LITFLOAT | LITHEX | LITINT | LITOCT | OPEN_QUOTE | CLOSING_QUOTE | MACRO_ID | REG_EXP | LONG_TEMPLATE_ENTRY_END)
  static boolean expression_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !expression_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // '!' | '!=' | '%' | '%=' | '&&' | '&' | '&=' | '(' | ')' | '*' | '*=' | '+' | '++' | '+=' | ',' | '-' | '--' | '-=' | '.' | '...' | '/' | '/=' | ':' | ';' | '<' | '<<' | '<<=' | '<=' | '=' | '==' | '>' | '>=' | '>>=' | '>>>=' | '?' | metaKeyWord | '[' | ']' | '^' | '^=' | 'break' | 'case' | 'cast' | 'catch' | 'continue' | 'default' | 'do' | 'dynamic' | 'else' | 'false' | 'for' | 'function' | 'if' | 'inline' | 'new' | 'null' | 'override' | 'private' | 'public' | 'return' | 'static' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '|' | '|=' | '||' | '}' | '~' | ID | LITFLOAT | LITHEX | LITINT | LITOCT | OPEN_QUOTE | CLOSING_QUOTE | MACRO_ID | REG_EXP | LONG_TEMPLATE_ENTRY_END
  private static boolean expression_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ONOT);
    if (!result_) result_ = consumeToken(builder_, ONOT_EQ);
    if (!result_) result_ = consumeToken(builder_, OREMAINDER);
    if (!result_) result_ = consumeToken(builder_, OREMAINDER_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OCOND_AND);
    if (!result_) result_ = consumeToken(builder_, OBIT_AND);
    if (!result_) result_ = consumeToken(builder_, OBIT_AND_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, PLPAREN);
    if (!result_) result_ = consumeToken(builder_, PRPAREN);
    if (!result_) result_ = consumeToken(builder_, OMUL);
    if (!result_) result_ = consumeToken(builder_, OMUL_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OPLUS);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (!result_) result_ = consumeToken(builder_, OPLUS_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OCOMMA);
    if (!result_) result_ = consumeToken(builder_, OMINUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, ODOT);
    if (!result_) result_ = consumeToken(builder_, OTRIPLE_DOT);
    if (!result_) result_ = consumeToken(builder_, OQUOTIENT);
    if (!result_) result_ = consumeToken(builder_, OQUOTIENT_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OCOLON);
    if (!result_) result_ = consumeToken(builder_, OSEMI);
    if (!result_) result_ = consumeToken(builder_, OLESS);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_LEFT);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_LEFT_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OLESS_OR_EQUAL);
    if (!result_) result_ = consumeToken(builder_, OASSIGN);
    if (!result_) result_ = consumeToken(builder_, OEQ);
    if (!result_) result_ = consumeToken(builder_, OGREATER);
    if (!result_) result_ = consumeToken(builder_, OGREATER_OR_EQUAL);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_RIGHT_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, ">>>=");
    if (!result_) result_ = consumeToken(builder_, OQUEST);
    if (!result_) result_ = metaKeyWord(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, PLBRACK);
    if (!result_) result_ = consumeToken(builder_, PRBRACK);
    if (!result_) result_ = consumeToken(builder_, OBIT_XOR);
    if (!result_) result_ = consumeToken(builder_, OBIT_XOR_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, KBREAK);
    if (!result_) result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KCAST);
    if (!result_) result_ = consumeToken(builder_, KCATCH);
    if (!result_) result_ = consumeToken(builder_, KCONTINUE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, KDO);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KELSE);
    if (!result_) result_ = consumeToken(builder_, KFALSE);
    if (!result_) result_ = consumeToken(builder_, KFOR);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KIF);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KRETURN);
    if (!result_) result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KSUPER);
    if (!result_) result_ = consumeToken(builder_, KSWITCH);
    if (!result_) result_ = consumeToken(builder_, KTHIS);
    if (!result_) result_ = consumeToken(builder_, KTHROW);
    if (!result_) result_ = consumeToken(builder_, KTRUE);
    if (!result_) result_ = consumeToken(builder_, KTRY);
    if (!result_) result_ = consumeToken(builder_, KUNTYPED);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, KWHILE);
    if (!result_) result_ = consumeToken(builder_, PLCURLY);
    if (!result_) result_ = consumeToken(builder_, OBIT_OR);
    if (!result_) result_ = consumeToken(builder_, OBIT_OR_ASSIGN);
    if (!result_) result_ = consumeToken(builder_, OCOND_OR);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, OCOMPLEMENT);
    if (!result_) result_ = consumeToken(builder_, ID);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, OPEN_QUOTE);
    if (!result_) result_ = consumeToken(builder_, CLOSING_QUOTE);
    if (!result_) result_ = consumeToken(builder_, MACRO_ID);
    if (!result_) result_ = consumeToken(builder_, REG_EXP);
    if (!result_) result_ = consumeToken(builder_, LONG_TEMPLATE_ENTRY_END);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // typeExtends (',' anonymousTypeFieldList)? (',' interfaceBody)?
  static boolean extendedAnonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeExtends(builder_, level_ + 1);
    result_ = result_ && extendedAnonymousTypeBody_1(builder_, level_ + 1);
    result_ = result_ && extendedAnonymousTypeBody_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' anonymousTypeFieldList)?
  private static boolean extendedAnonymousTypeBody_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_1")) return false;
    extendedAnonymousTypeBody_1_0(builder_, level_ + 1);
    return true;
  }

  // ',' anonymousTypeFieldList
  private static boolean extendedAnonymousTypeBody_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && anonymousTypeFieldList(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' interfaceBody)?
  private static boolean extendedAnonymousTypeBody_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_2")) return false;
    extendedAnonymousTypeBody_2_0(builder_, level_ + 1);
    return true;
  }

  // ',' interfaceBody
  private static boolean extendedAnonymousTypeBody_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && interfaceBody(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // macroClassList? externOrPrivate* 'class' componentName genericParam? inheritList? '{' externClassDeclarationBody '}'
  public static boolean externClassDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<extern class declaration>");
    result_ = externClassDeclaration_0(builder_, level_ + 1);
    result_ = result_ && externClassDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KCLASS);
    result_ = result_ && componentName(builder_, level_ + 1);
    pinned_ = result_; // pin = 4
    result_ = result_ && report_error_(builder_, externClassDeclaration_4(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, externClassDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && report_error_(builder_, externClassDeclarationBody(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, EXTERN_CLASS_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // macroClassList?
  private static boolean externClassDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration_0")) return false;
    macroClassList(builder_, level_ + 1);
    return true;
  }

  // externOrPrivate*
  private static boolean externClassDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!externOrPrivate(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "externClassDeclaration_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // genericParam?
  private static boolean externClassDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration_4")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // inheritList?
  private static boolean externClassDeclaration_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration_5")) return false;
    inheritList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // externClassDeclarationBodyPart*
  public static boolean externClassDeclarationBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclarationBody")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<extern class declaration body>");
    int pos_ = current_position_(builder_);
    while (true) {
      if (!externClassDeclarationBodyPart(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "externClassDeclarationBody", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, EXTERN_CLASS_DECLARATION_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | externFunctionDeclaration
  static boolean externClassDeclarationBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclarationBodyPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = externFunctionDeclaration(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, extern_class_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // (functionMacroMember| declarationAttribute)* 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? (functionCommonBody | ';')
  public static boolean externFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, "<extern function declaration>", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE, KSETTER,
      KDYNAMIC, KFUNCTION, KINLINE, KMACRO2, KOVERRIDE, KPRIVATE,
      KPUBLIC, KSTATIC, MACRO_ID)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<extern function declaration>");
    result_ = externFunctionDeclaration_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KFUNCTION);
    result_ = result_ && externFunctionDeclaration_2(builder_, level_ + 1);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, externFunctionDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, externFunctionDeclaration_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, externFunctionDeclaration_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && externFunctionDeclaration_8(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, EXTERN_FUNCTION_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (functionMacroMember| declarationAttribute)*
  private static boolean externFunctionDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!externFunctionDeclaration_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "externFunctionDeclaration_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // functionMacroMember| declarationAttribute
  private static boolean externFunctionDeclaration_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionMacroMember(builder_, level_ + 1);
    if (!result_) result_ = declarationAttribute(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'new' | componentName genericParam?
  private static boolean externFunctionDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = externFunctionDeclaration_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // componentName genericParam?
  private static boolean externFunctionDeclaration_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_2_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && externFunctionDeclaration_2_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // genericParam?
  private static boolean externFunctionDeclaration_2_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_2_1_1")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // parameterList?
  private static boolean externFunctionDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_4")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean externFunctionDeclaration_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_6")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean externFunctionDeclaration_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_7")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  // functionCommonBody | ';'
  private static boolean externFunctionDeclaration_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_8")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionCommonBody(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, OSEMI);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'extern' | privateKeyWord
  public static boolean externOrPrivate(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externOrPrivate")) return false;
    if (!nextTokenIs(builder_, "<extern or private>", KEXTERN, KPRIVATE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<extern or private>");
    result_ = consumeToken(builder_, KEXTERN);
    if (!result_) result_ = privateKeyWord(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, EXTERN_OR_PRIVATE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}')
  static boolean extern_class_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !extern_class_body_part_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}'
  private static boolean extern_class_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = metaKeyWord(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:fakeEnum' '(' type ')'
  public static boolean fakeEnumMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "fakeEnumMeta")) return false;
    if (!nextTokenIs(builder_, KFAKEENUM)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KFAKEENUM);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, type(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, FAKE_ENUM_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:final'
  public static boolean finalMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "finalMeta")) return false;
    if (!nextTokenIs(builder_, KFINAL)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KFINAL);
    exit_section_(builder_, marker_, FINAL_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'for' '(' componentName 'in' iterable')' statement ';'?
  public static boolean forStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forStatement")) return false;
    if (!nextTokenIs(builder_, KFOR)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KFOR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, componentName(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OIN)) && result_;
    result_ = pinned_ && report_error_(builder_, iterable(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && forStatement_7(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, FOR_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean forStatement_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forStatement_7")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // blockStatement | returnStatement | expression | throwStatement
  static boolean functionCommonBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionCommonBody")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KRETURN, KSUPER,
      KSWITCH, KTHIS, KTHROW, KTRUE, KTRY, KUNTYPED,
      PLCURLY, OCOMPLEMENT, ID, LITFLOAT, LITHEX, LITINT,
      LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = blockStatement(builder_, level_ + 1);
    if (!result_) result_ = returnStatement(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    if (!result_) result_ = throwStatement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (functionMacroMember | declarationAttribute)* 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean functionDeclarationWithAttributes(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes")) return false;
    if (!nextTokenIs(builder_, "<function declaration with attributes>", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE, KSETTER,
      KDYNAMIC, KFUNCTION, KINLINE, KMACRO2, KOVERRIDE, KPRIVATE,
      KPUBLIC, KSTATIC, MACRO_ID)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function declaration with attributes>");
    result_ = functionDeclarationWithAttributes_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KFUNCTION);
    result_ = result_ && functionDeclarationWithAttributes_2(builder_, level_ + 1);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, functionDeclarationWithAttributes_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionDeclarationWithAttributes_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, functionDeclarationWithAttributes_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && functionCommonBody(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, FUNCTION_DECLARATION_WITH_ATTRIBUTES, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (functionMacroMember | declarationAttribute)*
  private static boolean functionDeclarationWithAttributes_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!functionDeclarationWithAttributes_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "functionDeclarationWithAttributes_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // functionMacroMember | declarationAttribute
  private static boolean functionDeclarationWithAttributes_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionMacroMember(builder_, level_ + 1);
    if (!result_) result_ = declarationAttribute(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'new' | componentName genericParam?
  private static boolean functionDeclarationWithAttributes_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = functionDeclarationWithAttributes_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // componentName genericParam?
  private static boolean functionDeclarationWithAttributes_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_2_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && functionDeclarationWithAttributes_2_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // genericParam?
  private static boolean functionDeclarationWithAttributes_2_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_2_1_1")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // parameterList?
  private static boolean functionDeclarationWithAttributes_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_4")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean functionDeclarationWithAttributes_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_6")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionDeclarationWithAttributes_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_7")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // 'function' '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean functionLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral")) return false;
    if (!nextTokenIs(builder_, KFUNCTION)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KFUNCTION);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, functionLiteral_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionLiteral_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, functionLiteral_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && functionCommonBody(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, FUNCTION_LITERAL, result_, pinned_, null);
    return result_ || pinned_;
  }

  // parameterList?
  private static boolean functionLiteral_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_2")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean functionLiteral_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_4")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionLiteral_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral_5")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // finalMeta | macroMember | overloadMeta
  static boolean functionMacroMember(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionMacroMember")) return false;
    if (!nextTokenIs(builder_, "", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE, KSETTER, MACRO_ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = finalMeta(builder_, level_ + 1);
    if (!result_) result_ = macroMember(builder_, level_ + 1);
    if (!result_) result_ = overloadMeta(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (functionMacroMember| declarationAttribute)* 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? ';'
  public static boolean functionPrototypeDeclarationWithAttributes(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes")) return false;
    if (!nextTokenIs(builder_, "<function prototype declaration with attributes>", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE, KSETTER,
      KDYNAMIC, KFUNCTION, KINLINE, KMACRO2, KOVERRIDE, KPRIVATE,
      KPUBLIC, KSTATIC, MACRO_ID)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<function prototype declaration with attributes>");
    result_ = functionPrototypeDeclarationWithAttributes_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KFUNCTION);
    result_ = result_ && functionPrototypeDeclarationWithAttributes_2(builder_, level_ + 1);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, functionPrototypeDeclarationWithAttributes_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionPrototypeDeclarationWithAttributes_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, functionPrototypeDeclarationWithAttributes_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    exit_section_(builder_, level_, marker_, FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (functionMacroMember| declarationAttribute)*
  private static boolean functionPrototypeDeclarationWithAttributes_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!functionPrototypeDeclarationWithAttributes_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "functionPrototypeDeclarationWithAttributes_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // functionMacroMember| declarationAttribute
  private static boolean functionPrototypeDeclarationWithAttributes_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionMacroMember(builder_, level_ + 1);
    if (!result_) result_ = declarationAttribute(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'new' | componentName genericParam?
  private static boolean functionPrototypeDeclarationWithAttributes_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = functionPrototypeDeclarationWithAttributes_2_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // componentName genericParam?
  private static boolean functionPrototypeDeclarationWithAttributes_2_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_2_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && functionPrototypeDeclarationWithAttributes_2_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // genericParam?
  private static boolean functionPrototypeDeclarationWithAttributes_2_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_2_1_1")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // parameterList?
  private static boolean functionPrototypeDeclarationWithAttributes_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_4")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean functionPrototypeDeclarationWithAttributes_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_6")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionPrototypeDeclarationWithAttributes_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_7")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // '->' typeOrAnonymous
  public static boolean functionType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionType")) return false;
    if (!nextTokenIs(builder_, OARROW)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, OARROW);
    result_ = result_ && typeOrAnonymous(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, FUNCTION_TYPE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // typeOrAnonymous | '(' functionTypeWrapper ')'
  static boolean functionTypeOrWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeOrWrapper")) return false;
    if (!nextTokenIs(builder_, "", PLPAREN, PLCURLY, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = typeOrAnonymous(builder_, level_ + 1);
    if (!result_) result_ = functionTypeOrWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '(' functionTypeWrapper ')'
  private static boolean functionTypeOrWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeOrWrapper_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && functionTypeWrapper(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // functionTypeOrWrapper functionType*
  static boolean functionTypeWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeWrapper")) return false;
    if (!nextTokenIs(builder_, "", PLPAREN, PLCURLY, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = functionTypeOrWrapper(builder_, level_ + 1);
    result_ = result_ && functionTypeWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // functionType*
  private static boolean functionTypeWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!functionType(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "functionTypeWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // componentName (':' ('(' typeList ')' | typeListPart))?
  public static boolean genericListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && genericListPart_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, GENERIC_LIST_PART, result_);
    return result_;
  }

  // (':' ('(' typeList ')' | typeListPart))?
  private static boolean genericListPart_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1")) return false;
    genericListPart_1_0(builder_, level_ + 1);
    return true;
  }

  // ':' ('(' typeList ')' | typeListPart)
  private static boolean genericListPart_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOLON);
    result_ = result_ && genericListPart_1_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '(' typeList ')' | typeListPart
  private static boolean genericListPart_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = genericListPart_1_0_1_0(builder_, level_ + 1);
    if (!result_) result_ = typeListPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '(' typeList ')'
  private static boolean genericListPart_1_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && typeList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '<' genericListPart (',' genericListPart)* '>'
  public static boolean genericParam(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericParam")) return false;
    if (!nextTokenIs(builder_, OLESS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OLESS);
    result_ = result_ && genericListPart(builder_, level_ + 1);
    result_ = result_ && genericParam_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OGREATER);
    exit_section_(builder_, marker_, GENERIC_PARAM, result_);
    return result_;
  }

  // (',' genericListPart)*
  private static boolean genericParam_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericParam_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!genericParam_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "genericParam_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' genericListPart
  private static boolean genericParam_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericParam_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && genericListPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:getter' '(' referenceExpression ')'
  public static boolean getterMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getterMeta")) return false;
    if (!nextTokenIs(builder_, KGETTER)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KGETTER);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, referenceExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, GETTER_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:hack'
  public static boolean hackMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "hackMeta")) return false;
    if (!nextTokenIs(builder_, KHACK)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KHACK);
    exit_section_(builder_, marker_, HACK_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // packageStatement? topLevelList
  static boolean haxeFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "haxeFile")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = haxeFile_0(builder_, level_ + 1);
    result_ = result_ && topLevelList(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // packageStatement?
  private static boolean haxeFile_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "haxeFile_0")) return false;
    packageStatement(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ID
  public static boolean identifier(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "identifier")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ID);
    exit_section_(builder_, marker_, IDENTIFIER, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'if' '(' expression ')' statement ';'? ('else' statement ';'?)?
  public static boolean ifStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement")) return false;
    if (!nextTokenIs(builder_, KIF)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KIF);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, ifStatement_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && ifStatement_6(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, IF_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean ifStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_5")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  // ('else' statement ';'?)?
  private static boolean ifStatement_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_6")) return false;
    ifStatement_6_0(builder_, level_ + 1);
    return true;
  }

  // 'else' statement ';'?
  private static boolean ifStatement_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_6_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KELSE);
    result_ = result_ && statement(builder_, level_ + 1);
    result_ = result_ && ifStatement_6_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ';'?
  private static boolean ifStatement_6_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_6_0_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // importStatementWithWildcard | importStatementWithInSupport | importStatementRegular
  static boolean importStatementAll(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importStatementAll")) return false;
    if (!nextTokenIs(builder_, KIMPORT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = importStatementWithWildcard(builder_, level_ + 1);
    if (!result_) result_ = importStatementWithInSupport(builder_, level_ + 1);
    if (!result_) result_ = importStatementRegular(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpression ';'
  public static boolean importStatementRegular(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importStatementRegular")) return false;
    if (!nextTokenIs(builder_, KIMPORT)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KIMPORT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, simpleQualifiedReferenceExpression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    exit_section_(builder_, level_, marker_, IMPORT_STATEMENT_REGULAR, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpression 'in' identifier';'
  public static boolean importStatementWithInSupport(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importStatementWithInSupport")) return false;
    if (!nextTokenIs(builder_, KIMPORT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KIMPORT);
    result_ = result_ && simpleQualifiedReferenceExpression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OIN);
    result_ = result_ && identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OSEMI);
    exit_section_(builder_, marker_, IMPORT_STATEMENT_WITH_IN_SUPPORT, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpressionWithWildcardSupport ';'
  public static boolean importStatementWithWildcard(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importStatementWithWildcard")) return false;
    if (!nextTokenIs(builder_, KIMPORT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KIMPORT);
    result_ = result_ && simpleQualifiedReferenceExpressionWithWildcardSupport(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OSEMI);
    exit_section_(builder_, marker_, IMPORT_STATEMENT_WITH_WILDCARD, result_);
    return result_;
  }

  /* ********************************************************** */
  // ('extends' | 'implements') type
  public static boolean inherit(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<inherit>");
    result_ = inherit_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, INHERIT, result_, pinned_, inherit_recover_parser_);
    return result_ || pinned_;
  }

  // 'extends' | 'implements'
  private static boolean inherit_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KEXTENDS);
    if (!result_) result_ = consumeToken(builder_, KIMPLEMENTS);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // inherit (','? inherit)*
  public static boolean inheritList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList")) return false;
    if (!nextTokenIs(builder_, "<inherit list>", KEXTENDS, KIMPLEMENTS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<inherit list>");
    result_ = inherit(builder_, level_ + 1);
    result_ = result_ && inheritList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, INHERIT_LIST, result_, false, null);
    return result_;
  }

  // (','? inherit)*
  private static boolean inheritList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!inheritList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "inheritList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ','? inherit
  private static boolean inheritList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = inheritList_1_0_0(builder_, level_ + 1);
    result_ = result_ && inherit(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ','?
  private static boolean inheritList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList_1_0_0")) return false;
    consumeToken(builder_, OCOMMA);
    return true;
  }

  /* ********************************************************** */
  // !(',' | '{' | 'extends' | 'implements')
  static boolean inherit_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !inherit_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // ',' | '{' | 'extends' | 'implements'
  private static boolean inherit_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    if (!result_) result_ = consumeToken(builder_, PLCURLY);
    if (!result_) result_ = consumeToken(builder_, KEXTENDS);
    if (!result_) result_ = consumeToken(builder_, KIMPLEMENTS);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // interfaceBodyPart*
  public static boolean interfaceBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceBody")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<interface body>");
    int pos_ = current_position_(builder_);
    while (true) {
      if (!interfaceBodyPart(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "interfaceBody", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, INTERFACE_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionPrototypeDeclarationWithAttributes
  static boolean interfaceBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceBodyPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = functionPrototypeDeclarationWithAttributes(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, interface_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // macroClassList? externOrPrivate? 'interface' componentName genericParam? inheritList? '{' interfaceBody '}'
  public static boolean interfaceDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<interface declaration>");
    result_ = interfaceDeclaration_0(builder_, level_ + 1);
    result_ = result_ && interfaceDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KINTERFACE);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, interfaceDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, interfaceDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && report_error_(builder_, interfaceBody(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, INTERFACE_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // macroClassList?
  private static boolean interfaceDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration_0")) return false;
    macroClassList(builder_, level_ + 1);
    return true;
  }

  // externOrPrivate?
  private static boolean interfaceDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration_1")) return false;
    externOrPrivate(builder_, level_ + 1);
    return true;
  }

  // genericParam?
  private static boolean interfaceDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration_4")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // inheritList?
  private static boolean interfaceDeclaration_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration_5")) return false;
    inheritList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}')
  static boolean interface_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !interface_body_part_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}'
  private static boolean interface_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = metaKeyWord(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression
  public static boolean iterable(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iterable")) return false;
    if (!nextTokenIs(builder_, "<iterable>", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<iterable>");
    result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ITERABLE, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '...' ternaryExpressionWrapper
  public static boolean iteratorExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpression")) return false;
    if (!nextTokenIs(builder_, OTRIPLE_DOT)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, OTRIPLE_DOT);
    pinned_ = result_; // pin = 1
    result_ = result_ && ternaryExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, ITERATOR_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // ternaryExpressionWrapper iteratorExpression?
  static boolean iteratorExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = ternaryExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && iteratorExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // iteratorExpression?
  private static boolean iteratorExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpressionWrapper_1")) return false;
    iteratorExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // "@:jsRequire" '(' stringLiteralExpression ')'
  public static boolean jsRequireMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jsRequireMeta")) return false;
    if (!nextTokenIs(builder_, KJSREQUIRE)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KJSREQUIRE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, stringLiteralExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, JS_REQUIRE_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:keep'
  public static boolean keepMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "keepMeta")) return false;
    if (!nextTokenIs(builder_, KKEEP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KKEEP);
    exit_section_(builder_, marker_, KEEP_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // LITINT | LITHEX | LITOCT | LITFLOAT
  //                     | regularExpressionLiteral
  //                     | 'null' | 'true' | 'false'
  //                     | functionLiteral
  //                     | arrayLiteral
  //                     | objectLiteral
  //                     | stringLiteralExpression
  public static boolean literalExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literalExpression")) return false;
    if (!nextTokenIs(builder_, "<literal expression>", PLBRACK, KFALSE,
      KFUNCTION, KNULL, KTRUE, PLCURLY, LITFLOAT, LITHEX,
      LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, "<literal expression>");
    result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = regularExpressionLiteral(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KTRUE);
    if (!result_) result_ = consumeToken(builder_, KFALSE);
    if (!result_) result_ = functionLiteral(builder_, level_ + 1);
    if (!result_) result_ = arrayLiteral(builder_, level_ + 1);
    if (!result_) result_ = objectLiteral(builder_, level_ + 1);
    if (!result_) result_ = stringLiteralExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LITERAL_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // 'function' componentName genericParam? '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean localFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, KFUNCTION)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KFUNCTION);
    result_ = result_ && componentName(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, localFunctionDeclaration_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, localFunctionDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, localFunctionDeclaration_6(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, localFunctionDeclaration_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && functionCommonBody(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, LOCAL_FUNCTION_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // genericParam?
  private static boolean localFunctionDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localFunctionDeclaration_2")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // parameterList?
  private static boolean localFunctionDeclaration_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localFunctionDeclaration_4")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean localFunctionDeclaration_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localFunctionDeclaration_6")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean localFunctionDeclaration_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localFunctionDeclaration_7")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // 'var' localVarDeclarationPartList ';'?
  public static boolean localVarDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclaration")) return false;
    if (!nextTokenIs(builder_, KVAR)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KVAR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, localVarDeclarationPartList(builder_, level_ + 1));
    result_ = pinned_ && localVarDeclaration_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, LOCAL_VAR_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean localVarDeclaration_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclaration_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // componentName propertyDeclaration? typeTag? varInit?
  public static boolean localVarDeclarationPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<local var declaration part>");
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_1(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_2(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LOCAL_VAR_DECLARATION_PART, result_, false, local_var_declaration_part_recover_parser_);
    return result_;
  }

  // propertyDeclaration?
  private static boolean localVarDeclarationPart_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPart_1")) return false;
    propertyDeclaration(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean localVarDeclarationPart_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPart_2")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // varInit?
  private static boolean localVarDeclarationPart_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPart_3")) return false;
    varInit(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // localVarDeclarationPart (',' localVarDeclarationPart)*
  static boolean localVarDeclarationPartList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPartList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = localVarDeclarationPart(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPartList_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' localVarDeclarationPart)*
  private static boolean localVarDeclarationPartList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPartList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!localVarDeclarationPartList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "localVarDeclarationPartList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' localVarDeclarationPart
  private static boolean localVarDeclarationPartList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPartList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && localVarDeclarationPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !('!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | ')' | '++' | ',' | '-' | '--' | ';' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'else' | 'false' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '}' | '~' | ID | OPEN_QUOTE | LITFLOAT | LITHEX | LITINT | LITOCT | REG_EXP)
  static boolean local_var_declaration_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !local_var_declaration_part_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // '!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | ')' | '++' | ',' | '-' | '--' | ';' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'else' | 'false' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '}' | '~' | ID | OPEN_QUOTE | LITFLOAT | LITHEX | LITINT | LITOCT | REG_EXP
  private static boolean local_var_declaration_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ONOT);
    if (!result_) result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, PLPAREN);
    if (!result_) result_ = consumeToken(builder_, PRPAREN);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (!result_) result_ = consumeToken(builder_, OCOMMA);
    if (!result_) result_ = consumeToken(builder_, OMINUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, OSEMI);
    if (!result_) result_ = consumeToken(builder_, PLBRACK);
    if (!result_) result_ = consumeToken(builder_, KBREAK);
    if (!result_) result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KCAST);
    if (!result_) result_ = consumeToken(builder_, KCONTINUE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, KDO);
    if (!result_) result_ = consumeToken(builder_, KELSE);
    if (!result_) result_ = consumeToken(builder_, KFALSE);
    if (!result_) result_ = consumeToken(builder_, KFOR);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KIF);
    if (!result_) result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KRETURN);
    if (!result_) result_ = consumeToken(builder_, KSUPER);
    if (!result_) result_ = consumeToken(builder_, KSWITCH);
    if (!result_) result_ = consumeToken(builder_, KTHIS);
    if (!result_) result_ = consumeToken(builder_, KTHROW);
    if (!result_) result_ = consumeToken(builder_, KTRUE);
    if (!result_) result_ = consumeToken(builder_, KTRY);
    if (!result_) result_ = consumeToken(builder_, KUNTYPED);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, KWHILE);
    if (!result_) result_ = consumeToken(builder_, PLCURLY);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, OCOMPLEMENT);
    if (!result_) result_ = consumeToken(builder_, ID);
    if (!result_) result_ = consumeToken(builder_, OPEN_QUOTE);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, REG_EXP);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '&&' compareExpressionWrapper
  public static boolean logicAndExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicAndExpression")) return false;
    if (!nextTokenIs(builder_, OCOND_AND)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, OCOND_AND);
    pinned_ = result_; // pin = 1
    result_ = result_ && compareExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LOGIC_AND_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // compareExpressionWrapper logicAndExpression*
  static boolean logicAndExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicAndExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = compareExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && logicAndExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // logicAndExpression*
  private static boolean logicAndExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicAndExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!logicAndExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "logicAndExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // '||' logicAndExpressionWrapper
  public static boolean logicOrExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicOrExpression")) return false;
    if (!nextTokenIs(builder_, OCOND_OR)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, OCOND_OR);
    pinned_ = result_; // pin = 1
    result_ = result_ && logicAndExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, LOGIC_OR_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // logicAndExpressionWrapper logicOrExpression*
  static boolean logicOrExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicOrExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = logicAndExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && logicOrExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // logicOrExpression*
  private static boolean logicOrExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicOrExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!logicOrExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "logicOrExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // LONG_TEMPLATE_ENTRY_START expression LONG_TEMPLATE_ENTRY_END
  public static boolean longTemplateEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "longTemplateEntry")) return false;
    if (!nextTokenIs(builder_, LONG_TEMPLATE_ENTRY_START)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, LONG_TEMPLATE_ENTRY_START);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, LONG_TEMPLATE_ENTRY_END) && result_;
    exit_section_(builder_, level_, marker_, LONG_TEMPLATE_ENTRY, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // simpleMeta | requireMeta | fakeEnumMeta | nativeMeta | jsRequireMeta | bitmapMeta | nsMeta | customMeta | metaMeta | buildMacro | autoBuildMacro
  public static boolean macroClass(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroClass")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<macro class>");
    result_ = simpleMeta(builder_, level_ + 1);
    if (!result_) result_ = requireMeta(builder_, level_ + 1);
    if (!result_) result_ = fakeEnumMeta(builder_, level_ + 1);
    if (!result_) result_ = nativeMeta(builder_, level_ + 1);
    if (!result_) result_ = jsRequireMeta(builder_, level_ + 1);
    if (!result_) result_ = bitmapMeta(builder_, level_ + 1);
    if (!result_) result_ = nsMeta(builder_, level_ + 1);
    if (!result_) result_ = customMeta(builder_, level_ + 1);
    if (!result_) result_ = metaMeta(builder_, level_ + 1);
    if (!result_) result_ = buildMacro(builder_, level_ + 1);
    if (!result_) result_ = autoBuildMacro(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, MACRO_CLASS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // macroClass (',' macroClass)*
  public static boolean macroClassList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroClassList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<macro class list>");
    result_ = macroClass(builder_, level_ + 1);
    result_ = result_ && macroClassList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, MACRO_CLASS_LIST, result_, false, null);
    return result_;
  }

  // (',' macroClass)*
  private static boolean macroClassList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroClassList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!macroClassList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "macroClassList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' macroClass
  private static boolean macroClassList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroClassList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && macroClass(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // macroMeta | protectedMeta | debugMeta | noDebugMeta | keepMeta
  //                        | requireMeta | nsMeta | getterMeta | setterMeta | customMeta | metaMeta | buildMacro | autoBuildMacro
  static boolean macroMember(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroMember")) return false;
    if (!nextTokenIs(builder_, "", KAUTOBUILD, KBUILD,
      KDEBUG, KGETTER, KKEEP, KMACRO, KMETA, KNODEBUG,
      KNS, KPROTECTED, KREQUIRE, KSETTER, MACRO_ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = macroMeta(builder_, level_ + 1);
    if (!result_) result_ = protectedMeta(builder_, level_ + 1);
    if (!result_) result_ = debugMeta(builder_, level_ + 1);
    if (!result_) result_ = noDebugMeta(builder_, level_ + 1);
    if (!result_) result_ = keepMeta(builder_, level_ + 1);
    if (!result_) result_ = requireMeta(builder_, level_ + 1);
    if (!result_) result_ = nsMeta(builder_, level_ + 1);
    if (!result_) result_ = getterMeta(builder_, level_ + 1);
    if (!result_) result_ = setterMeta(builder_, level_ + 1);
    if (!result_) result_ = customMeta(builder_, level_ + 1);
    if (!result_) result_ = metaMeta(builder_, level_ + 1);
    if (!result_) result_ = buildMacro(builder_, level_ + 1);
    if (!result_) result_ = autoBuildMacro(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:macro'
  public static boolean macroMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroMeta")) return false;
    if (!nextTokenIs(builder_, KMACRO)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KMACRO);
    exit_section_(builder_, marker_, MACRO_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // ID '=' stringLiteralExpression
  public static boolean metaKeyValue(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaKeyValue")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, ID);
    result_ = result_ && consumeToken(builder_, OASSIGN);
    result_ = result_ && stringLiteralExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, META_KEY_VALUE, result_);
    return result_;
  }

  /* ********************************************************** */
  // MACRO_ID | '@:final' | '@:hack' | '@:native' | '@:macro' | '@:build' | '@:autoBuild' | '@:keep' | '@:require' | '@:fakeEnum' | '@:core_api' | '@:bind' | '@:bitmap' | '@:ns' | '@:protected' | '@:getter' | '@:setter' | '@:debug' | '@:nodebug' | '@:meta' | '@:overload' | '@:jsRequire'
  static boolean metaKeyWord(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaKeyWord")) return false;
    if (!nextTokenIs(builder_, "", KAUTOBUILD, KBIND,
      KBITMAP, KBUILD, KCOREAPI, KDEBUG, KFAKEENUM, KFINAL,
      KGETTER, KHACK, KJSREQUIRE, KKEEP, KMACRO, KMETA,
      KNATIVE, KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE, KSETTER, MACRO_ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, MACRO_ID);
    if (!result_) result_ = consumeToken(builder_, KFINAL);
    if (!result_) result_ = consumeToken(builder_, KHACK);
    if (!result_) result_ = consumeToken(builder_, KNATIVE);
    if (!result_) result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KBUILD);
    if (!result_) result_ = consumeToken(builder_, KAUTOBUILD);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KREQUIRE);
    if (!result_) result_ = consumeToken(builder_, KFAKEENUM);
    if (!result_) result_ = consumeToken(builder_, KCOREAPI);
    if (!result_) result_ = consumeToken(builder_, KBIND);
    if (!result_) result_ = consumeToken(builder_, KBITMAP);
    if (!result_) result_ = consumeToken(builder_, KNS);
    if (!result_) result_ = consumeToken(builder_, KPROTECTED);
    if (!result_) result_ = consumeToken(builder_, KGETTER);
    if (!result_) result_ = consumeToken(builder_, KSETTER);
    if (!result_) result_ = consumeToken(builder_, KDEBUG);
    if (!result_) result_ = consumeToken(builder_, KNODEBUG);
    if (!result_) result_ = consumeToken(builder_, KMETA);
    if (!result_) result_ = consumeToken(builder_, KOVERLOAD);
    if (!result_) result_ = consumeToken(builder_, KJSREQUIRE);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:meta' '(' ID '(' metaPartList? ')' ')'
  public static boolean metaMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaMeta")) return false;
    if (!nextTokenIs(builder_, KMETA)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KMETA);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, ID)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, metaMeta_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, META_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  // metaPartList?
  private static boolean metaMeta_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaMeta_4")) return false;
    metaPartList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // metaKeyValue (',' metaKeyValue)*
  static boolean metaPartList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = metaKeyValue(builder_, level_ + 1);
    result_ = result_ && metaPartList_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' metaKeyValue)*
  private static boolean metaPartList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!metaPartList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "metaPartList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' metaKeyValue
  private static boolean metaPartList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && metaKeyValue(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // ('*' | '/' | '%') (prefixExpression | suffixExpressionWrapper)
  public static boolean multiplicativeExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression")) return false;
    if (!nextTokenIs(builder_, "<multiplicative expression>", OREMAINDER, OMUL, OQUOTIENT)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<multiplicative expression>");
    result_ = multiplicativeExpression_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && multiplicativeExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, MULTIPLICATIVE_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // '*' | '/' | '%'
  private static boolean multiplicativeExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OMUL);
    if (!result_) result_ = consumeToken(builder_, OQUOTIENT);
    if (!result_) result_ = consumeToken(builder_, OREMAINDER);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // prefixExpression | suffixExpressionWrapper
  private static boolean multiplicativeExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = prefixExpression(builder_, level_ + 1);
    if (!result_) result_ = suffixExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // prefixExpression multiplicativeExpression*
  static boolean multiplicativeExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = prefixExpression(builder_, level_ + 1);
    result_ = result_ && multiplicativeExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // multiplicativeExpression*
  private static boolean multiplicativeExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!multiplicativeExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "multiplicativeExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // '@:native' '(' stringLiteralExpression ')'
  public static boolean nativeMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nativeMeta")) return false;
    if (!nextTokenIs(builder_, KNATIVE)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KNATIVE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, stringLiteralExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, NATIVE_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'new' type '(' expressionList? ')'
  public static boolean newExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "newExpression")) return false;
    if (!nextTokenIs(builder_, ONEW)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, ONEW);
    result_ = result_ && type(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, newExpression_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, NEW_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // expressionList?
  private static boolean newExpression_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "newExpression_3")) return false;
    expressionList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // newExpression qualifiedReferenceTail?
  static boolean newExpressionOrCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "newExpressionOrCall")) return false;
    if (!nextTokenIs(builder_, ONEW)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = newExpression(builder_, level_ + 1);
    result_ = result_ && newExpressionOrCall_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // qualifiedReferenceTail?
  private static boolean newExpressionOrCall_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "newExpressionOrCall_1")) return false;
    qualifiedReferenceTail(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // '@:nodebug'
  public static boolean noDebugMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "noDebugMeta")) return false;
    if (!nextTokenIs(builder_, KNODEBUG)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KNODEBUG);
    exit_section_(builder_, marker_, NO_DEBUG_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // ('untyped' statement ';'?)
  //                             | localVarDeclaration
  //                             | localFunctionDeclaration
  //                             | ifStatement
  //                             | forStatement
  //                             | whileStatement
  //                             | doWhileStatement
  //                             | returnStatement
  //                             | breakStatement
  //                             | continueStatement
  //                             | switchStatement
  //                             | throwStatement
  //                             | tryStatement
  //                             | expression
  static boolean notBlockStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KBREAK, KCAST,
      KCONTINUE, KDO, KFALSE, KFOR, KFUNCTION, KIF,
      ONEW, KNULL, KRETURN, KSUPER, KSWITCH, KTHIS,
      KTHROW, KTRUE, KTRY, KUNTYPED, KVAR, KWHILE,
      PLCURLY, OCOMPLEMENT, ID, LITFLOAT, LITHEX, LITINT,
      LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = notBlockStatement_0(builder_, level_ + 1);
    if (!result_) result_ = localVarDeclaration(builder_, level_ + 1);
    if (!result_) result_ = localFunctionDeclaration(builder_, level_ + 1);
    if (!result_) result_ = ifStatement(builder_, level_ + 1);
    if (!result_) result_ = forStatement(builder_, level_ + 1);
    if (!result_) result_ = whileStatement(builder_, level_ + 1);
    if (!result_) result_ = doWhileStatement(builder_, level_ + 1);
    if (!result_) result_ = returnStatement(builder_, level_ + 1);
    if (!result_) result_ = breakStatement(builder_, level_ + 1);
    if (!result_) result_ = continueStatement(builder_, level_ + 1);
    if (!result_) result_ = switchStatement(builder_, level_ + 1);
    if (!result_) result_ = throwStatement(builder_, level_ + 1);
    if (!result_) result_ = tryStatement(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'untyped' statement ';'?
  private static boolean notBlockStatement_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KUNTYPED);
    result_ = result_ && statement(builder_, level_ + 1);
    result_ = result_ && notBlockStatement_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ';'?
  private static boolean notBlockStatement_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_0_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // '@:ns' '(' stringLiteralExpression ')'
  public static boolean nsMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nsMeta")) return false;
    if (!nextTokenIs(builder_, KNS)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KNS);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, stringLiteralExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, NS_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '{' objectLiteralElementList? '}'
  public static boolean objectLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteral")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLCURLY);
    result_ = result_ && objectLiteral_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, OBJECT_LITERAL, result_);
    return result_;
  }

  // objectLiteralElementList?
  private static boolean objectLiteral_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteral_1")) return false;
    objectLiteralElementList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // identifier ':' expression
  public static boolean objectLiteralElement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElement")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<object literal element>");
    result_ = identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOLON);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, OBJECT_LITERAL_ELEMENT, result_, false, object_literal_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // objectLiteralElement (',' objectLiteralElement)*
  static boolean objectLiteralElementList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = objectLiteralElement(builder_, level_ + 1);
    result_ = result_ && objectLiteralElementList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, object_literal_list_recover_parser_);
    return result_;
  }

  // (',' objectLiteralElement)*
  private static boolean objectLiteralElementList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!objectLiteralElementList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "objectLiteralElementList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' objectLiteralElement
  private static boolean objectLiteralElementList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && objectLiteralElement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !('}')
  static boolean object_literal_list_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_list_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !object_literal_list_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // ('}')
  private static boolean object_literal_list_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_list_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !(',' | '}')
  static boolean object_literal_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_part_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !object_literal_part_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // ',' | '}'
  private static boolean object_literal_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_part_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:overload' '(' anonymousFunctionDeclaration ')'
  public static boolean overloadMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "overloadMeta")) return false;
    if (!nextTokenIs(builder_, KOVERLOAD)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KOVERLOAD);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, anonymousFunctionDeclaration(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, OVERLOAD_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'package' simpleQualifiedReferenceExpression? ';'
  public static boolean packageStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageStatement")) return false;
    if (!nextTokenIs(builder_, KPACKAGE)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KPACKAGE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, packageStatement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    exit_section_(builder_, level_, marker_, PACKAGE_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // simpleQualifiedReferenceExpression?
  private static boolean packageStatement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageStatement_1")) return false;
    simpleQualifiedReferenceExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // '?'? componentName typeTag? varInit?
  public static boolean parameter(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter")) return false;
    if (!nextTokenIs(builder_, "<parameter>", OQUEST, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<parameter>");
    result_ = parameter_0(builder_, level_ + 1);
    result_ = result_ && componentName(builder_, level_ + 1);
    result_ = result_ && parameter_2(builder_, level_ + 1);
    result_ = result_ && parameter_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PARAMETER, result_, false, null);
    return result_;
  }

  // '?'?
  private static boolean parameter_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_0")) return false;
    consumeToken(builder_, OQUEST);
    return true;
  }

  // typeTag?
  private static boolean parameter_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_2")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // varInit?
  private static boolean parameter_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_3")) return false;
    varInit(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // parameter (',' parameter)*
  public static boolean parameterList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<parameter list>");
    result_ = parameter(builder_, level_ + 1);
    result_ = result_ && parameterList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PARAMETER_LIST, result_, false, parameterListRecovery_parser_);
    return result_;
  }

  // (',' parameter)*
  private static boolean parameterList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!parameterList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "parameterList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' parameter
  private static boolean parameterList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && parameter(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !')'
  static boolean parameterListRecovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterListRecovery")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !consumeToken(builder_, PRPAREN);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '(' (expression | statement) ')'
  public static boolean parenthesizedExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, parenthesizedExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, PARENTHESIZED_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // expression | statement
  private static boolean parenthesizedExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = expression(builder_, level_ + 1);
    if (!result_) result_ = statement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // parenthesizedExpression qualifiedReferenceTail?
  static boolean parenthesizedExpressionOrCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpressionOrCall")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parenthesizedExpression(builder_, level_ + 1);
    result_ = result_ && parenthesizedExpressionOrCall_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // qualifiedReferenceTail?
  private static boolean parenthesizedExpressionOrCall_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpressionOrCall_1")) return false;
    qualifiedReferenceTail(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ('-' | '--' | '++' | '!' | '~') prefixExpression | suffixExpressionWrapper
  public static boolean prefixExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression")) return false;
    if (!nextTokenIs(builder_, "<prefix expression>", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, "<prefix expression>");
    result_ = prefixExpression_0(builder_, level_ + 1);
    if (!result_) result_ = suffixExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PREFIX_EXPRESSION, result_, false, null);
    return result_;
  }

  // ('-' | '--' | '++' | '!' | '~') prefixExpression
  private static boolean prefixExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = prefixExpression_0_0(builder_, level_ + 1);
    result_ = result_ && prefixExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // '-' | '--' | '++' | '!' | '~'
  private static boolean prefixExpression_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OMINUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (!result_) result_ = consumeToken(builder_, ONOT);
    if (!result_) result_ = consumeToken(builder_, OCOMPLEMENT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'private'
  public static boolean privateKeyWord(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "privateKeyWord")) return false;
    if (!nextTokenIs(builder_, KPRIVATE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KPRIVATE);
    exit_section_(builder_, marker_, PRIVATE_KEY_WORD, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'null' | 'default' | 'dynamic' | 'never' | 'get' | 'set' | referenceExpression
  public static boolean propertyAccessor(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyAccessor")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<property accessor>");
    result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KNEVER);
    if (!result_) result_ = consumeToken(builder_, "get");
    if (!result_) result_ = consumeToken(builder_, "set");
    if (!result_) result_ = referenceExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, PROPERTY_ACCESSOR, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '(' propertyAccessor ',' propertyAccessor ')'
  public static boolean propertyDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyDeclaration")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && propertyAccessor(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOMMA);
    result_ = result_ && propertyAccessor(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    exit_section_(builder_, marker_, PROPERTY_DECLARATION, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:protected'
  public static boolean protectedMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "protectedMeta")) return false;
    if (!nextTokenIs(builder_, KPROTECTED)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KPROTECTED);
    exit_section_(builder_, marker_, PROTECTED_META, result_);
    return result_;
  }

  /* ********************************************************** */
  // '.' referenceExpression
  public static boolean qualifiedReferenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceExpression")) return false;
    if (!nextTokenIs(builder_, ODOT)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, ODOT);
    pinned_ = result_; // pin = 1
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, REFERENCE_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '.' referenceExpression
  public static boolean qualifiedReferenceExpressionOrWildcard(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceExpressionOrWildcard")) return false;
    if (!nextTokenIs(builder_, ODOT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, ODOT);
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, REFERENCE_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // qualifiedReferenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  static boolean qualifiedReferenceTail(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail")) return false;
    if (!nextTokenIs(builder_, ODOT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    result_ = result_ && qualifiedReferenceTail_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean qualifiedReferenceTail_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!qualifiedReferenceTail_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "qualifiedReferenceTail_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean qualifiedReferenceTail_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean referenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceExpression")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = identifier(builder_, level_ + 1);
    exit_section_(builder_, marker_, REFERENCE_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // REG_EXP
  public static boolean regularExpressionLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "regularExpressionLiteral")) return false;
    if (!nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, REG_EXP);
    exit_section_(builder_, marker_, REGULAR_EXPRESSION_LITERAL, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:require' '(' identifier ')'
  public static boolean requireMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "requireMeta")) return false;
    if (!nextTokenIs(builder_, KREQUIRE)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KREQUIRE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, identifier(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, REQUIRE_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'return' expression? ';'?
  public static boolean returnStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement")) return false;
    if (!nextTokenIs(builder_, KRETURN)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KRETURN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, returnStatement_1(builder_, level_ + 1));
    result_ = pinned_ && returnStatement_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, RETURN_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // expression?
  private static boolean returnStatement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  // ';'?
  private static boolean returnStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // 'return' expression
  public static boolean returnStatementWithoutSemicolon(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatementWithoutSemicolon")) return false;
    if (!nextTokenIs(builder_, KRETURN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KRETURN);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, RETURN_STATEMENT_WITHOUT_SEMICOLON, result_);
    return result_;
  }

  /* ********************************************************** */
  // '@:setter' '(' referenceExpression ')'
  public static boolean setterMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setterMeta")) return false;
    if (!nextTokenIs(builder_, KSETTER)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KSETTER);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, referenceExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    exit_section_(builder_, level_, marker_, SETTER_META, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // shiftOperator additiveExpressionWrapper
  public static boolean shiftExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpression")) return false;
    if (!nextTokenIs(builder_, "<shift expression>", OSHIFT_LEFT, OGREATER)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<shift expression>");
    result_ = shiftOperator(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && additiveExpressionWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SHIFT_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // additiveExpressionWrapper shiftExpression*
  static boolean shiftExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = additiveExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && shiftExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // shiftExpression*
  private static boolean shiftExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!shiftExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "shiftExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // unsignedShiftRightOperator | shiftRightOperator | '<<'
  public static boolean shiftOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftOperator")) return false;
    if (!nextTokenIs(builder_, "<shift operator>", OSHIFT_LEFT, OGREATER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<shift operator>");
    result_ = unsignedShiftRightOperator(builder_, level_ + 1);
    if (!result_) result_ = shiftRightOperator(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_LEFT);
    exit_section_(builder_, level_, marker_, SHIFT_OPERATOR, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '>' '>'
  public static boolean shiftRightOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftRightOperator")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    exit_section_(builder_, marker_, SHIFT_RIGHT_OPERATOR, result_);
    return result_;
  }

  /* ********************************************************** */
  // SHORT_TEMPLATE_ENTRY_START (thisExpression | referenceExpression)
  public static boolean shortTemplateEntry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shortTemplateEntry")) return false;
    if (!nextTokenIs(builder_, SHORT_TEMPLATE_ENTRY_START)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, SHORT_TEMPLATE_ENTRY_START);
    pinned_ = result_; // pin = 1
    result_ = result_ && shortTemplateEntry_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SHORT_TEMPLATE_ENTRY, result_, pinned_, null);
    return result_ || pinned_;
  }

  // thisExpression | referenceExpression
  private static boolean shortTemplateEntry_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shortTemplateEntry_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = thisExpression(builder_, level_ + 1);
    if (!result_) result_ = referenceExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // anonymousTypeFieldList (',' interfaceBody)?
  static boolean simpleAnonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleAnonymousTypeBody")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = anonymousTypeFieldList(builder_, level_ + 1);
    result_ = result_ && simpleAnonymousTypeBody_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' interfaceBody)?
  private static boolean simpleAnonymousTypeBody_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleAnonymousTypeBody_1")) return false;
    simpleAnonymousTypeBody_1_0(builder_, level_ + 1);
    return true;
  }

  // ',' interfaceBody
  private static boolean simpleAnonymousTypeBody_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleAnonymousTypeBody_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && interfaceBody(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // unreflectiveMeta | finalMeta | keepMeta | coreApiMeta | bindMeta | macroMeta | hackMeta
  public static boolean simpleMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleMeta")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<simple meta>");
    result_ = unreflectiveMeta(builder_, level_ + 1);
    if (!result_) result_ = finalMeta(builder_, level_ + 1);
    if (!result_) result_ = keepMeta(builder_, level_ + 1);
    if (!result_) result_ = coreApiMeta(builder_, level_ + 1);
    if (!result_) result_ = bindMeta(builder_, level_ + 1);
    if (!result_) result_ = macroMeta(builder_, level_ + 1);
    if (!result_) result_ = hackMeta(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SIMPLE_META, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpression *
  public static boolean simpleQualifiedReferenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleQualifiedReferenceExpression")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, null);
    result_ = referenceExpression(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && simpleQualifiedReferenceExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, REFERENCE_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // qualifiedReferenceExpression *
  private static boolean simpleQualifiedReferenceExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleQualifiedReferenceExpression_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!qualifiedReferenceExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "simpleQualifiedReferenceExpression_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpressionOrWildcard * wildcard
  public static boolean simpleQualifiedReferenceExpressionWithWildcardSupport(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleQualifiedReferenceExpressionWithWildcardSupport")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && simpleQualifiedReferenceExpressionWithWildcardSupport_1(builder_, level_ + 1);
    result_ = result_ && wildcard(builder_, level_ + 1);
    exit_section_(builder_, marker_, REFERENCE_EXPRESSION, result_);
    return result_;
  }

  // qualifiedReferenceExpressionOrWildcard *
  private static boolean simpleQualifiedReferenceExpressionWithWildcardSupport_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleQualifiedReferenceExpressionWithWildcardSupport_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!qualifiedReferenceExpressionOrWildcard(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "simpleQualifiedReferenceExpressionWithWildcardSupport_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // blockStatement | notBlockStatement
  static boolean statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KBREAK, KCAST,
      KCONTINUE, KDO, KFALSE, KFOR, KFUNCTION, KIF,
      ONEW, KNULL, KRETURN, KSUPER, KSWITCH, KTHIS,
      KTHROW, KTRUE, KTRY, KUNTYPED, KVAR, KWHILE,
      PLCURLY, OCOMPLEMENT, ID, LITFLOAT, LITHEX, LITINT,
      LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = blockStatement(builder_, level_ + 1);
    if (!result_) result_ = notBlockStatement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (statement ';'?)+
  static boolean statementList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = statementList_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!statementList_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "statementList", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, level_, marker_, null, result_, false, statement_recovery_parser_);
    return result_;
  }

  // statement ';'?
  private static boolean statementList_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = statement(builder_, level_ + 1);
    result_ = result_ && statementList_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ';'?
  private static boolean statementList_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList_0_1")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // !('case' | 'default' | '}')
  static boolean statement_recovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !statement_recovery_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // 'case' | 'default' | '}'
  private static boolean statement_recovery_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // OPEN_QUOTE (REGULAR_STRING_PART | shortTemplateEntry | longTemplateEntry)* CLOSING_QUOTE
  public static boolean stringLiteralExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringLiteralExpression")) return false;
    if (!nextTokenIs(builder_, OPEN_QUOTE)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, OPEN_QUOTE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, stringLiteralExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, CLOSING_QUOTE) && result_;
    exit_section_(builder_, level_, marker_, STRING_LITERAL_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (REGULAR_STRING_PART | shortTemplateEntry | longTemplateEntry)*
  private static boolean stringLiteralExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringLiteralExpression_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!stringLiteralExpression_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "stringLiteralExpression_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // REGULAR_STRING_PART | shortTemplateEntry | longTemplateEntry
  private static boolean stringLiteralExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "stringLiteralExpression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, REGULAR_STRING_PART);
    if (!result_) result_ = shortTemplateEntry(builder_, level_ + 1);
    if (!result_) result_ = longTemplateEntry(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '--' | '++'
  public static boolean suffixExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpression")) return false;
    if (!nextTokenIs(builder_, "<suffix expression>", OPLUS_PLUS, OMINUS_MINUS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, "<suffix expression>");
    result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    exit_section_(builder_, level_, marker_, SUFFIX_EXPRESSION, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // value suffixExpression*
  static boolean suffixExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", PLPAREN, PLBRACK,
      KCAST, KFALSE, KFUNCTION, KIF, ONEW, KNULL,
      KSUPER, KSWITCH, KTHIS, KTRUE, KTRY, KUNTYPED,
      PLCURLY, ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = value(builder_, level_ + 1);
    result_ = result_ && suffixExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // suffixExpression*
  private static boolean suffixExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpressionWrapper_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!suffixExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "suffixExpressionWrapper_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // 'super'
  public static boolean superExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "superExpression")) return false;
    if (!nextTokenIs(builder_, KSUPER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KSUPER);
    exit_section_(builder_, marker_, SUPER_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // '{'  switchCase* defaultCase? '}'
  public static boolean switchBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchBlock")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, PLCURLY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, switchBlock_1(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, switchBlock_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    exit_section_(builder_, level_, marker_, SWITCH_BLOCK, result_, pinned_, null);
    return result_ || pinned_;
  }

  // switchCase*
  private static boolean switchBlock_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchBlock_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!switchCase(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "switchBlock_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // defaultCase?
  private static boolean switchBlock_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchBlock_2")) return false;
    defaultCase(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // ('case' switchCaseExpression (',' switchCaseExpression)* ':')+ switchCaseBlock?
  public static boolean switchCase(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase")) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<switch case>");
    result_ = switchCase_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && switchCase_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SWITCH_CASE, result_, pinned_, switch_case_recover_parser_);
    return result_ || pinned_;
  }

  // ('case' switchCaseExpression (',' switchCaseExpression)* ':')+
  private static boolean switchCase_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = switchCase_0_0(builder_, level_ + 1);
    int pos_ = current_position_(builder_);
    while (result_) {
      if (!switchCase_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "switchCase_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'case' switchCaseExpression (',' switchCaseExpression)* ':'
  private static boolean switchCase_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KCASE);
    result_ = result_ && switchCaseExpression(builder_, level_ + 1);
    result_ = result_ && switchCase_0_0_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOLON);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' switchCaseExpression)*
  private static boolean switchCase_0_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0_2")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!switchCase_0_0_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "switchCase_0_0_2", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' switchCaseExpression
  private static boolean switchCase_0_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && switchCaseExpression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // switchCaseBlock?
  private static boolean switchCase_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_1")) return false;
    switchCaseBlock(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // (literalExpression ";") | statementList
  public static boolean switchCaseBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCaseBlock")) return false;
    if (!nextTokenIs(builder_, "<switch case block>", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KBREAK, KCAST,
      KCONTINUE, KDO, KFALSE, KFOR, KFUNCTION, KIF,
      ONEW, KNULL, KRETURN, KSUPER, KSWITCH, KTHIS,
      KTHROW, KTRUE, KTRY, KUNTYPED, KVAR, KWHILE,
      PLCURLY, OCOMPLEMENT, ID, LITFLOAT, LITHEX, LITINT,
      LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<switch case block>");
    result_ = switchCaseBlock_0(builder_, level_ + 1);
    if (!result_) result_ = statementList(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SWITCH_CASE_BLOCK, result_, false, null);
    return result_;
  }

  // literalExpression ";"
  private static boolean switchCaseBlock_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCaseBlock_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = literalExpression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OSEMI);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // expression ('if' '(' expression ')')?
  public static boolean switchCaseExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCaseExpression")) return false;
    if (!nextTokenIs(builder_, "<switch case expression>", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, "<switch case expression>");
    result_ = expression(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && switchCaseExpression_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, SWITCH_CASE_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ('if' '(' expression ')')?
  private static boolean switchCaseExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCaseExpression_1")) return false;
    switchCaseExpression_1_0(builder_, level_ + 1);
    return true;
  }

  // 'if' '(' expression ')'
  private static boolean switchCaseExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCaseExpression_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KIF);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'switch' expression switchBlock
  public static boolean switchStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchStatement")) return false;
    if (!nextTokenIs(builder_, KSWITCH)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KSWITCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && switchBlock(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, SWITCH_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // !('case' | 'default' | '}' | ID)
  static boolean switch_case_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switch_case_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !switch_case_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // 'case' | 'default' | '}' | ID
  private static boolean switch_case_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switch_case_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, ID);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '?' expression ':' ternaryExpressionWrapper
  public static boolean ternaryExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternaryExpression")) return false;
    if (!nextTokenIs(builder_, OQUEST)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, OQUEST);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OCOLON)) && result_;
    result_ = pinned_ && ternaryExpressionWrapper(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, TERNARY_EXPRESSION, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // logicOrExpressionWrapper ternaryExpression?
  static boolean ternaryExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternaryExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, "", ONOT, PLPAREN,
      OPLUS_PLUS, OMINUS, OMINUS_MINUS, PLBRACK, KCAST, KFALSE,
      KFUNCTION, KIF, ONEW, KNULL, KSUPER, KSWITCH,
      KTHIS, KTRUE, KTRY, KUNTYPED, PLCURLY, OCOMPLEMENT,
      ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = logicOrExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && ternaryExpressionWrapper_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ternaryExpression?
  private static boolean ternaryExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternaryExpressionWrapper_1")) return false;
    ternaryExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // 'this'
  public static boolean thisExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "thisExpression")) return false;
    if (!nextTokenIs(builder_, KTHIS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KTHIS);
    exit_section_(builder_, marker_, THIS_EXPRESSION, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'throw' expression ';'?
  public static boolean throwStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "throwStatement")) return false;
    if (!nextTokenIs(builder_, KTHROW)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KTHROW);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && throwStatement_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, THROW_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean throwStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "throwStatement_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // importStatementAll | usingStatement | topLevelDeclaration
  static boolean topLevel(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevel")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = importStatementAll(builder_, level_ + 1);
    if (!result_) result_ = usingStatement(builder_, level_ + 1);
    if (!result_) result_ = topLevelDeclaration(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, top_level_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // classDeclaration
  //                               | externClassDeclaration
  //                               | abstractClassDeclaration
  //                               | interfaceDeclaration
  //                               | enumDeclaration
  //                               | typedefDeclaration
  static boolean topLevelDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevelDeclaration")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = classDeclaration(builder_, level_ + 1);
    if (!result_) result_ = externClassDeclaration(builder_, level_ + 1);
    if (!result_) result_ = abstractClassDeclaration(builder_, level_ + 1);
    if (!result_) result_ = interfaceDeclaration(builder_, level_ + 1);
    if (!result_) result_ = enumDeclaration(builder_, level_ + 1);
    if (!result_) result_ = typedefDeclaration(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // topLevel*
  static boolean topLevelList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevelList")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!topLevel(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "topLevelList", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'abstract' | 'class'  | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef')
  static boolean top_level_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !top_level_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | metaKeyWord | 'abstract' | 'class'  | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef'
  private static boolean top_level_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = metaKeyWord(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KABSTRACT);
    if (!result_) result_ = consumeToken(builder_, KCLASS);
    if (!result_) result_ = consumeToken(builder_, KENUM);
    if (!result_) result_ = consumeToken(builder_, KEXTERN);
    if (!result_) result_ = consumeToken(builder_, KIMPORT);
    if (!result_) result_ = consumeToken(builder_, KUSING);
    if (!result_) result_ = consumeToken(builder_, KINTERFACE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KTYPEDEF);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'try' statement ';'? catchStatement*
  public static boolean tryStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement")) return false;
    if (!nextTokenIs(builder_, KTRY)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KTRY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, statement(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, tryStatement_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && tryStatement_3(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, TRY_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean tryStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  // catchStatement*
  private static boolean tryStatement_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_3")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!catchStatement(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "tryStatement_3", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpression* typeParam?
  public static boolean type(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && type_1(builder_, level_ + 1);
    result_ = result_ && type_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, TYPE, result_);
    return result_;
  }

  // qualifiedReferenceExpression*
  private static boolean type_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!qualifiedReferenceExpression(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "type_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // typeParam?
  private static boolean type_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_2")) return false;
    typeParam(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // '>' type
  public static boolean typeExtends(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeExtends")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && type(builder_, level_ + 1);
    exit_section_(builder_, marker_, TYPE_EXTENDS, result_);
    return result_;
  }

  /* ********************************************************** */
  // typeListPart (',' typeListPart)*
  public static boolean typeList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList")) return false;
    if (!nextTokenIs(builder_, "<type list>", PLPAREN, PLCURLY, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type list>");
    result_ = typeListPart(builder_, level_ + 1);
    result_ = result_ && typeList_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_LIST, result_, false, null);
    return result_;
  }

  // (',' typeListPart)*
  private static boolean typeList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!typeList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "typeList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' typeListPart
  private static boolean typeList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && typeListPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // functionTypeWrapper
  public static boolean typeListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeListPart")) return false;
    if (!nextTokenIs(builder_, "<type list part>", PLPAREN, PLCURLY, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type list part>");
    result_ = functionTypeWrapper(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_LIST_PART, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // type | anonymousType
  public static boolean typeOrAnonymous(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeOrAnonymous")) return false;
    if (!nextTokenIs(builder_, "<type or anonymous>", PLCURLY, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<type or anonymous>");
    result_ = type(builder_, level_ + 1);
    if (!result_) result_ = anonymousType(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, TYPE_OR_ANONYMOUS, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '<' typeList '>'
  public static boolean typeParam(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParam")) return false;
    if (!nextTokenIs(builder_, OLESS)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OLESS);
    result_ = result_ && typeList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OGREATER);
    exit_section_(builder_, marker_, TYPE_PARAM, result_);
    return result_;
  }

  /* ********************************************************** */
  // ':' functionTypeWrapper
  public static boolean typeTag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeTag")) return false;
    if (!nextTokenIs(builder_, OCOLON)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOLON);
    result_ = result_ && functionTypeWrapper(builder_, level_ + 1);
    exit_section_(builder_, marker_, TYPE_TAG, result_);
    return result_;
  }

  /* ********************************************************** */
  // externOrPrivate? 'typedef' componentName genericParam? '=' functionTypeWrapper ';'?
  public static boolean typedefDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedefDeclaration")) return false;
    if (!nextTokenIs(builder_, "<typedef declaration>", KEXTERN, KPRIVATE, KTYPEDEF)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<typedef declaration>");
    result_ = typedefDeclaration_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KTYPEDEF);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, typedefDeclaration_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OASSIGN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionTypeWrapper(builder_, level_ + 1)) && result_;
    result_ = pinned_ && typedefDeclaration_6(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, TYPEDEF_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // externOrPrivate?
  private static boolean typedefDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedefDeclaration_0")) return false;
    externOrPrivate(builder_, level_ + 1);
    return true;
  }

  // genericParam?
  private static boolean typedefDeclaration_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedefDeclaration_3")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // ';'?
  private static boolean typedefDeclaration_6(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedefDeclaration_6")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // '@:unreflective'
  public static boolean unreflectiveMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unreflectiveMeta")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<unreflective meta>");
    result_ = consumeToken(builder_, "@:unreflective");
    exit_section_(builder_, level_, marker_, UNREFLECTIVE_META, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '>' '>' '>'
  public static boolean unsignedShiftRightOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unsignedShiftRightOperator")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    exit_section_(builder_, marker_, UNSIGNED_SHIFT_RIGHT_OPERATOR, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'using' simpleQualifiedReferenceExpression ';'
  public static boolean usingStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usingStatement")) return false;
    if (!nextTokenIs(builder_, KUSING)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KUSING);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, simpleQualifiedReferenceExpression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    exit_section_(builder_, level_, marker_, USING_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'untyped' expression
  //                 | (literalExpression qualifiedReferenceTail?)
  //                 | ifStatement
  //                 | castExpression qualifiedReferenceTail?
  //                 | newExpressionOrCall
  //                 | parenthesizedExpressionOrCall
  //                 | callOrArrayAccess
  //                 | tryStatement
  //                 | switchStatement
  static boolean value(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value")) return false;
    if (!nextTokenIs(builder_, "", PLPAREN, PLBRACK,
      KCAST, KFALSE, KFUNCTION, KIF, ONEW, KNULL,
      KSUPER, KSWITCH, KTHIS, KTRUE, KTRY, KUNTYPED,
      PLCURLY, ID, LITFLOAT, LITHEX, LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = value_0(builder_, level_ + 1);
    if (!result_) result_ = value_1(builder_, level_ + 1);
    if (!result_) result_ = ifStatement(builder_, level_ + 1);
    if (!result_) result_ = value_3(builder_, level_ + 1);
    if (!result_) result_ = newExpressionOrCall(builder_, level_ + 1);
    if (!result_) result_ = parenthesizedExpressionOrCall(builder_, level_ + 1);
    if (!result_) result_ = callOrArrayAccess(builder_, level_ + 1);
    if (!result_) result_ = tryStatement(builder_, level_ + 1);
    if (!result_) result_ = switchStatement(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // 'untyped' expression
  private static boolean value_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, KUNTYPED);
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // literalExpression qualifiedReferenceTail?
  private static boolean value_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = literalExpression(builder_, level_ + 1);
    result_ = result_ && value_1_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // qualifiedReferenceTail?
  private static boolean value_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_1_1")) return false;
    qualifiedReferenceTail(builder_, level_ + 1);
    return true;
  }

  // castExpression qualifiedReferenceTail?
  private static boolean value_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_3")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = castExpression(builder_, level_ + 1);
    result_ = result_ && value_3_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // qualifiedReferenceTail?
  private static boolean value_3_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_3_1")) return false;
    qualifiedReferenceTail(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // (macroMember | declarationAttribute)* 'var' varDeclarationPartList ';'
  public static boolean varDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclaration")) return false;
    if (!nextTokenIs(builder_, "<var declaration>", KAUTOBUILD, KBUILD,
      KDEBUG, KGETTER, KKEEP, KMACRO, KMETA, KNODEBUG,
      KNS, KPROTECTED, KREQUIRE, KSETTER, KDYNAMIC, KINLINE,
      KMACRO2, KOVERRIDE, KPRIVATE, KPUBLIC, KSTATIC, KVAR, MACRO_ID)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<var declaration>");
    result_ = varDeclaration_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KVAR);
    result_ = result_ && varDeclarationPartList(builder_, level_ + 1);
    pinned_ = result_; // pin = 3
    result_ = result_ && consumeToken(builder_, OSEMI);
    exit_section_(builder_, level_, marker_, VAR_DECLARATION, result_, pinned_, null);
    return result_ || pinned_;
  }

  // (macroMember | declarationAttribute)*
  private static boolean varDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclaration_0")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!varDeclaration_0_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "varDeclaration_0", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // macroMember | declarationAttribute
  private static boolean varDeclaration_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclaration_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = macroMember(builder_, level_ + 1);
    if (!result_) result_ = declarationAttribute(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // componentName propertyDeclaration? typeTag? varInit?
  public static boolean varDeclarationPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPart")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, "<var declaration part>");
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_1(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_2(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_3(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, VAR_DECLARATION_PART, result_, false, var_declaration_part_recover_parser_);
    return result_;
  }

  // propertyDeclaration?
  private static boolean varDeclarationPart_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPart_1")) return false;
    propertyDeclaration(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean varDeclarationPart_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPart_2")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // varInit?
  private static boolean varDeclarationPart_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPart_3")) return false;
    varInit(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // varDeclarationPart (',' varDeclarationPart)*
  static boolean varDeclarationPartList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPartList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = varDeclarationPart(builder_, level_ + 1);
    result_ = result_ && varDeclarationPartList_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (',' varDeclarationPart)*
  private static boolean varDeclarationPartList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPartList_1")) return false;
    int pos_ = current_position_(builder_);
    while (true) {
      if (!varDeclarationPartList_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "varDeclarationPartList_1", pos_)) break;
      pos_ = current_position_(builder_);
    }
    return true;
  }

  // ',' varDeclarationPart
  private static boolean varDeclarationPartList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPartList_1_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && varDeclarationPart(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // '=' expression
  public static boolean varInit(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varInit")) return false;
    if (!nextTokenIs(builder_, OASSIGN)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, OASSIGN);
    pinned_ = result_; // pin = 1
    result_ = result_ && expression(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, VAR_INIT, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // !(';' | ',')
  static boolean var_declaration_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "var_declaration_part_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_, null);
    result_ = !var_declaration_part_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, null, result_, false, null);
    return result_;
  }

  // ';' | ','
  private static boolean var_declaration_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "var_declaration_part_recover_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, OSEMI);
    if (!result_) result_ = consumeToken(builder_, OCOMMA);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // 'while' '(' expression ')' statement ';'?
  public static boolean whileStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whileStatement")) return false;
    if (!nextTokenIs(builder_, KWHILE)) return false;
    boolean result_;
    boolean pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null);
    result_ = consumeToken(builder_, KWHILE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && whileStatement_5(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, WHILE_STATEMENT, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean whileStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whileStatement_5")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // '.' '*'
  public static boolean wildcard(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "wildcard")) return false;
    if (!nextTokenIs(builder_, ODOT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, null);
    result_ = consumeToken(builder_, ODOT);
    result_ = result_ && consumeToken(builder_, OMUL);
    exit_section_(builder_, level_, marker_, WILDCARD, result_, false, null);
    return result_;
  }

  final static Parser class_body_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return class_body_part_recover(builder_, level_ + 1);
    }
  };
  final static Parser enum_value_declaration_recovery_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return enum_value_declaration_recovery(builder_, level_ + 1);
    }
  };
  final static Parser expression_list_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return expression_list_recover(builder_, level_ + 1);
    }
  };
  final static Parser expression_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return expression_recover(builder_, level_ + 1);
    }
  };
  final static Parser extern_class_body_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return extern_class_body_part_recover(builder_, level_ + 1);
    }
  };
  final static Parser inherit_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return inherit_recover(builder_, level_ + 1);
    }
  };
  final static Parser interface_body_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return interface_body_part_recover(builder_, level_ + 1);
    }
  };
  final static Parser local_var_declaration_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return local_var_declaration_part_recover(builder_, level_ + 1);
    }
  };
  final static Parser object_literal_list_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return object_literal_list_recover(builder_, level_ + 1);
    }
  };
  final static Parser object_literal_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return object_literal_part_recover(builder_, level_ + 1);
    }
  };
  final static Parser parameterListRecovery_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return parameterListRecovery(builder_, level_ + 1);
    }
  };
  final static Parser statement_recovery_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return statement_recovery(builder_, level_ + 1);
    }
  };
  final static Parser switch_case_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return switch_case_recover(builder_, level_ + 1);
    }
  };
  final static Parser top_level_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return top_level_recover(builder_, level_ + 1);
    }
  };
  final static Parser var_declaration_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder builder_, int level_) {
      return var_declaration_part_recover(builder_, level_ + 1);
    }
  };
}
