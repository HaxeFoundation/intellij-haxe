/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HaxeParser implements PsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == ABSTRACT_CLASS_DECLARATION) {
      r = abstractClassDeclaration(b, 0);
    }
    else if (t == ACCESS) {
      r = access(b, 0);
    }
    else if (t == ADDITIVE_EXPRESSION) {
      r = additiveExpression(b, 0);
    }
    else if (t == ANONYMOUS_FUNCTION_DECLARATION) {
      r = anonymousFunctionDeclaration(b, 0);
    }
    else if (t == ANONYMOUS_TYPE) {
      r = anonymousType(b, 0);
    }
    else if (t == ANONYMOUS_TYPE_BODY) {
      r = anonymousTypeBody(b, 0);
    }
    else if (t == ANONYMOUS_TYPE_FIELD) {
      r = anonymousTypeField(b, 0);
    }
    else if (t == ANONYMOUS_TYPE_FIELD_LIST) {
      r = anonymousTypeFieldList(b, 0);
    }
    else if (t == ARRAY_ACCESS_EXPRESSION) {
      r = arrayAccessExpression(b, 0);
    }
    else if (t == ARRAY_LITERAL) {
      r = arrayLiteral(b, 0);
    }
    else if (t == ASSIGN_EXPRESSION) {
      r = assignExpression(b, 0);
    }
    else if (t == ASSIGN_OPERATION) {
      r = assignOperation(b, 0);
    }
    else if (t == AUTO_BUILD_MACRO) {
      r = autoBuildMacro(b, 0);
    }
    else if (t == BIND_META) {
      r = bindMeta(b, 0);
    }
    else if (t == BIT_OPERATION) {
      r = bitOperation(b, 0);
    }
    else if (t == BITMAP_META) {
      r = bitmapMeta(b, 0);
    }
    else if (t == BITWISE_EXPRESSION) {
      r = bitwiseExpression(b, 0);
    }
    else if (t == BLOCK_STATEMENT) {
      r = blockStatement(b, 0);
    }
    else if (t == BREAK_STATEMENT) {
      r = breakStatement(b, 0);
    }
    else if (t == BUILD_MACRO) {
      r = buildMacro(b, 0);
    }
    else if (t == CALL_EXPRESSION) {
      r = callExpression(b, 0);
    }
    else if (t == CAST_EXPRESSION) {
      r = castExpression(b, 0);
    }
    else if (t == CATCH_STATEMENT) {
      r = catchStatement(b, 0);
    }
    else if (t == CLASS_BODY) {
      r = classBody(b, 0);
    }
    else if (t == CLASS_DECLARATION) {
      r = classDeclaration(b, 0);
    }
    else if (t == COMPARE_EXPRESSION) {
      r = compareExpression(b, 0);
    }
    else if (t == COMPARE_OPERATION) {
      r = compareOperation(b, 0);
    }
    else if (t == COMPONENT_NAME) {
      r = componentName(b, 0);
    }
    else if (t == CONTINUE_STATEMENT) {
      r = continueStatement(b, 0);
    }
    else if (t == CORE_API_META) {
      r = coreApiMeta(b, 0);
    }
    else if (t == CUSTOM_META) {
      r = customMeta(b, 0);
    }
    else if (t == DEBUG_META) {
      r = debugMeta(b, 0);
    }
    else if (t == DECLARATION_ATTRIBUTE) {
      r = declarationAttribute(b, 0);
    }
    else if (t == DEFAULT_CASE) {
      r = defaultCase(b, 0);
    }
    else if (t == DO_WHILE_STATEMENT) {
      r = doWhileStatement(b, 0);
    }
    else if (t == ENUM_BODY) {
      r = enumBody(b, 0);
    }
    else if (t == ENUM_CONSTRUCTOR_PARAMETERS) {
      r = enumConstructorParameters(b, 0);
    }
    else if (t == ENUM_DECLARATION) {
      r = enumDeclaration(b, 0);
    }
    else if (t == ENUM_VALUE_DECLARATION) {
      r = enumValueDeclaration(b, 0);
    }
    else if (t == EXPRESSION) {
      r = expression(b, 0);
    }
    else if (t == EXPRESSION_LIST) {
      r = expressionList(b, 0);
    }
    else if (t == EXTENDS_DECLARATION) {
      r = extendsDeclaration(b, 0);
    }
    else if (t == EXTERN_CLASS_DECLARATION) {
      r = externClassDeclaration(b, 0);
    }
    else if (t == EXTERN_CLASS_DECLARATION_BODY) {
      r = externClassDeclarationBody(b, 0);
    }
    else if (t == EXTERN_FUNCTION_DECLARATION) {
      r = externFunctionDeclaration(b, 0);
    }
    else if (t == EXTERN_INTERFACE_DECLARATION) {
      r = externInterfaceDeclaration(b, 0);
    }
    else if (t == EXTERN_KEY_WORD) {
      r = externKeyWord(b, 0);
    }
    else if (t == FAKE_ENUM_META) {
      r = fakeEnumMeta(b, 0);
    }
    else if (t == FAT_ARROW_EXPRESSION) {
      r = fatArrowExpression(b, 0);
    }
    else if (t == FINAL_META) {
      r = finalMeta(b, 0);
    }
    else if (t == FOR_STATEMENT) {
      r = forStatement(b, 0);
    }
    else if (t == FUNCTION_DECLARATION_WITH_ATTRIBUTES) {
      r = functionDeclarationWithAttributes(b, 0);
    }
    else if (t == FUNCTION_LITERAL) {
      r = functionLiteral(b, 0);
    }
    else if (t == FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES) {
      r = functionPrototypeDeclarationWithAttributes(b, 0);
    }
    else if (t == FUNCTION_TYPE) {
      r = functionType(b, 0);
    }
    else if (t == GENERIC_LIST_PART) {
      r = genericListPart(b, 0);
    }
    else if (t == GENERIC_PARAM) {
      r = genericParam(b, 0);
    }
    else if (t == GETTER_META) {
      r = getterMeta(b, 0);
    }
    else if (t == HACK_META) {
      r = hackMeta(b, 0);
    }
    else if (t == IDENTIFIER) {
      r = identifier(b, 0);
    }
    else if (t == IF_STATEMENT) {
      r = ifStatement(b, 0);
    }
    else if (t == IMPLEMENTS_DECLARATION) {
      r = implementsDeclaration(b, 0);
    }
    else if (t == IMPORT_STATEMENT_REGULAR) {
      r = importStatementRegular(b, 0);
    }
    else if (t == IMPORT_STATEMENT_WITH_IN_SUPPORT) {
      r = importStatementWithInSupport(b, 0);
    }
    else if (t == IMPORT_STATEMENT_WITH_WILDCARD) {
      r = importStatementWithWildcard(b, 0);
    }
    else if (t == INHERIT_LIST) {
      r = inheritList(b, 0);
    }
    else if (t == INTERFACE_BODY) {
      r = interfaceBody(b, 0);
    }
    else if (t == INTERFACE_DECLARATION) {
      r = interfaceDeclaration(b, 0);
    }
    else if (t == ITERABLE) {
      r = iterable(b, 0);
    }
    else if (t == ITERATOR_EXPRESSION) {
      r = iteratorExpression(b, 0);
    }
    else if (t == JS_REQUIRE_META) {
      r = jsRequireMeta(b, 0);
    }
    else if (t == KEEP_META) {
      r = keepMeta(b, 0);
    }
    else if (t == LITERAL_EXPRESSION) {
      r = literalExpression(b, 0);
    }
    else if (t == LOCAL_FUNCTION_DECLARATION) {
      r = localFunctionDeclaration(b, 0);
    }
    else if (t == LOCAL_VAR_DECLARATION) {
      r = localVarDeclaration(b, 0);
    }
    else if (t == LOCAL_VAR_DECLARATION_PART) {
      r = localVarDeclarationPart(b, 0);
    }
    else if (t == LOGIC_AND_EXPRESSION) {
      r = logicAndExpression(b, 0);
    }
    else if (t == LOGIC_OR_EXPRESSION) {
      r = logicOrExpression(b, 0);
    }
    else if (t == LONG_TEMPLATE_ENTRY) {
      r = longTemplateEntry(b, 0);
    }
    else if (t == MACRO_CLASS) {
      r = macroClass(b, 0);
    }
    else if (t == MACRO_CLASS_LIST) {
      r = macroClassList(b, 0);
    }
    else if (t == META_KEY_VALUE) {
      r = metaKeyValue(b, 0);
    }
    else if (t == META_META) {
      r = metaMeta(b, 0);
    }
    else if (t == MULTIPLICATIVE_EXPRESSION) {
      r = multiplicativeExpression(b, 0);
    }
    else if (t == NATIVE_META) {
      r = nativeMeta(b, 0);
    }
    else if (t == NEW_EXPRESSION) {
      r = newExpression(b, 0);
    }
    else if (t == NO_DEBUG_META) {
      r = noDebugMeta(b, 0);
    }
    else if (t == NS_META) {
      r = nsMeta(b, 0);
    }
    else if (t == OBJECT_LITERAL) {
      r = objectLiteral(b, 0);
    }
    else if (t == OBJECT_LITERAL_ELEMENT) {
      r = objectLiteralElement(b, 0);
    }
    else if (t == OVERLOAD_META) {
      r = overloadMeta(b, 0);
    }
    else if (t == PACKAGE_STATEMENT) {
      r = packageStatement(b, 0);
    }
    else if (t == PARAMETER) {
      r = parameter(b, 0);
    }
    else if (t == PARAMETER_LIST) {
      r = parameterList(b, 0);
    }
    else if (t == PARENTHESIZED_EXPRESSION) {
      r = parenthesizedExpression(b, 0);
    }
    else if (t == PREFIX_EXPRESSION) {
      r = prefixExpression(b, 0);
    }
    else if (t == PRIVATE_KEY_WORD) {
      r = privateKeyWord(b, 0);
    }
    else if (t == PROPERTY_ACCESSOR) {
      r = propertyAccessor(b, 0);
    }
    else if (t == PROPERTY_DECLARATION) {
      r = propertyDeclaration(b, 0);
    }
    else if (t == PROTECTED_META) {
      r = protectedMeta(b, 0);
    }
    else if (t == REFERENCE_EXPRESSION) {
      r = referenceExpression(b, 0);
    }
    else if (t == REGULAR_EXPRESSION_LITERAL) {
      r = regularExpressionLiteral(b, 0);
    }
    else if (t == REQUIRE_META) {
      r = requireMeta(b, 0);
    }
    else if (t == RETURN_STATEMENT) {
      r = returnStatement(b, 0);
    }
    else if (t == RETURN_STATEMENT_WITHOUT_SEMICOLON) {
      r = returnStatementWithoutSemicolon(b, 0);
    }
    else if (t == SETTER_META) {
      r = setterMeta(b, 0);
    }
    else if (t == SHIFT_EXPRESSION) {
      r = shiftExpression(b, 0);
    }
    else if (t == SHIFT_OPERATOR) {
      r = shiftOperator(b, 0);
    }
    else if (t == SHIFT_RIGHT_OPERATOR) {
      r = shiftRightOperator(b, 0);
    }
    else if (t == SHORT_TEMPLATE_ENTRY) {
      r = shortTemplateEntry(b, 0);
    }
    else if (t == SIMPLE_META) {
      r = simpleMeta(b, 0);
    }
    else if (t == STRING_LITERAL_EXPRESSION) {
      r = stringLiteralExpression(b, 0);
    }
    else if (t == SUFFIX_EXPRESSION) {
      r = suffixExpression(b, 0);
    }
    else if (t == SUPER_EXPRESSION) {
      r = superExpression(b, 0);
    }
    else if (t == SWITCH_BLOCK) {
      r = switchBlock(b, 0);
    }
    else if (t == SWITCH_CASE) {
      r = switchCase(b, 0);
    }
    else if (t == SWITCH_CASE_BLOCK) {
      r = switchCaseBlock(b, 0);
    }
    else if (t == SWITCH_CASE_EXPRESSION) {
      r = switchCaseExpression(b, 0);
    }
    else if (t == SWITCH_STATEMENT) {
      r = switchStatement(b, 0);
    }
    else if (t == TERNARY_EXPRESSION) {
      r = ternaryExpression(b, 0);
    }
    else if (t == THIS_EXPRESSION) {
      r = thisExpression(b, 0);
    }
    else if (t == THROW_STATEMENT) {
      r = throwStatement(b, 0);
    }
    else if (t == TRY_STATEMENT) {
      r = tryStatement(b, 0);
    }
    else if (t == TYPE) {
      r = type(b, 0);
    }
    else if (t == TYPE_EXTENDS) {
      r = typeExtends(b, 0);
    }
    else if (t == TYPE_LIST) {
      r = typeList(b, 0);
    }
    else if (t == TYPE_LIST_PART) {
      r = typeListPart(b, 0);
    }
    else if (t == TYPE_OR_ANONYMOUS) {
      r = typeOrAnonymous(b, 0);
    }
    else if (t == TYPE_PARAM) {
      r = typeParam(b, 0);
    }
    else if (t == TYPE_TAG) {
      r = typeTag(b, 0);
    }
    else if (t == TYPEDEF_DECLARATION) {
      r = typedefDeclaration(b, 0);
    }
    else if (t == UNREFLECTIVE_META) {
      r = unreflectiveMeta(b, 0);
    }
    else if (t == UNSIGNED_SHIFT_RIGHT_OPERATOR) {
      r = unsignedShiftRightOperator(b, 0);
    }
    else if (t == USING_STATEMENT) {
      r = usingStatement(b, 0);
    }
    else if (t == VAR_DECLARATION) {
      r = varDeclaration(b, 0);
    }
    else if (t == VAR_DECLARATION_PART) {
      r = varDeclarationPart(b, 0);
    }
    else if (t == VAR_INIT) {
      r = varInit(b, 0);
    }
    else if (t == WHILE_STATEMENT) {
      r = whileStatement(b, 0);
    }
    else if (t == WILDCARD) {
      r = wildcard(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return haxeFile(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(LITERAL_EXPRESSION, REGULAR_EXPRESSION_LITERAL),
    create_token_set_(ADDITIVE_EXPRESSION, ARRAY_ACCESS_EXPRESSION, ARRAY_LITERAL, ASSIGN_EXPRESSION,
      BITWISE_EXPRESSION, CALL_EXPRESSION, CAST_EXPRESSION, COMPARE_EXPRESSION,
      EXPRESSION, FAT_ARROW_EXPRESSION, FUNCTION_LITERAL, ITERATOR_EXPRESSION,
      LITERAL_EXPRESSION, LOGIC_AND_EXPRESSION, LOGIC_OR_EXPRESSION, MULTIPLICATIVE_EXPRESSION,
      NEW_EXPRESSION, OBJECT_LITERAL, PARENTHESIZED_EXPRESSION, PREFIX_EXPRESSION,
      REFERENCE_EXPRESSION, REGULAR_EXPRESSION_LITERAL, SHIFT_EXPRESSION, STRING_LITERAL_EXPRESSION,
      SUFFIX_EXPRESSION, SUPER_EXPRESSION, SWITCH_CASE_EXPRESSION, TERNARY_EXPRESSION,
      THIS_EXPRESSION),
  };

  /* ********************************************************** */
  // macroClassList? privateKeyWord? 'abstract' componentName genericParam? ('(' functionTypeWrapper ')')? ((identifier) type)* '{' classBody '}'
  public static boolean abstractClassDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<abstract class declaration>");
    r = abstractClassDeclaration_0(b, l + 1);
    r = r && abstractClassDeclaration_1(b, l + 1);
    r = r && consumeToken(b, KABSTRACT);
    p = r; // pin = 3
    r = r && report_error_(b, componentName(b, l + 1));
    r = p && report_error_(b, abstractClassDeclaration_4(b, l + 1)) && r;
    r = p && report_error_(b, abstractClassDeclaration_5(b, l + 1)) && r;
    r = p && report_error_(b, abstractClassDeclaration_6(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLCURLY)) && r;
    r = p && report_error_(b, classBody(b, l + 1)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, ABSTRACT_CLASS_DECLARATION, r, p, null);
    return r || p;
  }

  // macroClassList?
  private static boolean abstractClassDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // privateKeyWord?
  private static boolean abstractClassDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_1")) return false;
    privateKeyWord(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean abstractClassDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_4")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // ('(' functionTypeWrapper ')')?
  private static boolean abstractClassDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_5")) return false;
    abstractClassDeclaration_5_0(b, l + 1);
    return true;
  }

  // '(' functionTypeWrapper ')'
  private static boolean abstractClassDeclaration_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_5_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLPAREN);
    r = r && functionTypeWrapper(b, l + 1);
    r = r && consumeToken(b, PRPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // ((identifier) type)*
  private static boolean abstractClassDeclaration_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_6")) return false;
    int c = current_position_(b);
    while (true) {
      if (!abstractClassDeclaration_6_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "abstractClassDeclaration_6", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // (identifier) type
  private static boolean abstractClassDeclaration_6_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_6_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = abstractClassDeclaration_6_0_0(b, l + 1);
    r = r && type(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (identifier)
  private static boolean abstractClassDeclaration_6_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "abstractClassDeclaration_6_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = identifier(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'public' | 'private'
  public static boolean access(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "access")) return false;
    if (!nextTokenIs(b, "<access>", KPRIVATE, KPUBLIC)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<access>");
    r = consumeToken(b, KPUBLIC);
    if (!r) r = consumeToken(b, KPRIVATE);
    exit_section_(b, l, m, ACCESS, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ('+' | '-') multiplicativeExpressionWrapper
  public static boolean additiveExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additiveExpression")) return false;
    if (!nextTokenIs(b, "<additive expression>", OPLUS, OMINUS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, "<additive expression>");
    r = additiveExpression_0(b, l + 1);
    p = r; // pin = 1
    r = r && multiplicativeExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, ADDITIVE_EXPRESSION, r, p, null);
    return r || p;
  }

  // '+' | '-'
  private static boolean additiveExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additiveExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OPLUS);
    if (!r) r = consumeToken(b, OMINUS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // multiplicativeExpressionWrapper additiveExpression*
  static boolean additiveExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additiveExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = multiplicativeExpressionWrapper(b, l + 1);
    r = r && additiveExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // additiveExpression*
  private static boolean additiveExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "additiveExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!additiveExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "additiveExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // 'function' '(' parameterList ')' typeTag? 'untyped'? '{' '}'
  public static boolean anonymousFunctionDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousFunctionDeclaration")) return false;
    if (!nextTokenIs(b, KFUNCTION)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KFUNCTION);
    r = r && consumeToken(b, PLPAREN);
    p = r; // pin = 2
    r = r && report_error_(b, parameterList(b, l + 1));
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, anonymousFunctionDeclaration_4(b, l + 1)) && r;
    r = p && report_error_(b, anonymousFunctionDeclaration_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLCURLY)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, ANONYMOUS_FUNCTION_DECLARATION, r, p, null);
    return r || p;
  }

  // typeTag?
  private static boolean anonymousFunctionDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousFunctionDeclaration_4")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // 'untyped'?
  private static boolean anonymousFunctionDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousFunctionDeclaration_5")) return false;
    consumeToken(b, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // '{' anonymousTypeBody '}'
  public static boolean anonymousType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousType")) return false;
    if (!nextTokenIs(b, PLCURLY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLCURLY);
    r = r && anonymousTypeBody(b, l + 1);
    r = r && consumeToken(b, PRCURLY);
    exit_section_(b, m, ANONYMOUS_TYPE, r);
    return r;
  }

  /* ********************************************************** */
  // extendedAnonymousTypeBody | simpleAnonymousTypeBody | interfaceBody
  public static boolean anonymousTypeBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousTypeBody")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<anonymous type body>");
    r = extendedAnonymousTypeBody(b, l + 1);
    if (!r) r = simpleAnonymousTypeBody(b, l + 1);
    if (!r) r = interfaceBody(b, l + 1);
    exit_section_(b, l, m, ANONYMOUS_TYPE_BODY, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // "?"? componentName typeTag
  public static boolean anonymousTypeField(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousTypeField")) return false;
    if (!nextTokenIs(b, "<anonymous type field>", OQUEST, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<anonymous type field>");
    r = anonymousTypeField_0(b, l + 1);
    r = r && componentName(b, l + 1);
    r = r && typeTag(b, l + 1);
    exit_section_(b, l, m, ANONYMOUS_TYPE_FIELD, r, false, null);
    return r;
  }

  // "?"?
  private static boolean anonymousTypeField_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousTypeField_0")) return false;
    consumeToken(b, OQUEST);
    return true;
  }

  /* ********************************************************** */
  // anonymousTypeField (',' anonymousTypeField)*
  public static boolean anonymousTypeFieldList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousTypeFieldList")) return false;
    if (!nextTokenIs(b, "<anonymous type field list>", OQUEST, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<anonymous type field list>");
    r = anonymousTypeField(b, l + 1);
    r = r && anonymousTypeFieldList_1(b, l + 1);
    exit_section_(b, l, m, ANONYMOUS_TYPE_FIELD_LIST, r, false, null);
    return r;
  }

  // (',' anonymousTypeField)*
  private static boolean anonymousTypeFieldList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousTypeFieldList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!anonymousTypeFieldList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "anonymousTypeFieldList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' anonymousTypeField
  private static boolean anonymousTypeFieldList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousTypeFieldList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && anonymousTypeField(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '[' expression? ']'
  public static boolean arrayAccessExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrayAccessExpression")) return false;
    if (!nextTokenIs(b, PLBRACK)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, PLBRACK);
    p = r; // pin = 1
    r = r && report_error_(b, arrayAccessExpression_1(b, l + 1));
    r = p && consumeToken(b, PRBRACK) && r;
    exit_section_(b, l, m, ARRAY_ACCESS_EXPRESSION, r, p, null);
    return r || p;
  }

  // expression?
  private static boolean arrayAccessExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrayAccessExpression_1")) return false;
    expression(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '[' (expressionList ','?)? ']'
  public static boolean arrayLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrayLiteral")) return false;
    if (!nextTokenIs(b, PLBRACK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLBRACK);
    r = r && arrayLiteral_1(b, l + 1);
    r = r && consumeToken(b, PRBRACK);
    exit_section_(b, m, ARRAY_LITERAL, r);
    return r;
  }

  // (expressionList ','?)?
  private static boolean arrayLiteral_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrayLiteral_1")) return false;
    arrayLiteral_1_0(b, l + 1);
    return true;
  }

  // expressionList ','?
  private static boolean arrayLiteral_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrayLiteral_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expressionList(b, l + 1);
    r = r && arrayLiteral_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean arrayLiteral_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arrayLiteral_1_0_1")) return false;
    consumeToken(b, OCOMMA);
    return true;
  }

  /* ********************************************************** */
  // assignOperation iteratorExpressionWrapper
  public static boolean assignExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignExpression")) return false;
    if (!nextTokenIs(b, "<assign expression>", OREMAINDER_ASSIGN, OBIT_AND_ASSIGN,
      OMUL_ASSIGN, OPLUS_ASSIGN, OMINUS_ASSIGN, OQUOTIENT_ASSIGN, OSHIFT_LEFT_ASSIGN, OASSIGN,
      OGREATER, OBIT_XOR_ASSIGN, OBIT_OR_ASSIGN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, "<assign expression>");
    r = assignOperation(b, l + 1);
    p = r; // pin = 1
    r = r && iteratorExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, ASSIGN_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // iteratorExpressionWrapper assignExpression*
  static boolean assignExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = iteratorExpressionWrapper(b, l + 1);
    r = r && assignExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // assignExpression*
  private static boolean assignExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!assignExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "assignExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // '=' | '+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' | '<<=' | ('>' '>' '=') | ('>' '>' '>' '=')
  public static boolean assignOperation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignOperation")) return false;
    if (!nextTokenIs(b, "<assign operation>", OREMAINDER_ASSIGN, OBIT_AND_ASSIGN,
      OMUL_ASSIGN, OPLUS_ASSIGN, OMINUS_ASSIGN, OQUOTIENT_ASSIGN, OSHIFT_LEFT_ASSIGN, OASSIGN,
      OGREATER, OBIT_XOR_ASSIGN, OBIT_OR_ASSIGN)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<assign operation>");
    r = consumeToken(b, OASSIGN);
    if (!r) r = consumeToken(b, OPLUS_ASSIGN);
    if (!r) r = consumeToken(b, OMINUS_ASSIGN);
    if (!r) r = consumeToken(b, OMUL_ASSIGN);
    if (!r) r = consumeToken(b, OQUOTIENT_ASSIGN);
    if (!r) r = consumeToken(b, OREMAINDER_ASSIGN);
    if (!r) r = consumeToken(b, OBIT_AND_ASSIGN);
    if (!r) r = consumeToken(b, OBIT_OR_ASSIGN);
    if (!r) r = consumeToken(b, OBIT_XOR_ASSIGN);
    if (!r) r = consumeToken(b, OSHIFT_LEFT_ASSIGN);
    if (!r) r = assignOperation_10(b, l + 1);
    if (!r) r = assignOperation_11(b, l + 1);
    exit_section_(b, l, m, ASSIGN_OPERATION, r, false, null);
    return r;
  }

  // '>' '>' '='
  private static boolean assignOperation_10(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignOperation_10")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OGREATER);
    r = r && consumeToken(b, OGREATER);
    r = r && consumeToken(b, OASSIGN);
    exit_section_(b, m, null, r);
    return r;
  }

  // '>' '>' '>' '='
  private static boolean assignOperation_11(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "assignOperation_11")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OGREATER);
    r = r && consumeToken(b, OGREATER);
    r = r && consumeToken(b, OGREATER);
    r = r && consumeToken(b, OASSIGN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:autoBuild' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean autoBuildMacro(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "autoBuildMacro")) return false;
    if (!nextTokenIs(b, KAUTOBUILD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KAUTOBUILD);
    r = r && consumeToken(b, PLPAREN);
    p = r; // pin = 2
    r = r && report_error_(b, referenceExpression(b, l + 1));
    r = p && report_error_(b, autoBuildMacro_3(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, AUTO_BUILD_MACRO, r, p, null);
    return r || p;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean autoBuildMacro_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "autoBuildMacro_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!autoBuildMacro_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "autoBuildMacro_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean autoBuildMacro_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "autoBuildMacro_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = callExpression(b, l + 1);
    if (!r) r = arrayAccessExpression(b, l + 1);
    if (!r) r = qualifiedReferenceExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:bind'
  public static boolean bindMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bindMeta")) return false;
    if (!nextTokenIs(b, KBIND)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KBIND);
    exit_section_(b, m, BIND_META, r);
    return r;
  }

  /* ********************************************************** */
  // '|' | '&' | '^'
  public static boolean bitOperation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitOperation")) return false;
    if (!nextTokenIs(b, "<bit operation>", OBIT_AND, OBIT_XOR, OBIT_OR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<bit operation>");
    r = consumeToken(b, OBIT_OR);
    if (!r) r = consumeToken(b, OBIT_AND);
    if (!r) r = consumeToken(b, OBIT_XOR);
    exit_section_(b, l, m, BIT_OPERATION, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '@:bitmap' '(' stringLiteralExpression ')'
  public static boolean bitmapMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitmapMeta")) return false;
    if (!nextTokenIs(b, KBITMAP)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KBITMAP);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, stringLiteralExpression(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, BITMAP_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // bitOperation shiftExpressionWrapper
  public static boolean bitwiseExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwiseExpression")) return false;
    if (!nextTokenIs(b, "<bitwise expression>", OBIT_AND, OBIT_XOR, OBIT_OR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, "<bitwise expression>");
    r = bitOperation(b, l + 1);
    p = r; // pin = 1
    r = r && shiftExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, BITWISE_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // shiftExpressionWrapper bitwiseExpression*
  static boolean bitwiseExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwiseExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = shiftExpressionWrapper(b, l + 1);
    r = r && bitwiseExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // bitwiseExpression*
  private static boolean bitwiseExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "bitwiseExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!bitwiseExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "bitwiseExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // '{' statementList? '}'
  public static boolean blockStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "blockStatement")) return false;
    if (!nextTokenIs(b, PLCURLY)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, PLCURLY);
    p = r; // pin = 1
    r = r && report_error_(b, blockStatement_1(b, l + 1));
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, BLOCK_STATEMENT, r, p, null);
    return r || p;
  }

  // statementList?
  private static boolean blockStatement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "blockStatement_1")) return false;
    statementList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // 'break' ';'
  public static boolean breakStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "breakStatement")) return false;
    if (!nextTokenIs(b, KBREAK)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KBREAK);
    p = r; // pin = 1
    r = r && consumeToken(b, OSEMI);
    exit_section_(b, l, m, BREAK_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '@:build' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean buildMacro(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "buildMacro")) return false;
    if (!nextTokenIs(b, KBUILD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KBUILD);
    r = r && consumeToken(b, PLPAREN);
    p = r; // pin = 2
    r = r && report_error_(b, referenceExpression(b, l + 1));
    r = p && report_error_(b, buildMacro_3(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, BUILD_MACRO, r, p, null);
    return r || p;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean buildMacro_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "buildMacro_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!buildMacro_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "buildMacro_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean buildMacro_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "buildMacro_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = callExpression(b, l + 1);
    if (!r) r = arrayAccessExpression(b, l + 1);
    if (!r) r = qualifiedReferenceExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '(' expressionList? ')'
  public static boolean callExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callExpression")) return false;
    if (!nextTokenIs(b, PLPAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, PLPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, callExpression_1(b, l + 1));
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, CALL_EXPRESSION, r, p, null);
    return r || p;
  }

  // expressionList?
  private static boolean callExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callExpression_1")) return false;
    expressionList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (referenceExpression | thisExpression | superExpression) (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  static boolean callOrArrayAccess(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callOrArrayAccess")) return false;
    if (!nextTokenIs(b, "", KSUPER, KTHIS, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = callOrArrayAccess_0(b, l + 1);
    r = r && callOrArrayAccess_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // referenceExpression | thisExpression | superExpression
  private static boolean callOrArrayAccess_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callOrArrayAccess_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = referenceExpression(b, l + 1);
    if (!r) r = thisExpression(b, l + 1);
    if (!r) r = superExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean callOrArrayAccess_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callOrArrayAccess_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!callOrArrayAccess_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "callOrArrayAccess_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean callOrArrayAccess_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "callOrArrayAccess_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = callExpression(b, l + 1);
    if (!r) r = arrayAccessExpression(b, l + 1);
    if (!r) r = qualifiedReferenceExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'cast' (('(' expression ',' functionTypeWrapper ')')  | expression)
  public static boolean castExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "castExpression")) return false;
    if (!nextTokenIs(b, KCAST)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KCAST);
    p = r; // pin = 1
    r = r && castExpression_1(b, l + 1);
    exit_section_(b, l, m, CAST_EXPRESSION, r, p, null);
    return r || p;
  }

  // ('(' expression ',' functionTypeWrapper ')')  | expression
  private static boolean castExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "castExpression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = castExpression_1_0(b, l + 1);
    if (!r) r = expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '(' expression ',' functionTypeWrapper ')'
  private static boolean castExpression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "castExpression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLPAREN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, OCOMMA);
    r = r && functionTypeWrapper(b, l + 1);
    r = r && consumeToken(b, PRPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'catch' '(' parameter ')' statement ';'?
  public static boolean catchStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "catchStatement")) return false;
    if (!nextTokenIs(b, KCATCH)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KCATCH);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, parameter(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, statement(b, l + 1)) && r;
    r = p && catchStatement_5(b, l + 1) && r;
    exit_section_(b, l, m, CATCH_STATEMENT, r, p, null);
    return r || p;
  }

  // ';'?
  private static boolean catchStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "catchStatement_5")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // classBodyPart*
  public static boolean classBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classBody")) return false;
    Marker m = enter_section_(b, l, _NONE_, "<class body>");
    int c = current_position_(b);
    while (true) {
      if (!classBodyPart(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "classBody", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, CLASS_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionDeclarationWithAttributes
  static boolean classBodyPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classBodyPart")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = varDeclaration(b, l + 1);
    if (!r) r = functionDeclarationWithAttributes(b, l + 1);
    exit_section_(b, l, m, null, r, false, class_body_part_recover_parser_);
    return r;
  }

  /* ********************************************************** */
  // macroClassList? privateKeyWord? 'class' componentName genericParam? inheritList? '{' classBody '}'
  public static boolean classDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<class declaration>");
    r = classDeclaration_0(b, l + 1);
    r = r && classDeclaration_1(b, l + 1);
    r = r && consumeToken(b, KCLASS);
    p = r; // pin = 3
    r = r && report_error_(b, componentName(b, l + 1));
    r = p && report_error_(b, classDeclaration_4(b, l + 1)) && r;
    r = p && report_error_(b, classDeclaration_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLCURLY)) && r;
    r = p && report_error_(b, classBody(b, l + 1)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, CLASS_DECLARATION, r, p, null);
    return r || p;
  }

  // macroClassList?
  private static boolean classDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDeclaration_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // privateKeyWord?
  private static boolean classDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDeclaration_1")) return false;
    privateKeyWord(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean classDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDeclaration_4")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // inheritList?
  private static boolean classDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classDeclaration_5")) return false;
    inheritList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // !(ppToken | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | 'macro')
  static boolean class_body_part_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_body_part_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !class_body_part_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ppToken | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | 'macro'
  private static boolean class_body_part_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_body_part_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ppToken(b, l + 1);
    if (!r) r = metaKeyWord(b, l + 1);
    if (!r) r = consumeToken(b, KDYNAMIC);
    if (!r) r = consumeToken(b, KFUNCTION);
    if (!r) r = consumeToken(b, KINLINE);
    if (!r) r = consumeToken(b, KOVERRIDE);
    if (!r) r = consumeToken(b, KPRIVATE);
    if (!r) r = consumeToken(b, KPUBLIC);
    if (!r) r = consumeToken(b, KSTATIC);
    if (!r) r = consumeToken(b, KVAR);
    if (!r) r = consumeToken(b, PRCURLY);
    if (!r) r = consumeToken(b, KMACRO2);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // compareOperation bitwiseExpressionWrapper
  public static boolean compareExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compareExpression")) return false;
    if (!nextTokenIs(b, "<compare expression>", ONOT_EQ, OLESS,
      OLESS_OR_EQUAL, OEQ, OGREATER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, "<compare expression>");
    r = compareOperation(b, l + 1);
    p = r; // pin = 1
    r = r && bitwiseExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, COMPARE_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // bitwiseExpressionWrapper compareExpression*
  static boolean compareExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compareExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = bitwiseExpressionWrapper(b, l + 1);
    r = r && compareExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // compareExpression*
  private static boolean compareExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compareExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!compareExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "compareExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // '==' | '!=' | '<=' | '<' | ('>' '=') | '>'
  public static boolean compareOperation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compareOperation")) return false;
    if (!nextTokenIs(b, "<compare operation>", ONOT_EQ, OLESS,
      OLESS_OR_EQUAL, OEQ, OGREATER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<compare operation>");
    r = consumeToken(b, OEQ);
    if (!r) r = consumeToken(b, ONOT_EQ);
    if (!r) r = consumeToken(b, OLESS_OR_EQUAL);
    if (!r) r = consumeToken(b, OLESS);
    if (!r) r = compareOperation_4(b, l + 1);
    if (!r) r = consumeToken(b, OGREATER);
    exit_section_(b, l, m, COMPARE_OPERATION, r, false, null);
    return r;
  }

  // '>' '='
  private static boolean compareOperation_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "compareOperation_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OGREATER);
    r = r && consumeToken(b, OASSIGN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // identifier
  public static boolean componentName(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "componentName")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = identifier(b, l + 1);
    exit_section_(b, m, COMPONENT_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // 'new'
  static boolean constructorName(PsiBuilder b, int l) {
    return consumeToken(b, ONEW);
  }

  /* ********************************************************** */
  // 'continue' ';'
  public static boolean continueStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "continueStatement")) return false;
    if (!nextTokenIs(b, KCONTINUE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KCONTINUE);
    p = r; // pin = 1
    r = r && consumeToken(b, OSEMI);
    exit_section_(b, l, m, CONTINUE_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '@:coreApi'
  public static boolean coreApiMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "coreApiMeta")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<core api meta>");
    r = consumeToken(b, "@:coreApi");
    exit_section_(b, l, m, CORE_API_META, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // MACRO_ID ('(' expressionList ')')?
  public static boolean customMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "customMeta")) return false;
    if (!nextTokenIs(b, MACRO_ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, MACRO_ID);
    p = r; // pin = 1
    r = r && customMeta_1(b, l + 1);
    exit_section_(b, l, m, CUSTOM_META, r, p, null);
    return r || p;
  }

  // ('(' expressionList ')')?
  private static boolean customMeta_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "customMeta_1")) return false;
    customMeta_1_0(b, l + 1);
    return true;
  }

  // '(' expressionList ')'
  private static boolean customMeta_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "customMeta_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLPAREN);
    r = r && expressionList(b, l + 1);
    r = r && consumeToken(b, PRPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:debug'
  public static boolean debugMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "debugMeta")) return false;
    if (!nextTokenIs(b, KDEBUG)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KDEBUG);
    exit_section_(b, m, DEBUG_META, r);
    return r;
  }

  /* ********************************************************** */
  // 'static' | 'inline' | 'dynamic' | 'override' | 'macro' | access
  public static boolean declarationAttribute(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "declarationAttribute")) return false;
    if (!nextTokenIs(b, "<declaration attribute>", KDYNAMIC, KINLINE,
      KMACRO2, KOVERRIDE, KPRIVATE, KPUBLIC, KSTATIC)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<declaration attribute>");
    r = consumeToken(b, KSTATIC);
    if (!r) r = consumeToken(b, KINLINE);
    if (!r) r = consumeToken(b, KDYNAMIC);
    if (!r) r = consumeToken(b, KOVERRIDE);
    if (!r) r = consumeToken(b, KMACRO2);
    if (!r) r = access(b, l + 1);
    exit_section_(b, l, m, DECLARATION_ATTRIBUTE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // 'default' ':' switchCaseBlock?
  public static boolean defaultCase(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "defaultCase")) return false;
    if (!nextTokenIs(b, KDEFAULT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KDEFAULT);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, OCOLON));
    r = p && defaultCase_2(b, l + 1) && r;
    exit_section_(b, l, m, DEFAULT_CASE, r, p, null);
    return r || p;
  }

  // switchCaseBlock?
  private static boolean defaultCase_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "defaultCase_2")) return false;
    switchCaseBlock(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // 'do' statement 'while' '(' expression ')' ';'
  public static boolean doWhileStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doWhileStatement")) return false;
    if (!nextTokenIs(b, KDO)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KDO);
    p = r; // pin = 1
    r = r && report_error_(b, statement(b, l + 1));
    r = p && report_error_(b, consumeToken(b, KWHILE)) && r;
    r = p && report_error_(b, consumeToken(b, PLPAREN)) && r;
    r = p && report_error_(b, expression(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && consumeToken(b, OSEMI) && r;
    exit_section_(b, l, m, DO_WHILE_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '{' '}'
  static boolean emptyObjectLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "emptyObjectLiteral")) return false;
    if (!nextTokenIs(b, PLCURLY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLCURLY);
    r = r && consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // enumValueDeclaration*
  public static boolean enumBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumBody")) return false;
    Marker m = enter_section_(b, l, _NONE_, "<enum body>");
    int c = current_position_(b);
    while (true) {
      if (!enumValueDeclaration(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "enumBody", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, ENUM_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // '(' parameterList ')'
  public static boolean enumConstructorParameters(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumConstructorParameters")) return false;
    if (!nextTokenIs(b, PLPAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, PLPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, parameterList(b, l + 1));
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, ENUM_CONSTRUCTOR_PARAMETERS, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // macroClassList? externOrPrivate? 'enum' componentName genericParam? '{' enumBody '}'
  public static boolean enumDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<enum declaration>");
    r = enumDeclaration_0(b, l + 1);
    r = r && enumDeclaration_1(b, l + 1);
    r = r && consumeToken(b, KENUM);
    p = r; // pin = 3
    r = r && report_error_(b, componentName(b, l + 1));
    r = p && report_error_(b, enumDeclaration_4(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLCURLY)) && r;
    r = p && report_error_(b, enumBody(b, l + 1)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, ENUM_DECLARATION, r, p, null);
    return r || p;
  }

  // macroClassList?
  private static boolean enumDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumDeclaration_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // externOrPrivate?
  private static boolean enumDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumDeclaration_1")) return false;
    externOrPrivate(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean enumDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumDeclaration_4")) return false;
    genericParam(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // componentName enumConstructorParameters? ';'
  public static boolean enumValueDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumValueDeclaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<enum value declaration>");
    r = componentName(b, l + 1);
    r = r && enumValueDeclaration_1(b, l + 1);
    r = r && consumeToken(b, OSEMI);
    exit_section_(b, l, m, ENUM_VALUE_DECLARATION, r, false, enum_value_declaration_recovery_parser_);
    return r;
  }

  // enumConstructorParameters?
  private static boolean enumValueDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enumValueDeclaration_1")) return false;
    enumConstructorParameters(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // !(ID | '}')
  static boolean enum_value_declaration_recovery(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_value_declaration_recovery")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !enum_value_declaration_recovery_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ID | '}'
  private static boolean enum_value_declaration_recovery_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "enum_value_declaration_recovery_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ID);
    if (!r) r = consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // fatArrowExpressionWrapper
  public static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, "<expression>");
    r = fatArrowExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, EXPRESSION, r, false, expression_recover_parser_);
    return r;
  }

  /* ********************************************************** */
  // forStatement | whileStatement | (expression (',' expression)*)
  public static boolean expressionList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<expression list>");
    r = forStatement(b, l + 1);
    if (!r) r = whileStatement(b, l + 1);
    if (!r) r = expressionList_2(b, l + 1);
    exit_section_(b, l, m, EXPRESSION_LIST, r, false, expression_list_recover_parser_);
    return r;
  }

  // expression (',' expression)*
  private static boolean expressionList_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionList_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1);
    r = r && expressionList_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' expression)*
  private static boolean expressionList_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionList_2_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!expressionList_2_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expressionList_2_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' expression
  private static boolean expressionList_2_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionList_2_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(')' | ']')
  static boolean expression_list_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !expression_list_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ')' | ']'
  private static boolean expression_list_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_list_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PRPAREN);
    if (!r) r = consumeToken(b, PRBRACK);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !('!' | '!=' | '%' | '%=' | '&&' | '&' | '&=' | '(' | ')' | '*' | '*=' | '+' | '++' | '+=' | ',' | '-' | '--' | '-=' | '.' | '...' | '/' | '/=' | ':' | ';' | '<' | '<<' | '<<=' | '<=' | '=' | '==' | '>' | '?' | metaKeyWord | '[' | ']' | '^' | '^=' | 'break' | 'case' | 'cast' | 'catch' | 'continue' | 'default' | 'do' | 'dynamic' | 'else' | 'false' | 'for' | 'function' | 'if' | 'inline' | 'new' | 'null' | 'override' | 'private' | 'public' | 'return' | 'static' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '|' | '|=' | '||' | '}' | '~' | ID | LITFLOAT | LITHEX | LITINT | LITOCT | OPEN_QUOTE | CLOSING_QUOTE | MACRO_ID | REG_EXP | LONG_TEMPLATE_ENTRY_END | '=>')
  static boolean expression_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !expression_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // '!' | '!=' | '%' | '%=' | '&&' | '&' | '&=' | '(' | ')' | '*' | '*=' | '+' | '++' | '+=' | ',' | '-' | '--' | '-=' | '.' | '...' | '/' | '/=' | ':' | ';' | '<' | '<<' | '<<=' | '<=' | '=' | '==' | '>' | '?' | metaKeyWord | '[' | ']' | '^' | '^=' | 'break' | 'case' | 'cast' | 'catch' | 'continue' | 'default' | 'do' | 'dynamic' | 'else' | 'false' | 'for' | 'function' | 'if' | 'inline' | 'new' | 'null' | 'override' | 'private' | 'public' | 'return' | 'static' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '|' | '|=' | '||' | '}' | '~' | ID | LITFLOAT | LITHEX | LITINT | LITOCT | OPEN_QUOTE | CLOSING_QUOTE | MACRO_ID | REG_EXP | LONG_TEMPLATE_ENTRY_END | '=>'
  private static boolean expression_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ONOT);
    if (!r) r = consumeToken(b, ONOT_EQ);
    if (!r) r = consumeToken(b, OREMAINDER);
    if (!r) r = consumeToken(b, OREMAINDER_ASSIGN);
    if (!r) r = consumeToken(b, OCOND_AND);
    if (!r) r = consumeToken(b, OBIT_AND);
    if (!r) r = consumeToken(b, OBIT_AND_ASSIGN);
    if (!r) r = consumeToken(b, PLPAREN);
    if (!r) r = consumeToken(b, PRPAREN);
    if (!r) r = consumeToken(b, OMUL);
    if (!r) r = consumeToken(b, OMUL_ASSIGN);
    if (!r) r = consumeToken(b, OPLUS);
    if (!r) r = consumeToken(b, OPLUS_PLUS);
    if (!r) r = consumeToken(b, OPLUS_ASSIGN);
    if (!r) r = consumeToken(b, OCOMMA);
    if (!r) r = consumeToken(b, OMINUS);
    if (!r) r = consumeToken(b, OMINUS_MINUS);
    if (!r) r = consumeToken(b, OMINUS_ASSIGN);
    if (!r) r = consumeToken(b, ODOT);
    if (!r) r = consumeToken(b, OTRIPLE_DOT);
    if (!r) r = consumeToken(b, OQUOTIENT);
    if (!r) r = consumeToken(b, OQUOTIENT_ASSIGN);
    if (!r) r = consumeToken(b, OCOLON);
    if (!r) r = consumeToken(b, OSEMI);
    if (!r) r = consumeToken(b, OLESS);
    if (!r) r = consumeToken(b, OSHIFT_LEFT);
    if (!r) r = consumeToken(b, OSHIFT_LEFT_ASSIGN);
    if (!r) r = consumeToken(b, OLESS_OR_EQUAL);
    if (!r) r = consumeToken(b, OASSIGN);
    if (!r) r = consumeToken(b, OEQ);
    if (!r) r = consumeToken(b, OGREATER);
    if (!r) r = consumeToken(b, OQUEST);
    if (!r) r = metaKeyWord(b, l + 1);
    if (!r) r = consumeToken(b, PLBRACK);
    if (!r) r = consumeToken(b, PRBRACK);
    if (!r) r = consumeToken(b, OBIT_XOR);
    if (!r) r = consumeToken(b, OBIT_XOR_ASSIGN);
    if (!r) r = consumeToken(b, KBREAK);
    if (!r) r = consumeToken(b, KCASE);
    if (!r) r = consumeToken(b, KCAST);
    if (!r) r = consumeToken(b, KCATCH);
    if (!r) r = consumeToken(b, KCONTINUE);
    if (!r) r = consumeToken(b, KDEFAULT);
    if (!r) r = consumeToken(b, KDO);
    if (!r) r = consumeToken(b, KDYNAMIC);
    if (!r) r = consumeToken(b, KELSE);
    if (!r) r = consumeToken(b, KFALSE);
    if (!r) r = consumeToken(b, KFOR);
    if (!r) r = consumeToken(b, KFUNCTION);
    if (!r) r = consumeToken(b, KIF);
    if (!r) r = consumeToken(b, KINLINE);
    if (!r) r = consumeToken(b, ONEW);
    if (!r) r = consumeToken(b, KNULL);
    if (!r) r = consumeToken(b, KOVERRIDE);
    if (!r) r = consumeToken(b, KPRIVATE);
    if (!r) r = consumeToken(b, KPUBLIC);
    if (!r) r = consumeToken(b, KRETURN);
    if (!r) r = consumeToken(b, KSTATIC);
    if (!r) r = consumeToken(b, KSUPER);
    if (!r) r = consumeToken(b, KSWITCH);
    if (!r) r = consumeToken(b, KTHIS);
    if (!r) r = consumeToken(b, KTHROW);
    if (!r) r = consumeToken(b, KTRUE);
    if (!r) r = consumeToken(b, KTRY);
    if (!r) r = consumeToken(b, KUNTYPED);
    if (!r) r = consumeToken(b, KVAR);
    if (!r) r = consumeToken(b, KWHILE);
    if (!r) r = consumeToken(b, PLCURLY);
    if (!r) r = consumeToken(b, OBIT_OR);
    if (!r) r = consumeToken(b, OBIT_OR_ASSIGN);
    if (!r) r = consumeToken(b, OCOND_OR);
    if (!r) r = consumeToken(b, PRCURLY);
    if (!r) r = consumeToken(b, OCOMPLEMENT);
    if (!r) r = consumeToken(b, ID);
    if (!r) r = consumeToken(b, LITFLOAT);
    if (!r) r = consumeToken(b, LITHEX);
    if (!r) r = consumeToken(b, LITINT);
    if (!r) r = consumeToken(b, LITOCT);
    if (!r) r = consumeToken(b, OPEN_QUOTE);
    if (!r) r = consumeToken(b, CLOSING_QUOTE);
    if (!r) r = consumeToken(b, MACRO_ID);
    if (!r) r = consumeToken(b, REG_EXP);
    if (!r) r = consumeToken(b, LONG_TEMPLATE_ENTRY_END);
    if (!r) r = consumeToken(b, OFAT_ARROW);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // typeExtends (',' anonymousTypeFieldList)? (',' interfaceBody)?
  static boolean extendedAnonymousTypeBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extendedAnonymousTypeBody")) return false;
    if (!nextTokenIs(b, OGREATER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = typeExtends(b, l + 1);
    r = r && extendedAnonymousTypeBody_1(b, l + 1);
    r = r && extendedAnonymousTypeBody_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' anonymousTypeFieldList)?
  private static boolean extendedAnonymousTypeBody_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extendedAnonymousTypeBody_1")) return false;
    extendedAnonymousTypeBody_1_0(b, l + 1);
    return true;
  }

  // ',' anonymousTypeFieldList
  private static boolean extendedAnonymousTypeBody_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extendedAnonymousTypeBody_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && anonymousTypeFieldList(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' interfaceBody)?
  private static boolean extendedAnonymousTypeBody_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extendedAnonymousTypeBody_2")) return false;
    extendedAnonymousTypeBody_2_0(b, l + 1);
    return true;
  }

  // ',' interfaceBody
  private static boolean extendedAnonymousTypeBody_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extendedAnonymousTypeBody_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && interfaceBody(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ('extends' type)+
  public static boolean extendsDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extendsDeclaration")) return false;
    if (!nextTokenIs(b, KEXTENDS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = extendsDeclaration_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!extendsDeclaration_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "extendsDeclaration", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, EXTENDS_DECLARATION, r);
    return r;
  }

  // 'extends' type
  private static boolean extendsDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extendsDeclaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KEXTENDS);
    r = r && type(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // externAndMaybePrivate2 | externAndMaybePrivate1
  static boolean externAndMaybePrivate(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externAndMaybePrivate")) return false;
    if (!nextTokenIs(b, "", KEXTERN, KPRIVATE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = externAndMaybePrivate2(b, l + 1);
    if (!r) r = externAndMaybePrivate1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // privateKeyWord? externKeyWord
  static boolean externAndMaybePrivate1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externAndMaybePrivate1")) return false;
    if (!nextTokenIs(b, "", KEXTERN, KPRIVATE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = externAndMaybePrivate1_0(b, l + 1);
    r = r && externKeyWord(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // privateKeyWord?
  private static boolean externAndMaybePrivate1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externAndMaybePrivate1_0")) return false;
    privateKeyWord(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // externKeyWord privateKeyWord?
  static boolean externAndMaybePrivate2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externAndMaybePrivate2")) return false;
    if (!nextTokenIs(b, KEXTERN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = externKeyWord(b, l + 1);
    r = r && externAndMaybePrivate2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // privateKeyWord?
  private static boolean externAndMaybePrivate2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externAndMaybePrivate2_1")) return false;
    privateKeyWord(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // macroClassList? externAndMaybePrivate 'class' componentName genericParam? inheritList? '{' externClassDeclarationBody '}'
  public static boolean externClassDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externClassDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<extern class declaration>");
    r = externClassDeclaration_0(b, l + 1);
    r = r && externAndMaybePrivate(b, l + 1);
    r = r && consumeToken(b, KCLASS);
    p = r; // pin = 3
    r = r && report_error_(b, componentName(b, l + 1));
    r = p && report_error_(b, externClassDeclaration_4(b, l + 1)) && r;
    r = p && report_error_(b, externClassDeclaration_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLCURLY)) && r;
    r = p && report_error_(b, externClassDeclarationBody(b, l + 1)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, EXTERN_CLASS_DECLARATION, r, p, null);
    return r || p;
  }

  // macroClassList?
  private static boolean externClassDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externClassDeclaration_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean externClassDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externClassDeclaration_4")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // inheritList?
  private static boolean externClassDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externClassDeclaration_5")) return false;
    inheritList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // externClassDeclarationBodyPart*
  public static boolean externClassDeclarationBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externClassDeclarationBody")) return false;
    Marker m = enter_section_(b, l, _NONE_, "<extern class declaration body>");
    int c = current_position_(b);
    while (true) {
      if (!externClassDeclarationBodyPart(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "externClassDeclarationBody", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, EXTERN_CLASS_DECLARATION_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | externFunctionDeclaration
  static boolean externClassDeclarationBodyPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externClassDeclarationBodyPart")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = varDeclaration(b, l + 1);
    if (!r) r = externFunctionDeclaration(b, l + 1);
    exit_section_(b, l, m, null, r, false, extern_class_body_part_recover_parser_);
    return r;
  }

  /* ********************************************************** */
  // (functionMacroMember| declarationAttribute)* 'function' (constructorName | componentName) genericParam? '(' parameterList ')' typeTag? 'untyped'? (functionCommonBody | ';')
  public static boolean externFunctionDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration")) return false;
    if (!nextTokenIs(b, "<extern function declaration>", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNATIVE, KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE,
      KSETTER, KDYNAMIC, KFUNCTION, KINLINE, KMACRO2, KOVERRIDE,
      KPRIVATE, KPUBLIC, KSTATIC, MACRO_ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<extern function declaration>");
    r = externFunctionDeclaration_0(b, l + 1);
    r = r && consumeToken(b, KFUNCTION);
    r = r && externFunctionDeclaration_2(b, l + 1);
    p = r; // pin = 3
    r = r && report_error_(b, externFunctionDeclaration_3(b, l + 1));
    r = p && report_error_(b, consumeToken(b, PLPAREN)) && r;
    r = p && report_error_(b, parameterList(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, externFunctionDeclaration_7(b, l + 1)) && r;
    r = p && report_error_(b, externFunctionDeclaration_8(b, l + 1)) && r;
    r = p && externFunctionDeclaration_9(b, l + 1) && r;
    exit_section_(b, l, m, EXTERN_FUNCTION_DECLARATION, r, p, null);
    return r || p;
  }

  // (functionMacroMember| declarationAttribute)*
  private static boolean externFunctionDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!externFunctionDeclaration_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "externFunctionDeclaration_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // functionMacroMember| declarationAttribute
  private static boolean externFunctionDeclaration_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = functionMacroMember(b, l + 1);
    if (!r) r = declarationAttribute(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // constructorName | componentName
  private static boolean externFunctionDeclaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = constructorName(b, l + 1);
    if (!r) r = componentName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // genericParam?
  private static boolean externFunctionDeclaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration_3")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // typeTag?
  private static boolean externFunctionDeclaration_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration_7")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // 'untyped'?
  private static boolean externFunctionDeclaration_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration_8")) return false;
    consumeToken(b, KUNTYPED);
    return true;
  }

  // functionCommonBody | ';'
  private static boolean externFunctionDeclaration_9(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externFunctionDeclaration_9")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = functionCommonBody(b, l + 1);
    if (!r) r = consumeToken(b, OSEMI);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // macroClassList? externAndMaybePrivate? 'interface' componentName genericParam? inheritList? '{' interfaceBody '}'
  public static boolean externInterfaceDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externInterfaceDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<extern interface declaration>");
    r = externInterfaceDeclaration_0(b, l + 1);
    r = r && externInterfaceDeclaration_1(b, l + 1);
    r = r && consumeToken(b, KINTERFACE);
    p = r; // pin = 3
    r = r && report_error_(b, componentName(b, l + 1));
    r = p && report_error_(b, externInterfaceDeclaration_4(b, l + 1)) && r;
    r = p && report_error_(b, externInterfaceDeclaration_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLCURLY)) && r;
    r = p && report_error_(b, interfaceBody(b, l + 1)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, EXTERN_INTERFACE_DECLARATION, r, p, null);
    return r || p;
  }

  // macroClassList?
  private static boolean externInterfaceDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externInterfaceDeclaration_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // externAndMaybePrivate?
  private static boolean externInterfaceDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externInterfaceDeclaration_1")) return false;
    externAndMaybePrivate(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean externInterfaceDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externInterfaceDeclaration_4")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // inheritList?
  private static boolean externInterfaceDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externInterfaceDeclaration_5")) return false;
    inheritList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // 'extern'
  public static boolean externKeyWord(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externKeyWord")) return false;
    if (!nextTokenIs(b, KEXTERN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KEXTERN);
    exit_section_(b, m, EXTERN_KEY_WORD, r);
    return r;
  }

  /* ********************************************************** */
  // externPrivate | privateExtern | privateKeyWord | externKeyWord
  static boolean externOrPrivate(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externOrPrivate")) return false;
    if (!nextTokenIs(b, "", KEXTERN, KPRIVATE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = externPrivate(b, l + 1);
    if (!r) r = privateExtern(b, l + 1);
    if (!r) r = privateKeyWord(b, l + 1);
    if (!r) r = externKeyWord(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // externKeyWord privateKeyWord
  static boolean externPrivate(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "externPrivate")) return false;
    if (!nextTokenIs(b, KEXTERN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = externKeyWord(b, l + 1);
    r = r && privateKeyWord(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(pptoken | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}')
  static boolean extern_class_body_part_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extern_class_body_part_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !extern_class_body_part_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // pptoken | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}'
  private static boolean extern_class_body_part_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "extern_class_body_part_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PPTOKEN);
    if (!r) r = metaKeyWord(b, l + 1);
    if (!r) r = consumeToken(b, KDYNAMIC);
    if (!r) r = consumeToken(b, KFUNCTION);
    if (!r) r = consumeToken(b, KINLINE);
    if (!r) r = consumeToken(b, KOVERRIDE);
    if (!r) r = consumeToken(b, KPRIVATE);
    if (!r) r = consumeToken(b, KPUBLIC);
    if (!r) r = consumeToken(b, KSTATIC);
    if (!r) r = consumeToken(b, KVAR);
    if (!r) r = consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:fakeEnum' '(' type ')'
  public static boolean fakeEnumMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fakeEnumMeta")) return false;
    if (!nextTokenIs(b, KFAKEENUM)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KFAKEENUM);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, type(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, FAKE_ENUM_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '=>' assignExpressionWrapper
  public static boolean fatArrowExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fatArrowExpression")) return false;
    if (!nextTokenIs(b, OFAT_ARROW)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, OFAT_ARROW);
    p = r; // pin = 1
    r = r && assignExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, FAT_ARROW_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // assignExpressionWrapper fatArrowExpression*
  static boolean fatArrowExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fatArrowExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = assignExpressionWrapper(b, l + 1);
    r = r && fatArrowExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // fatArrowExpression*
  private static boolean fatArrowExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fatArrowExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!fatArrowExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "fatArrowExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // '@:final'
  public static boolean finalMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "finalMeta")) return false;
    if (!nextTokenIs(b, KFINAL)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KFINAL);
    exit_section_(b, m, FINAL_META, r);
    return r;
  }

  /* ********************************************************** */
  // 'for' '(' componentName 'in' iterable')' statement ';'?
  public static boolean forStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forStatement")) return false;
    if (!nextTokenIs(b, KFOR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KFOR);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, componentName(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, OIN)) && r;
    r = p && report_error_(b, iterable(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, statement(b, l + 1)) && r;
    r = p && forStatement_7(b, l + 1) && r;
    exit_section_(b, l, m, FOR_STATEMENT, r, p, null);
    return r || p;
  }

  // ';'?
  private static boolean forStatement_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "forStatement_7")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // blockStatement | returnStatement | (expression ';'?) | throwStatement | ifStatement | forStatement | whileStatement | doWhileStatement
  static boolean functionCommonBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCommonBody")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = blockStatement(b, l + 1);
    if (!r) r = returnStatement(b, l + 1);
    if (!r) r = functionCommonBody_2(b, l + 1);
    if (!r) r = throwStatement(b, l + 1);
    if (!r) r = ifStatement(b, l + 1);
    if (!r) r = forStatement(b, l + 1);
    if (!r) r = whileStatement(b, l + 1);
    if (!r) r = doWhileStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expression ';'?
  private static boolean functionCommonBody_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCommonBody_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1);
    r = r && functionCommonBody_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean functionCommonBody_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionCommonBody_2_1")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // (functionMacroMember | declarationAttribute)* 'function' (constructorName | componentName) genericParam? '(' parameterList ')' typeTag? 'untyped'? functionCommonBody
  public static boolean functionDeclarationWithAttributes(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDeclarationWithAttributes")) return false;
    if (!nextTokenIs(b, "<function declaration with attributes>", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNATIVE, KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE,
      KSETTER, KDYNAMIC, KFUNCTION, KINLINE, KMACRO2, KOVERRIDE,
      KPRIVATE, KPUBLIC, KSTATIC, MACRO_ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<function declaration with attributes>");
    r = functionDeclarationWithAttributes_0(b, l + 1);
    r = r && consumeToken(b, KFUNCTION);
    r = r && functionDeclarationWithAttributes_2(b, l + 1);
    p = r; // pin = 3
    r = r && report_error_(b, functionDeclarationWithAttributes_3(b, l + 1));
    r = p && report_error_(b, consumeToken(b, PLPAREN)) && r;
    r = p && report_error_(b, parameterList(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, functionDeclarationWithAttributes_7(b, l + 1)) && r;
    r = p && report_error_(b, functionDeclarationWithAttributes_8(b, l + 1)) && r;
    r = p && functionCommonBody(b, l + 1) && r;
    exit_section_(b, l, m, FUNCTION_DECLARATION_WITH_ATTRIBUTES, r, p, null);
    return r || p;
  }

  // (functionMacroMember | declarationAttribute)*
  private static boolean functionDeclarationWithAttributes_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDeclarationWithAttributes_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!functionDeclarationWithAttributes_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "functionDeclarationWithAttributes_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // functionMacroMember | declarationAttribute
  private static boolean functionDeclarationWithAttributes_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDeclarationWithAttributes_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = functionMacroMember(b, l + 1);
    if (!r) r = declarationAttribute(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // constructorName | componentName
  private static boolean functionDeclarationWithAttributes_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDeclarationWithAttributes_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = constructorName(b, l + 1);
    if (!r) r = componentName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // genericParam?
  private static boolean functionDeclarationWithAttributes_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDeclarationWithAttributes_3")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // typeTag?
  private static boolean functionDeclarationWithAttributes_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDeclarationWithAttributes_7")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionDeclarationWithAttributes_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionDeclarationWithAttributes_8")) return false;
    consumeToken(b, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // 'function' '(' parameterList ')' typeTag? 'untyped'? functionCommonBody
  public static boolean functionLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionLiteral")) return false;
    if (!nextTokenIs(b, KFUNCTION)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KFUNCTION);
    r = r && consumeToken(b, PLPAREN);
    p = r; // pin = 2
    r = r && report_error_(b, parameterList(b, l + 1));
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, functionLiteral_4(b, l + 1)) && r;
    r = p && report_error_(b, functionLiteral_5(b, l + 1)) && r;
    r = p && functionCommonBody(b, l + 1) && r;
    exit_section_(b, l, m, FUNCTION_LITERAL, r, p, null);
    return r || p;
  }

  // typeTag?
  private static boolean functionLiteral_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionLiteral_4")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionLiteral_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionLiteral_5")) return false;
    consumeToken(b, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // finalMeta | macroMember | overloadMeta
  static boolean functionMacroMember(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionMacroMember")) return false;
    if (!nextTokenIs(b, "", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNATIVE, KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE, KSETTER, MACRO_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = finalMeta(b, l + 1);
    if (!r) r = macroMember(b, l + 1);
    if (!r) r = overloadMeta(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (functionMacroMember| declarationAttribute)* 'function' (constructorName | componentName) genericParam? '(' parameterList ')' typeTag? 'untyped'? ';'
  public static boolean functionPrototypeDeclarationWithAttributes(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionPrototypeDeclarationWithAttributes")) return false;
    if (!nextTokenIs(b, "<function prototype declaration with attributes>", KAUTOBUILD, KBUILD,
      KDEBUG, KFINAL, KGETTER, KKEEP, KMACRO, KMETA,
      KNATIVE, KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE,
      KSETTER, KDYNAMIC, KFUNCTION, KINLINE, KMACRO2, KOVERRIDE,
      KPRIVATE, KPUBLIC, KSTATIC, MACRO_ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<function prototype declaration with attributes>");
    r = functionPrototypeDeclarationWithAttributes_0(b, l + 1);
    r = r && consumeToken(b, KFUNCTION);
    r = r && functionPrototypeDeclarationWithAttributes_2(b, l + 1);
    p = r; // pin = 3
    r = r && report_error_(b, functionPrototypeDeclarationWithAttributes_3(b, l + 1));
    r = p && report_error_(b, consumeToken(b, PLPAREN)) && r;
    r = p && report_error_(b, parameterList(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, functionPrototypeDeclarationWithAttributes_7(b, l + 1)) && r;
    r = p && report_error_(b, functionPrototypeDeclarationWithAttributes_8(b, l + 1)) && r;
    r = p && consumeToken(b, OSEMI) && r;
    exit_section_(b, l, m, FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES, r, p, null);
    return r || p;
  }

  // (functionMacroMember| declarationAttribute)*
  private static boolean functionPrototypeDeclarationWithAttributes_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionPrototypeDeclarationWithAttributes_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!functionPrototypeDeclarationWithAttributes_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "functionPrototypeDeclarationWithAttributes_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // functionMacroMember| declarationAttribute
  private static boolean functionPrototypeDeclarationWithAttributes_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionPrototypeDeclarationWithAttributes_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = functionMacroMember(b, l + 1);
    if (!r) r = declarationAttribute(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // constructorName | componentName
  private static boolean functionPrototypeDeclarationWithAttributes_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionPrototypeDeclarationWithAttributes_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = constructorName(b, l + 1);
    if (!r) r = componentName(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // genericParam?
  private static boolean functionPrototypeDeclarationWithAttributes_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionPrototypeDeclarationWithAttributes_3")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // typeTag?
  private static boolean functionPrototypeDeclarationWithAttributes_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionPrototypeDeclarationWithAttributes_7")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionPrototypeDeclarationWithAttributes_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionPrototypeDeclarationWithAttributes_8")) return false;
    consumeToken(b, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // '->' '?'? typeOrAnonymous
  public static boolean functionType(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionType")) return false;
    if (!nextTokenIs(b, OARROW)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, OARROW);
    r = r && functionType_1(b, l + 1);
    r = r && typeOrAnonymous(b, l + 1);
    exit_section_(b, l, m, FUNCTION_TYPE, r, false, null);
    return r;
  }

  // '?'?
  private static boolean functionType_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionType_1")) return false;
    consumeToken(b, OQUEST);
    return true;
  }

  /* ********************************************************** */
  // typeOrAnonymous | '(' functionTypeWrapper ')'
  static boolean functionTypeOrWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionTypeOrWrapper")) return false;
    if (!nextTokenIs(b, "", PLPAREN, PLCURLY, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = typeOrAnonymous(b, l + 1);
    if (!r) r = functionTypeOrWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '(' functionTypeWrapper ')'
  private static boolean functionTypeOrWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionTypeOrWrapper_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLPAREN);
    r = r && functionTypeWrapper(b, l + 1);
    r = r && consumeToken(b, PRPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // functionTypeOrWrapper functionType*
  static boolean functionTypeWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionTypeWrapper")) return false;
    if (!nextTokenIs(b, "", PLPAREN, PLCURLY, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = functionTypeOrWrapper(b, l + 1);
    r = r && functionTypeWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // functionType*
  private static boolean functionTypeWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "functionTypeWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!functionType(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "functionTypeWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // componentName (':' ('(' typeList ')' | typeListPart))?
  public static boolean genericListPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericListPart")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = componentName(b, l + 1);
    r = r && genericListPart_1(b, l + 1);
    exit_section_(b, m, GENERIC_LIST_PART, r);
    return r;
  }

  // (':' ('(' typeList ')' | typeListPart))?
  private static boolean genericListPart_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericListPart_1")) return false;
    genericListPart_1_0(b, l + 1);
    return true;
  }

  // ':' ('(' typeList ')' | typeListPart)
  private static boolean genericListPart_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericListPart_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOLON);
    r = r && genericListPart_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '(' typeList ')' | typeListPart
  private static boolean genericListPart_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericListPart_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = genericListPart_1_0_1_0(b, l + 1);
    if (!r) r = typeListPart(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '(' typeList ')'
  private static boolean genericListPart_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericListPart_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLPAREN);
    r = r && typeList(b, l + 1);
    r = r && consumeToken(b, PRPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '<' genericListPart (',' genericListPart)* '>'
  public static boolean genericParam(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericParam")) return false;
    if (!nextTokenIs(b, OLESS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OLESS);
    r = r && genericListPart(b, l + 1);
    r = r && genericParam_2(b, l + 1);
    r = r && consumeToken(b, OGREATER);
    exit_section_(b, m, GENERIC_PARAM, r);
    return r;
  }

  // (',' genericListPart)*
  private static boolean genericParam_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericParam_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!genericParam_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "genericParam_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' genericListPart
  private static boolean genericParam_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "genericParam_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && genericListPart(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:getter' '(' referenceExpression ')'
  public static boolean getterMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "getterMeta")) return false;
    if (!nextTokenIs(b, KGETTER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KGETTER);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, referenceExpression(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, GETTER_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '@:hack'
  public static boolean hackMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hackMeta")) return false;
    if (!nextTokenIs(b, KHACK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KHACK);
    exit_section_(b, m, HACK_META, r);
    return r;
  }

  /* ********************************************************** */
  // packageStatement? topLevelList
  static boolean haxeFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "haxeFile")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = haxeFile_0(b, l + 1);
    r = r && topLevelList(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // packageStatement?
  private static boolean haxeFile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "haxeFile_0")) return false;
    packageStatement(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ID
  public static boolean identifier(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identifier")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ID);
    exit_section_(b, m, IDENTIFIER, r);
    return r;
  }

  /* ********************************************************** */
  // 'if' '(' expression ')' statement ';'? ('else' statement ';'?)?
  public static boolean ifStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement")) return false;
    if (!nextTokenIs(b, KIF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KIF);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, expression(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, statement(b, l + 1)) && r;
    r = p && report_error_(b, ifStatement_5(b, l + 1)) && r;
    r = p && ifStatement_6(b, l + 1) && r;
    exit_section_(b, l, m, IF_STATEMENT, r, p, null);
    return r || p;
  }

  // ';'?
  private static boolean ifStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_5")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  // ('else' statement ';'?)?
  private static boolean ifStatement_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_6")) return false;
    ifStatement_6_0(b, l + 1);
    return true;
  }

  // 'else' statement ';'?
  private static boolean ifStatement_6_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_6_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KELSE);
    r = r && statement(b, l + 1);
    r = r && ifStatement_6_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean ifStatement_6_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ifStatement_6_0_2")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // ('implements' type)+
  public static boolean implementsDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "implementsDeclaration")) return false;
    if (!nextTokenIs(b, KIMPLEMENTS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = implementsDeclaration_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!implementsDeclaration_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "implementsDeclaration", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, IMPLEMENTS_DECLARATION, r);
    return r;
  }

  // 'implements' type
  private static boolean implementsDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "implementsDeclaration_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KIMPLEMENTS);
    r = r && type(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // importStatementWithWildcard | importStatementWithInSupport | importStatementRegular
  static boolean importStatementAll(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importStatementAll")) return false;
    if (!nextTokenIs(b, KIMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = importStatementWithWildcard(b, l + 1);
    if (!r) r = importStatementWithInSupport(b, l + 1);
    if (!r) r = importStatementRegular(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpression ';'
  public static boolean importStatementRegular(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importStatementRegular")) return false;
    if (!nextTokenIs(b, KIMPORT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KIMPORT);
    p = r; // pin = 1
    r = r && report_error_(b, simpleQualifiedReferenceExpression(b, l + 1));
    r = p && consumeToken(b, OSEMI) && r;
    exit_section_(b, l, m, IMPORT_STATEMENT_REGULAR, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpression ('in' | 'as') identifier';'
  public static boolean importStatementWithInSupport(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importStatementWithInSupport")) return false;
    if (!nextTokenIs(b, KIMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KIMPORT);
    r = r && simpleQualifiedReferenceExpression(b, l + 1);
    r = r && importStatementWithInSupport_2(b, l + 1);
    r = r && identifier(b, l + 1);
    r = r && consumeToken(b, OSEMI);
    exit_section_(b, m, IMPORT_STATEMENT_WITH_IN_SUPPORT, r);
    return r;
  }

  // 'in' | 'as'
  private static boolean importStatementWithInSupport_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importStatementWithInSupport_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OIN);
    if (!r) r = consumeToken(b, "as");
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpressionWithWildcardSupport ';'
  public static boolean importStatementWithWildcard(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importStatementWithWildcard")) return false;
    if (!nextTokenIs(b, KIMPORT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KIMPORT);
    r = r && simpleQualifiedReferenceExpressionWithWildcardSupport(b, l + 1);
    r = r && consumeToken(b, OSEMI);
    exit_section_(b, m, IMPORT_STATEMENT_WITH_WILDCARD, r);
    return r;
  }

  /* ********************************************************** */
  // extendsDeclaration | implementsDeclaration
  static boolean inherit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inherit")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = extendsDeclaration(b, l + 1);
    if (!r) r = implementsDeclaration(b, l + 1);
    exit_section_(b, l, m, null, r, false, inherit_recover_parser_);
    return r;
  }

  /* ********************************************************** */
  // inherit (','? inherit)*
  public static boolean inheritList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inheritList")) return false;
    if (!nextTokenIs(b, "<inherit list>", KEXTENDS, KIMPLEMENTS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<inherit list>");
    r = inherit(b, l + 1);
    r = r && inheritList_1(b, l + 1);
    exit_section_(b, l, m, INHERIT_LIST, r, false, null);
    return r;
  }

  // (','? inherit)*
  private static boolean inheritList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inheritList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!inheritList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "inheritList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ','? inherit
  private static boolean inheritList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inheritList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = inheritList_1_0_0(b, l + 1);
    r = r && inherit(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean inheritList_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inheritList_1_0_0")) return false;
    consumeToken(b, OCOMMA);
    return true;
  }

  /* ********************************************************** */
  // !(',' | '{' | 'extends' | 'implements')
  static boolean inherit_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inherit_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !inherit_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ',' | '{' | 'extends' | 'implements'
  private static boolean inherit_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inherit_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    if (!r) r = consumeToken(b, PLCURLY);
    if (!r) r = consumeToken(b, KEXTENDS);
    if (!r) r = consumeToken(b, KIMPLEMENTS);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // interfaceBodyPart*
  public static boolean interfaceBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceBody")) return false;
    Marker m = enter_section_(b, l, _NONE_, "<interface body>");
    int c = current_position_(b);
    while (true) {
      if (!interfaceBodyPart(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "interfaceBody", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, INTERFACE_BODY, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionPrototypeDeclarationWithAttributes
  static boolean interfaceBodyPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceBodyPart")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = varDeclaration(b, l + 1);
    if (!r) r = functionPrototypeDeclarationWithAttributes(b, l + 1);
    exit_section_(b, l, m, null, r, false, interface_body_part_recover_parser_);
    return r;
  }

  /* ********************************************************** */
  // macroClassList? privateKeyWord? 'interface' componentName genericParam? inheritList? '{' interfaceBody '}'
  public static boolean interfaceDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<interface declaration>");
    r = interfaceDeclaration_0(b, l + 1);
    r = r && interfaceDeclaration_1(b, l + 1);
    r = r && consumeToken(b, KINTERFACE);
    p = r; // pin = 3
    r = r && report_error_(b, componentName(b, l + 1));
    r = p && report_error_(b, interfaceDeclaration_4(b, l + 1)) && r;
    r = p && report_error_(b, interfaceDeclaration_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLCURLY)) && r;
    r = p && report_error_(b, interfaceBody(b, l + 1)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, INTERFACE_DECLARATION, r, p, null);
    return r || p;
  }

  // macroClassList?
  private static boolean interfaceDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceDeclaration_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // privateKeyWord?
  private static boolean interfaceDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceDeclaration_1")) return false;
    privateKeyWord(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean interfaceDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceDeclaration_4")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // inheritList?
  private static boolean interfaceDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interfaceDeclaration_5")) return false;
    inheritList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // !(ppToken | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}')
  static boolean interface_body_part_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_body_part_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !interface_body_part_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ppToken | metaKeyWord | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}'
  private static boolean interface_body_part_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interface_body_part_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ppToken(b, l + 1);
    if (!r) r = metaKeyWord(b, l + 1);
    if (!r) r = consumeToken(b, KDYNAMIC);
    if (!r) r = consumeToken(b, KFUNCTION);
    if (!r) r = consumeToken(b, KINLINE);
    if (!r) r = consumeToken(b, KOVERRIDE);
    if (!r) r = consumeToken(b, KPRIVATE);
    if (!r) r = consumeToken(b, KPUBLIC);
    if (!r) r = consumeToken(b, KSTATIC);
    if (!r) r = consumeToken(b, KVAR);
    if (!r) r = consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expression
  public static boolean iterable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iterable")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<iterable>");
    r = expression(b, l + 1);
    exit_section_(b, l, m, ITERABLE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '...' ternaryExpressionWrapper
  public static boolean iteratorExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iteratorExpression")) return false;
    if (!nextTokenIs(b, OTRIPLE_DOT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, OTRIPLE_DOT);
    p = r; // pin = 1
    r = r && ternaryExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, ITERATOR_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ternaryExpressionWrapper iteratorExpression?
  static boolean iteratorExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iteratorExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ternaryExpressionWrapper(b, l + 1);
    r = r && iteratorExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // iteratorExpression?
  private static boolean iteratorExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "iteratorExpressionWrapper_1")) return false;
    iteratorExpression(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // "@:jsRequire" '(' stringLiteralExpression ')'
  public static boolean jsRequireMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "jsRequireMeta")) return false;
    if (!nextTokenIs(b, KJSREQUIRE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KJSREQUIRE);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, stringLiteralExpression(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, JS_REQUIRE_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '@:keep'
  public static boolean keepMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keepMeta")) return false;
    if (!nextTokenIs(b, KKEEP)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KKEEP);
    exit_section_(b, m, KEEP_META, r);
    return r;
  }

  /* ********************************************************** */
  // LITINT | LITHEX | LITOCT | LITFLOAT
  //                     | regularExpressionLiteral
  //                     | 'null' | 'true' | 'false'
  //                     | functionLiteral
  //                     | arrayLiteral
  //                     | objectLiteral
  //                     | stringLiteralExpression
  public static boolean literalExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "literalExpression")) return false;
    if (!nextTokenIs(b, "<literal expression>", PLBRACK, KFALSE,
      KFUNCTION, KNULL, KTRUE, PLCURLY, LITFLOAT, LITHEX,
      LITINT, LITOCT, OPEN_QUOTE, REG_EXP)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, "<literal expression>");
    r = consumeToken(b, LITINT);
    if (!r) r = consumeToken(b, LITHEX);
    if (!r) r = consumeToken(b, LITOCT);
    if (!r) r = consumeToken(b, LITFLOAT);
    if (!r) r = regularExpressionLiteral(b, l + 1);
    if (!r) r = consumeToken(b, KNULL);
    if (!r) r = consumeToken(b, KTRUE);
    if (!r) r = consumeToken(b, KFALSE);
    if (!r) r = functionLiteral(b, l + 1);
    if (!r) r = arrayLiteral(b, l + 1);
    if (!r) r = objectLiteral(b, l + 1);
    if (!r) r = stringLiteralExpression(b, l + 1);
    exit_section_(b, l, m, LITERAL_EXPRESSION, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // localFunctionDeclarationAttribute? 'function' componentName genericParam? '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean localFunctionDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localFunctionDeclaration")) return false;
    if (!nextTokenIs(b, "<local function declaration>", KFUNCTION, KINLINE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<local function declaration>");
    r = localFunctionDeclaration_0(b, l + 1);
    r = r && consumeToken(b, KFUNCTION);
    p = r; // pin = 2
    r = r && report_error_(b, componentName(b, l + 1));
    r = p && report_error_(b, localFunctionDeclaration_3(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PLPAREN)) && r;
    r = p && report_error_(b, localFunctionDeclaration_5(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, localFunctionDeclaration_7(b, l + 1)) && r;
    r = p && report_error_(b, localFunctionDeclaration_8(b, l + 1)) && r;
    r = p && functionCommonBody(b, l + 1) && r;
    exit_section_(b, l, m, LOCAL_FUNCTION_DECLARATION, r, p, null);
    return r || p;
  }

  // localFunctionDeclarationAttribute?
  private static boolean localFunctionDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localFunctionDeclaration_0")) return false;
    localFunctionDeclarationAttribute(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean localFunctionDeclaration_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localFunctionDeclaration_3")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // parameterList?
  private static boolean localFunctionDeclaration_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localFunctionDeclaration_5")) return false;
    parameterList(b, l + 1);
    return true;
  }

  // typeTag?
  private static boolean localFunctionDeclaration_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localFunctionDeclaration_7")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // 'untyped'?
  private static boolean localFunctionDeclaration_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localFunctionDeclaration_8")) return false;
    consumeToken(b, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // 'inline'
  static boolean localFunctionDeclarationAttribute(PsiBuilder b, int l) {
    return consumeToken(b, KINLINE);
  }

  /* ********************************************************** */
  // 'var' localVarDeclarationPartList ';'?
  public static boolean localVarDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclaration")) return false;
    if (!nextTokenIs(b, KVAR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KVAR);
    r = r && localVarDeclarationPartList(b, l + 1);
    p = r; // pin = 2
    r = r && localVarDeclaration_2(b, l + 1);
    exit_section_(b, l, m, LOCAL_VAR_DECLARATION, r, p, null);
    return r || p;
  }

  // ';'?
  private static boolean localVarDeclaration_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclaration_2")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // componentName propertyDeclaration? typeTag? varInit?
  public static boolean localVarDeclarationPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclarationPart")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<local var declaration part>");
    r = componentName(b, l + 1);
    r = r && localVarDeclarationPart_1(b, l + 1);
    r = r && localVarDeclarationPart_2(b, l + 1);
    r = r && localVarDeclarationPart_3(b, l + 1);
    exit_section_(b, l, m, LOCAL_VAR_DECLARATION_PART, r, false, local_var_declaration_part_recover_parser_);
    return r;
  }

  // propertyDeclaration?
  private static boolean localVarDeclarationPart_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclarationPart_1")) return false;
    propertyDeclaration(b, l + 1);
    return true;
  }

  // typeTag?
  private static boolean localVarDeclarationPart_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclarationPart_2")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // varInit?
  private static boolean localVarDeclarationPart_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclarationPart_3")) return false;
    varInit(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // localVarDeclarationPart (',' localVarDeclarationPart)*
  static boolean localVarDeclarationPartList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclarationPartList")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = localVarDeclarationPart(b, l + 1);
    r = r && localVarDeclarationPartList_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' localVarDeclarationPart)*
  private static boolean localVarDeclarationPartList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclarationPartList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!localVarDeclarationPartList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "localVarDeclarationPartList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' localVarDeclarationPart
  private static boolean localVarDeclarationPartList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "localVarDeclarationPartList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && localVarDeclarationPart(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !('!' | ppToken | '(' | ')' | '++' | ',' | '-' | '--' | ';' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'else' | 'false' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '}' | '~' | ID | OPEN_QUOTE | LITFLOAT | LITHEX | LITINT | LITOCT | REG_EXP)
  static boolean local_var_declaration_part_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "local_var_declaration_part_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !local_var_declaration_part_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // '!' | ppToken | '(' | ')' | '++' | ',' | '-' | '--' | ';' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'else' | 'false' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '}' | '~' | ID | OPEN_QUOTE | LITFLOAT | LITHEX | LITINT | LITOCT | REG_EXP
  private static boolean local_var_declaration_part_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "local_var_declaration_part_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ONOT);
    if (!r) r = ppToken(b, l + 1);
    if (!r) r = consumeToken(b, PLPAREN);
    if (!r) r = consumeToken(b, PRPAREN);
    if (!r) r = consumeToken(b, OPLUS_PLUS);
    if (!r) r = consumeToken(b, OCOMMA);
    if (!r) r = consumeToken(b, OMINUS);
    if (!r) r = consumeToken(b, OMINUS_MINUS);
    if (!r) r = consumeToken(b, OSEMI);
    if (!r) r = consumeToken(b, PLBRACK);
    if (!r) r = consumeToken(b, KBREAK);
    if (!r) r = consumeToken(b, KCASE);
    if (!r) r = consumeToken(b, KCAST);
    if (!r) r = consumeToken(b, KCONTINUE);
    if (!r) r = consumeToken(b, KDEFAULT);
    if (!r) r = consumeToken(b, KDO);
    if (!r) r = consumeToken(b, KELSE);
    if (!r) r = consumeToken(b, KFALSE);
    if (!r) r = consumeToken(b, KFOR);
    if (!r) r = consumeToken(b, KFUNCTION);
    if (!r) r = consumeToken(b, KIF);
    if (!r) r = consumeToken(b, ONEW);
    if (!r) r = consumeToken(b, KNULL);
    if (!r) r = consumeToken(b, KRETURN);
    if (!r) r = consumeToken(b, KSUPER);
    if (!r) r = consumeToken(b, KSWITCH);
    if (!r) r = consumeToken(b, KTHIS);
    if (!r) r = consumeToken(b, KTHROW);
    if (!r) r = consumeToken(b, KTRUE);
    if (!r) r = consumeToken(b, KTRY);
    if (!r) r = consumeToken(b, KUNTYPED);
    if (!r) r = consumeToken(b, KVAR);
    if (!r) r = consumeToken(b, KWHILE);
    if (!r) r = consumeToken(b, PLCURLY);
    if (!r) r = consumeToken(b, PRCURLY);
    if (!r) r = consumeToken(b, OCOMPLEMENT);
    if (!r) r = consumeToken(b, ID);
    if (!r) r = consumeToken(b, OPEN_QUOTE);
    if (!r) r = consumeToken(b, LITFLOAT);
    if (!r) r = consumeToken(b, LITHEX);
    if (!r) r = consumeToken(b, LITINT);
    if (!r) r = consumeToken(b, LITOCT);
    if (!r) r = consumeToken(b, REG_EXP);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '&&' compareExpressionWrapper
  public static boolean logicAndExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logicAndExpression")) return false;
    if (!nextTokenIs(b, OCOND_AND)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, OCOND_AND);
    p = r; // pin = 1
    r = r && compareExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, LOGIC_AND_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // compareExpressionWrapper logicAndExpression*
  static boolean logicAndExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logicAndExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = compareExpressionWrapper(b, l + 1);
    r = r && logicAndExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // logicAndExpression*
  private static boolean logicAndExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logicAndExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!logicAndExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "logicAndExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // '||' logicAndExpressionWrapper
  public static boolean logicOrExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logicOrExpression")) return false;
    if (!nextTokenIs(b, OCOND_OR)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, OCOND_OR);
    p = r; // pin = 1
    r = r && logicAndExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, LOGIC_OR_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // logicAndExpressionWrapper logicOrExpression*
  static boolean logicOrExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logicOrExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = logicAndExpressionWrapper(b, l + 1);
    r = r && logicOrExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // logicOrExpression*
  private static boolean logicOrExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logicOrExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!logicOrExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "logicOrExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // LONG_TEMPLATE_ENTRY_START expression LONG_TEMPLATE_ENTRY_END
  public static boolean longTemplateEntry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "longTemplateEntry")) return false;
    if (!nextTokenIs(b, LONG_TEMPLATE_ENTRY_START)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, LONG_TEMPLATE_ENTRY_START);
    p = r; // pin = 1
    r = r && report_error_(b, expression(b, l + 1));
    r = p && consumeToken(b, LONG_TEMPLATE_ENTRY_END) && r;
    exit_section_(b, l, m, LONG_TEMPLATE_ENTRY, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // simpleMeta | requireMeta | fakeEnumMeta | nativeMeta | jsRequireMeta | bitmapMeta | nsMeta | customMeta | metaMeta | buildMacro | autoBuildMacro
  public static boolean macroClass(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "macroClass")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<macro class>");
    r = simpleMeta(b, l + 1);
    if (!r) r = requireMeta(b, l + 1);
    if (!r) r = fakeEnumMeta(b, l + 1);
    if (!r) r = nativeMeta(b, l + 1);
    if (!r) r = jsRequireMeta(b, l + 1);
    if (!r) r = bitmapMeta(b, l + 1);
    if (!r) r = nsMeta(b, l + 1);
    if (!r) r = customMeta(b, l + 1);
    if (!r) r = metaMeta(b, l + 1);
    if (!r) r = buildMacro(b, l + 1);
    if (!r) r = autoBuildMacro(b, l + 1);
    exit_section_(b, l, m, MACRO_CLASS, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // macroClass (macroClass)*
  public static boolean macroClassList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "macroClassList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<macro class list>");
    r = macroClass(b, l + 1);
    r = r && macroClassList_1(b, l + 1);
    exit_section_(b, l, m, MACRO_CLASS_LIST, r, false, null);
    return r;
  }

  // (macroClass)*
  private static boolean macroClassList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "macroClassList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!macroClassList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "macroClassList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // (macroClass)
  private static boolean macroClassList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "macroClassList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = macroClass(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // macroMeta | protectedMeta | debugMeta | noDebugMeta | keepMeta | nativeMeta
  //                        | requireMeta | nsMeta | getterMeta | setterMeta | customMeta | metaMeta | buildMacro | autoBuildMacro
  static boolean macroMember(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "macroMember")) return false;
    if (!nextTokenIs(b, "", KAUTOBUILD, KBUILD,
      KDEBUG, KGETTER, KKEEP, KMACRO, KMETA, KNATIVE,
      KNODEBUG, KNS, KPROTECTED, KREQUIRE, KSETTER, MACRO_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = macroMeta(b, l + 1);
    if (!r) r = protectedMeta(b, l + 1);
    if (!r) r = debugMeta(b, l + 1);
    if (!r) r = noDebugMeta(b, l + 1);
    if (!r) r = keepMeta(b, l + 1);
    if (!r) r = nativeMeta(b, l + 1);
    if (!r) r = requireMeta(b, l + 1);
    if (!r) r = nsMeta(b, l + 1);
    if (!r) r = getterMeta(b, l + 1);
    if (!r) r = setterMeta(b, l + 1);
    if (!r) r = customMeta(b, l + 1);
    if (!r) r = metaMeta(b, l + 1);
    if (!r) r = buildMacro(b, l + 1);
    if (!r) r = autoBuildMacro(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:macro'
  static boolean macroMeta(PsiBuilder b, int l) {
    return consumeToken(b, KMACRO);
  }

  /* ********************************************************** */
  // ID '=' stringLiteralExpression
  public static boolean metaKeyValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "metaKeyValue")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ID);
    r = r && consumeToken(b, OASSIGN);
    r = r && stringLiteralExpression(b, l + 1);
    exit_section_(b, m, META_KEY_VALUE, r);
    return r;
  }

  /* ********************************************************** */
  // MACRO_ID | '@:final' | '@:hack' | '@:native' | '@:macro' | '@:build' | '@:autoBuild' | '@:keep' | '@:require' | '@:fakeEnum' | '@:core_api' | '@:bind' | '@:bitmap' | '@:ns' | '@:protected' | '@:getter' | '@:setter' | '@:debug' | '@:nodebug' | '@:meta' | '@:overload' | '@:jsRequire'
  static boolean metaKeyWord(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "metaKeyWord")) return false;
    if (!nextTokenIs(b, "", KAUTOBUILD, KBIND,
      KBITMAP, KBUILD, KCOREAPI, KDEBUG, KFAKEENUM, KFINAL,
      KGETTER, KHACK, KJSREQUIRE, KKEEP, KMACRO, KMETA,
      KNATIVE, KNODEBUG, KNS, KOVERLOAD, KPROTECTED, KREQUIRE, KSETTER, MACRO_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MACRO_ID);
    if (!r) r = consumeToken(b, KFINAL);
    if (!r) r = consumeToken(b, KHACK);
    if (!r) r = consumeToken(b, KNATIVE);
    if (!r) r = consumeToken(b, KMACRO);
    if (!r) r = consumeToken(b, KBUILD);
    if (!r) r = consumeToken(b, KAUTOBUILD);
    if (!r) r = consumeToken(b, KKEEP);
    if (!r) r = consumeToken(b, KREQUIRE);
    if (!r) r = consumeToken(b, KFAKEENUM);
    if (!r) r = consumeToken(b, KCOREAPI);
    if (!r) r = consumeToken(b, KBIND);
    if (!r) r = consumeToken(b, KBITMAP);
    if (!r) r = consumeToken(b, KNS);
    if (!r) r = consumeToken(b, KPROTECTED);
    if (!r) r = consumeToken(b, KGETTER);
    if (!r) r = consumeToken(b, KSETTER);
    if (!r) r = consumeToken(b, KDEBUG);
    if (!r) r = consumeToken(b, KNODEBUG);
    if (!r) r = consumeToken(b, KMETA);
    if (!r) r = consumeToken(b, KOVERLOAD);
    if (!r) r = consumeToken(b, KJSREQUIRE);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:meta' '(' ID '(' metaPartList? ')' ')'
  public static boolean metaMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "metaMeta")) return false;
    if (!nextTokenIs(b, KMETA)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KMETA);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, consumeToken(b, ID)) && r;
    r = p && report_error_(b, consumeToken(b, PLPAREN)) && r;
    r = p && report_error_(b, metaMeta_4(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, META_META, r, p, null);
    return r || p;
  }

  // metaPartList?
  private static boolean metaMeta_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "metaMeta_4")) return false;
    metaPartList(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // metaKeyValue (',' metaKeyValue)*
  static boolean metaPartList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "metaPartList")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = metaKeyValue(b, l + 1);
    r = r && metaPartList_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' metaKeyValue)*
  private static boolean metaPartList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "metaPartList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!metaPartList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "metaPartList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' metaKeyValue
  private static boolean metaPartList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "metaPartList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && metaKeyValue(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ('*' | '/' | '%') (prefixExpression | suffixExpressionWrapper)
  public static boolean multiplicativeExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicativeExpression")) return false;
    if (!nextTokenIs(b, "<multiplicative expression>", OREMAINDER, OMUL, OQUOTIENT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, "<multiplicative expression>");
    r = multiplicativeExpression_0(b, l + 1);
    p = r; // pin = 1
    r = r && multiplicativeExpression_1(b, l + 1);
    exit_section_(b, l, m, MULTIPLICATIVE_EXPRESSION, r, p, null);
    return r || p;
  }

  // '*' | '/' | '%'
  private static boolean multiplicativeExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicativeExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OMUL);
    if (!r) r = consumeToken(b, OQUOTIENT);
    if (!r) r = consumeToken(b, OREMAINDER);
    exit_section_(b, m, null, r);
    return r;
  }

  // prefixExpression | suffixExpressionWrapper
  private static boolean multiplicativeExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicativeExpression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = prefixExpression(b, l + 1);
    if (!r) r = suffixExpressionWrapper(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // prefixExpression multiplicativeExpression*
  static boolean multiplicativeExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicativeExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = prefixExpression(b, l + 1);
    r = r && multiplicativeExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // multiplicativeExpression*
  private static boolean multiplicativeExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "multiplicativeExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!multiplicativeExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "multiplicativeExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // '@:native' '(' stringLiteralExpression ')'
  public static boolean nativeMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "nativeMeta")) return false;
    if (!nextTokenIs(b, KNATIVE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KNATIVE);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, stringLiteralExpression(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, NATIVE_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'new' type '(' (expression (',' expression)*)? ')'
  public static boolean newExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpression")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<new expression>");
    r = consumeToken(b, ONEW);
    r = r && type(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, newExpression_3(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, NEW_EXPRESSION, r, p, expression_recover_parser_);
    return r || p;
  }

  // (expression (',' expression)*)?
  private static boolean newExpression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpression_3")) return false;
    newExpression_3_0(b, l + 1);
    return true;
  }

  // expression (',' expression)*
  private static boolean newExpression_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpression_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1);
    r = r && newExpression_3_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' expression)*
  private static boolean newExpression_3_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpression_3_0_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!newExpression_3_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "newExpression_3_0_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' expression
  private static boolean newExpression_3_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpression_3_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // newExpression qualifiedReferenceTail?
  static boolean newExpressionOrCall(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpressionOrCall")) return false;
    if (!nextTokenIs(b, ONEW)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = newExpression(b, l + 1);
    r = r && newExpressionOrCall_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // qualifiedReferenceTail?
  private static boolean newExpressionOrCall_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "newExpressionOrCall_1")) return false;
    qualifiedReferenceTail(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '@:nodebug'
  public static boolean noDebugMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "noDebugMeta")) return false;
    if (!nextTokenIs(b, KNODEBUG)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KNODEBUG);
    exit_section_(b, m, NO_DEBUG_META, r);
    return r;
  }

  /* ********************************************************** */
  // ('untyped' statement ';'?)
  //                             | ('macro' statement ';'?)
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
  static boolean notBlockStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notBlockStatement")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = notBlockStatement_0(b, l + 1);
    if (!r) r = notBlockStatement_1(b, l + 1);
    if (!r) r = localVarDeclaration(b, l + 1);
    if (!r) r = localFunctionDeclaration(b, l + 1);
    if (!r) r = ifStatement(b, l + 1);
    if (!r) r = forStatement(b, l + 1);
    if (!r) r = whileStatement(b, l + 1);
    if (!r) r = doWhileStatement(b, l + 1);
    if (!r) r = returnStatement(b, l + 1);
    if (!r) r = breakStatement(b, l + 1);
    if (!r) r = continueStatement(b, l + 1);
    if (!r) r = switchStatement(b, l + 1);
    if (!r) r = throwStatement(b, l + 1);
    if (!r) r = tryStatement(b, l + 1);
    if (!r) r = expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // 'untyped' statement ';'?
  private static boolean notBlockStatement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notBlockStatement_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KUNTYPED);
    r = r && statement(b, l + 1);
    r = r && notBlockStatement_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean notBlockStatement_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notBlockStatement_0_2")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  // 'macro' statement ';'?
  private static boolean notBlockStatement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notBlockStatement_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KMACRO2);
    r = r && statement(b, l + 1);
    r = r && notBlockStatement_1_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean notBlockStatement_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "notBlockStatement_1_2")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // '@:ns' '(' stringLiteralExpression ')'
  public static boolean nsMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "nsMeta")) return false;
    if (!nextTokenIs(b, KNS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KNS);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, stringLiteralExpression(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, NS_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ('{' objectLiteralElementList+ '}') | emptyObjectLiteral
  public static boolean objectLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "objectLiteral")) return false;
    if (!nextTokenIs(b, PLCURLY)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = objectLiteral_0(b, l + 1);
    if (!r) r = emptyObjectLiteral(b, l + 1);
    exit_section_(b, m, OBJECT_LITERAL, r);
    return r;
  }

  // '{' objectLiteralElementList+ '}'
  private static boolean objectLiteral_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "objectLiteral_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLCURLY);
    r = r && objectLiteral_0_1(b, l + 1);
    r = r && consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  // objectLiteralElementList+
  private static boolean objectLiteral_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "objectLiteral_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = objectLiteralElementList(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!objectLiteralElementList(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "objectLiteral_0_1", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // identifier ':' expression
  public static boolean objectLiteralElement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "objectLiteralElement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<object literal element>");
    r = identifier(b, l + 1);
    r = r && consumeToken(b, OCOLON);
    r = r && expression(b, l + 1);
    exit_section_(b, l, m, OBJECT_LITERAL_ELEMENT, r, false, object_literal_part_recover_parser_);
    return r;
  }

  /* ********************************************************** */
  // objectLiteralElement (',' objectLiteralElement)*
  static boolean objectLiteralElementList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "objectLiteralElementList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = objectLiteralElement(b, l + 1);
    r = r && objectLiteralElementList_1(b, l + 1);
    exit_section_(b, l, m, null, r, false, object_literal_list_recover_parser_);
    return r;
  }

  // (',' objectLiteralElement)*
  private static boolean objectLiteralElementList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "objectLiteralElementList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!objectLiteralElementList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "objectLiteralElementList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' objectLiteralElement
  private static boolean objectLiteralElementList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "objectLiteralElementList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && objectLiteralElement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !('}')
  static boolean object_literal_list_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_list_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !object_literal_list_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ('}')
  private static boolean object_literal_list_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_list_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(',' | '}')
  static boolean object_literal_part_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_part_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !object_literal_part_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ',' | '}'
  private static boolean object_literal_part_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "object_literal_part_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    if (!r) r = consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '@:overload' '(' anonymousFunctionDeclaration ')'
  public static boolean overloadMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "overloadMeta")) return false;
    if (!nextTokenIs(b, KOVERLOAD)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KOVERLOAD);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, anonymousFunctionDeclaration(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, OVERLOAD_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'package' simpleQualifiedReferenceExpression? ';'
  public static boolean packageStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageStatement")) return false;
    if (!nextTokenIs(b, KPACKAGE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KPACKAGE);
    p = r; // pin = 1
    r = r && report_error_(b, packageStatement_1(b, l + 1));
    r = p && consumeToken(b, OSEMI) && r;
    exit_section_(b, l, m, PACKAGE_STATEMENT, r, p, null);
    return r || p;
  }

  // simpleQualifiedReferenceExpression?
  private static boolean packageStatement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "packageStatement_1")) return false;
    simpleQualifiedReferenceExpression(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '?'? componentName typeTag? varInit?
  public static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    if (!nextTokenIs(b, "<parameter>", OQUEST, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<parameter>");
    r = parameter_0(b, l + 1);
    r = r && componentName(b, l + 1);
    r = r && parameter_2(b, l + 1);
    r = r && parameter_3(b, l + 1);
    exit_section_(b, l, m, PARAMETER, r, false, null);
    return r;
  }

  // '?'?
  private static boolean parameter_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_0")) return false;
    consumeToken(b, OQUEST);
    return true;
  }

  // typeTag?
  private static boolean parameter_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_2")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // varInit?
  private static boolean parameter_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_3")) return false;
    varInit(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // parameter? (',' parameter)*
  public static boolean parameterList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<parameter list>");
    r = parameterList_0(b, l + 1);
    r = r && parameterList_1(b, l + 1);
    exit_section_(b, l, m, PARAMETER_LIST, r, false, parameterListRecovery_parser_);
    return r;
  }

  // parameter?
  private static boolean parameterList_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterList_0")) return false;
    parameter(b, l + 1);
    return true;
  }

  // (',' parameter)*
  private static boolean parameterList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!parameterList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameterList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' parameter
  private static boolean parameterList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !')'
  static boolean parameterListRecovery(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameterListRecovery")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !consumeToken(b, PRPAREN);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '(' (expression typeTag | expression | statement) ')'
  public static boolean parenthesizedExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesizedExpression")) return false;
    if (!nextTokenIs(b, PLPAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, PLPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, parenthesizedExpression_1(b, l + 1));
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, PARENTHESIZED_EXPRESSION, r, p, null);
    return r || p;
  }

  // expression typeTag | expression | statement
  private static boolean parenthesizedExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesizedExpression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parenthesizedExpression_1_0(b, l + 1);
    if (!r) r = expression(b, l + 1);
    if (!r) r = statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expression typeTag
  private static boolean parenthesizedExpression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesizedExpression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1);
    r = r && typeTag(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // parenthesizedExpression qualifiedReferenceTail?
  static boolean parenthesizedExpressionOrCall(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesizedExpressionOrCall")) return false;
    if (!nextTokenIs(b, PLPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parenthesizedExpression(b, l + 1);
    r = r && parenthesizedExpressionOrCall_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // qualifiedReferenceTail?
  private static boolean parenthesizedExpressionOrCall_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesizedExpressionOrCall_1")) return false;
    qualifiedReferenceTail(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // CONDITIONAL_STATEMENT_ID
  static boolean ppConditionalStatement(PsiBuilder b, int l) {
    return consumeToken(b, CONDITIONAL_STATEMENT_ID);
  }

  /* ********************************************************** */
  // '#if' | "#else" | "#elseif" | "#end" | "#error" | ppConditionalStatement
  static boolean ppToken(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ppToken")) return false;
    if (!nextTokenIs(b, "", PPELSE, PPELSEIF,
      PPEND, PPERROR, PPIF, CONDITIONAL_STATEMENT_ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PPIF);
    if (!r) r = consumeToken(b, PPELSE);
    if (!r) r = consumeToken(b, PPELSEIF);
    if (!r) r = consumeToken(b, PPEND);
    if (!r) r = consumeToken(b, PPERROR);
    if (!r) r = ppConditionalStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ('-' | '--' | '++' | '!' | '~') prefixExpression | suffixExpressionWrapper
  public static boolean prefixExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "prefixExpression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, "<prefix expression>");
    r = prefixExpression_0(b, l + 1);
    if (!r) r = suffixExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, PREFIX_EXPRESSION, r, false, null);
    return r;
  }

  // ('-' | '--' | '++' | '!' | '~') prefixExpression
  private static boolean prefixExpression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "prefixExpression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = prefixExpression_0_0(b, l + 1);
    r = r && prefixExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '-' | '--' | '++' | '!' | '~'
  private static boolean prefixExpression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "prefixExpression_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OMINUS);
    if (!r) r = consumeToken(b, OMINUS_MINUS);
    if (!r) r = consumeToken(b, OPLUS_PLUS);
    if (!r) r = consumeToken(b, ONOT);
    if (!r) r = consumeToken(b, OCOMPLEMENT);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // privateKeyWord externKeyWord
  static boolean privateExtern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "privateExtern")) return false;
    if (!nextTokenIs(b, KPRIVATE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = privateKeyWord(b, l + 1);
    r = r && externKeyWord(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'private'
  public static boolean privateKeyWord(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "privateKeyWord")) return false;
    if (!nextTokenIs(b, KPRIVATE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KPRIVATE);
    exit_section_(b, m, PRIVATE_KEY_WORD, r);
    return r;
  }

  /* ********************************************************** */
  // 'null' | 'default' | 'dynamic' | 'never' | 'get' | 'set' | referenceExpression
  public static boolean propertyAccessor(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "propertyAccessor")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<property accessor>");
    r = consumeToken(b, KNULL);
    if (!r) r = consumeToken(b, KDEFAULT);
    if (!r) r = consumeToken(b, KDYNAMIC);
    if (!r) r = consumeToken(b, KNEVER);
    if (!r) r = consumeToken(b, "get");
    if (!r) r = consumeToken(b, "set");
    if (!r) r = referenceExpression(b, l + 1);
    exit_section_(b, l, m, PROPERTY_ACCESSOR, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '(' propertyAccessor ',' propertyAccessor ')'
  public static boolean propertyDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "propertyDeclaration")) return false;
    if (!nextTokenIs(b, PLPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PLPAREN);
    r = r && propertyAccessor(b, l + 1);
    r = r && consumeToken(b, OCOMMA);
    r = r && propertyAccessor(b, l + 1);
    r = r && consumeToken(b, PRPAREN);
    exit_section_(b, m, PROPERTY_DECLARATION, r);
    return r;
  }

  /* ********************************************************** */
  // '@:protected'
  public static boolean protectedMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "protectedMeta")) return false;
    if (!nextTokenIs(b, KPROTECTED)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KPROTECTED);
    exit_section_(b, m, PROTECTED_META, r);
    return r;
  }

  /* ********************************************************** */
  // '.' referenceExpression
  public static boolean qualifiedReferenceExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedReferenceExpression")) return false;
    if (!nextTokenIs(b, ODOT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, ODOT);
    p = r; // pin = 1
    r = r && referenceExpression(b, l + 1);
    exit_section_(b, l, m, REFERENCE_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '.' referenceExpression
  public static boolean qualifiedReferenceExpressionOrWildcard(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedReferenceExpressionOrWildcard")) return false;
    if (!nextTokenIs(b, ODOT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, ODOT);
    r = r && referenceExpression(b, l + 1);
    exit_section_(b, l, m, REFERENCE_EXPRESSION, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // qualifiedReferenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  static boolean qualifiedReferenceTail(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedReferenceTail")) return false;
    if (!nextTokenIs(b, ODOT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = qualifiedReferenceExpression(b, l + 1);
    r = r && qualifiedReferenceTail_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean qualifiedReferenceTail_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedReferenceTail_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!qualifiedReferenceTail_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "qualifiedReferenceTail_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean qualifiedReferenceTail_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "qualifiedReferenceTail_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = callExpression(b, l + 1);
    if (!r) r = arrayAccessExpression(b, l + 1);
    if (!r) r = qualifiedReferenceExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // identifier
  public static boolean referenceExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "referenceExpression")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = identifier(b, l + 1);
    exit_section_(b, m, REFERENCE_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // REG_EXP
  public static boolean regularExpressionLiteral(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "regularExpressionLiteral")) return false;
    if (!nextTokenIs(b, REG_EXP)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, REG_EXP);
    exit_section_(b, m, REGULAR_EXPRESSION_LITERAL, r);
    return r;
  }

  /* ********************************************************** */
  // '@:require' '(' identifier ')'
  public static boolean requireMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "requireMeta")) return false;
    if (!nextTokenIs(b, KREQUIRE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KREQUIRE);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, identifier(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, REQUIRE_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // 'return' (objectLiteral | ("macro"? blockStatement) | expression )? ';'?
  public static boolean returnStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "returnStatement")) return false;
    if (!nextTokenIs(b, KRETURN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KRETURN);
    p = r; // pin = 1
    r = r && report_error_(b, returnStatement_1(b, l + 1));
    r = p && returnStatement_2(b, l + 1) && r;
    exit_section_(b, l, m, RETURN_STATEMENT, r, p, null);
    return r || p;
  }

  // (objectLiteral | ("macro"? blockStatement) | expression )?
  private static boolean returnStatement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "returnStatement_1")) return false;
    returnStatement_1_0(b, l + 1);
    return true;
  }

  // objectLiteral | ("macro"? blockStatement) | expression
  private static boolean returnStatement_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "returnStatement_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = objectLiteral(b, l + 1);
    if (!r) r = returnStatement_1_0_1(b, l + 1);
    if (!r) r = expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // "macro"? blockStatement
  private static boolean returnStatement_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "returnStatement_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = returnStatement_1_0_1_0(b, l + 1);
    r = r && blockStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // "macro"?
  private static boolean returnStatement_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "returnStatement_1_0_1_0")) return false;
    consumeToken(b, KMACRO2);
    return true;
  }

  // ';'?
  private static boolean returnStatement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "returnStatement_2")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // 'return' expression
  public static boolean returnStatementWithoutSemicolon(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "returnStatementWithoutSemicolon")) return false;
    if (!nextTokenIs(b, KRETURN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KRETURN);
    r = r && expression(b, l + 1);
    exit_section_(b, m, RETURN_STATEMENT_WITHOUT_SEMICOLON, r);
    return r;
  }

  /* ********************************************************** */
  // '@:setter' '(' referenceExpression ')'
  public static boolean setterMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "setterMeta")) return false;
    if (!nextTokenIs(b, KSETTER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KSETTER);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, referenceExpression(b, l + 1)) && r;
    r = p && consumeToken(b, PRPAREN) && r;
    exit_section_(b, l, m, SETTER_META, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // shiftOperator additiveExpressionWrapper
  public static boolean shiftExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shiftExpression")) return false;
    if (!nextTokenIs(b, "<shift expression>", OSHIFT_LEFT, OGREATER)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, "<shift expression>");
    r = shiftOperator(b, l + 1);
    p = r; // pin = 1
    r = r && additiveExpressionWrapper(b, l + 1);
    exit_section_(b, l, m, SHIFT_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // additiveExpressionWrapper shiftExpression*
  static boolean shiftExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shiftExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = additiveExpressionWrapper(b, l + 1);
    r = r && shiftExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // shiftExpression*
  private static boolean shiftExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shiftExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!shiftExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "shiftExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // unsignedShiftRightOperator | shiftRightOperator | '<<'
  public static boolean shiftOperator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shiftOperator")) return false;
    if (!nextTokenIs(b, "<shift operator>", OSHIFT_LEFT, OGREATER)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<shift operator>");
    r = unsignedShiftRightOperator(b, l + 1);
    if (!r) r = shiftRightOperator(b, l + 1);
    if (!r) r = consumeToken(b, OSHIFT_LEFT);
    exit_section_(b, l, m, SHIFT_OPERATOR, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '>' '>'
  public static boolean shiftRightOperator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shiftRightOperator")) return false;
    if (!nextTokenIs(b, OGREATER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OGREATER);
    r = r && consumeToken(b, OGREATER);
    exit_section_(b, m, SHIFT_RIGHT_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // SHORT_TEMPLATE_ENTRY_START (thisExpression | referenceExpression)
  public static boolean shortTemplateEntry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shortTemplateEntry")) return false;
    if (!nextTokenIs(b, SHORT_TEMPLATE_ENTRY_START)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, SHORT_TEMPLATE_ENTRY_START);
    p = r; // pin = 1
    r = r && shortTemplateEntry_1(b, l + 1);
    exit_section_(b, l, m, SHORT_TEMPLATE_ENTRY, r, p, null);
    return r || p;
  }

  // thisExpression | referenceExpression
  private static boolean shortTemplateEntry_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "shortTemplateEntry_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = thisExpression(b, l + 1);
    if (!r) r = referenceExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // anonymousTypeFieldList (',' interfaceBody)?
  static boolean simpleAnonymousTypeBody(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleAnonymousTypeBody")) return false;
    if (!nextTokenIs(b, "", OQUEST, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = anonymousTypeFieldList(b, l + 1);
    r = r && simpleAnonymousTypeBody_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' interfaceBody)?
  private static boolean simpleAnonymousTypeBody_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleAnonymousTypeBody_1")) return false;
    simpleAnonymousTypeBody_1_0(b, l + 1);
    return true;
  }

  // ',' interfaceBody
  private static boolean simpleAnonymousTypeBody_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleAnonymousTypeBody_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && interfaceBody(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // unreflectiveMeta | finalMeta | keepMeta | coreApiMeta | bindMeta | macroMeta | hackMeta
  public static boolean simpleMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleMeta")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<simple meta>");
    r = unreflectiveMeta(b, l + 1);
    if (!r) r = finalMeta(b, l + 1);
    if (!r) r = keepMeta(b, l + 1);
    if (!r) r = coreApiMeta(b, l + 1);
    if (!r) r = bindMeta(b, l + 1);
    if (!r) r = macroMeta(b, l + 1);
    if (!r) r = hackMeta(b, l + 1);
    exit_section_(b, l, m, SIMPLE_META, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpression *
  public static boolean simpleQualifiedReferenceExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleQualifiedReferenceExpression")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _COLLAPSE_, null);
    r = referenceExpression(b, l + 1);
    p = r; // pin = 1
    r = r && simpleQualifiedReferenceExpression_1(b, l + 1);
    exit_section_(b, l, m, REFERENCE_EXPRESSION, r, p, null);
    return r || p;
  }

  // qualifiedReferenceExpression *
  private static boolean simpleQualifiedReferenceExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleQualifiedReferenceExpression_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!qualifiedReferenceExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "simpleQualifiedReferenceExpression_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpressionOrWildcard * wildcard
  public static boolean simpleQualifiedReferenceExpressionWithWildcardSupport(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleQualifiedReferenceExpressionWithWildcardSupport")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = referenceExpression(b, l + 1);
    r = r && simpleQualifiedReferenceExpressionWithWildcardSupport_1(b, l + 1);
    r = r && wildcard(b, l + 1);
    exit_section_(b, m, REFERENCE_EXPRESSION, r);
    return r;
  }

  // qualifiedReferenceExpressionOrWildcard *
  private static boolean simpleQualifiedReferenceExpressionWithWildcardSupport_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simpleQualifiedReferenceExpressionWithWildcardSupport_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!qualifiedReferenceExpressionOrWildcard(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "simpleQualifiedReferenceExpressionWithWildcardSupport_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // macroClassList? (blockStatement | notBlockStatement)
  static boolean statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement_0(b, l + 1);
    r = r && statement_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // macroClassList?
  private static boolean statement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // blockStatement | notBlockStatement
  private static boolean statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = blockStatement(b, l + 1);
    if (!r) r = notBlockStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (statement ';'?)+
  static boolean statementList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statementList")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = statementList_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!statementList_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "statementList", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, null, r, false, statement_recovery_parser_);
    return r;
  }

  // statement ';'?
  private static boolean statementList_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statementList_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    r = r && statementList_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean statementList_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statementList_0_1")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // !('case' | 'default' | '}')
  static boolean statement_recovery(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_recovery")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !statement_recovery_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // 'case' | 'default' | '}'
  private static boolean statement_recovery_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_recovery_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KCASE);
    if (!r) r = consumeToken(b, KDEFAULT);
    if (!r) r = consumeToken(b, PRCURLY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // OPEN_QUOTE (REGULAR_STRING_PART | shortTemplateEntry | longTemplateEntry)* CLOSING_QUOTE
  public static boolean stringLiteralExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringLiteralExpression")) return false;
    if (!nextTokenIs(b, OPEN_QUOTE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, OPEN_QUOTE);
    p = r; // pin = 1
    r = r && report_error_(b, stringLiteralExpression_1(b, l + 1));
    r = p && consumeToken(b, CLOSING_QUOTE) && r;
    exit_section_(b, l, m, STRING_LITERAL_EXPRESSION, r, p, null);
    return r || p;
  }

  // (REGULAR_STRING_PART | shortTemplateEntry | longTemplateEntry)*
  private static boolean stringLiteralExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringLiteralExpression_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!stringLiteralExpression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "stringLiteralExpression_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // REGULAR_STRING_PART | shortTemplateEntry | longTemplateEntry
  private static boolean stringLiteralExpression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringLiteralExpression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, REGULAR_STRING_PART);
    if (!r) r = shortTemplateEntry(b, l + 1);
    if (!r) r = longTemplateEntry(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '--' | '++'
  public static boolean suffixExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "suffixExpression")) return false;
    if (!nextTokenIs(b, "<suffix expression>", OPLUS_PLUS, OMINUS_MINUS)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, "<suffix expression>");
    r = consumeToken(b, OMINUS_MINUS);
    if (!r) r = consumeToken(b, OPLUS_PLUS);
    exit_section_(b, l, m, SUFFIX_EXPRESSION, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // value suffixExpression*
  static boolean suffixExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "suffixExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value(b, l + 1);
    r = r && suffixExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // suffixExpression*
  private static boolean suffixExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "suffixExpressionWrapper_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!suffixExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "suffixExpressionWrapper_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // 'super'
  public static boolean superExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "superExpression")) return false;
    if (!nextTokenIs(b, KSUPER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KSUPER);
    exit_section_(b, m, SUPER_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // '{'  switchCase* defaultCase? '}'
  public static boolean switchBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchBlock")) return false;
    if (!nextTokenIs(b, PLCURLY)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, PLCURLY);
    p = r; // pin = 1
    r = r && report_error_(b, switchBlock_1(b, l + 1));
    r = p && report_error_(b, switchBlock_2(b, l + 1)) && r;
    r = p && consumeToken(b, PRCURLY) && r;
    exit_section_(b, l, m, SWITCH_BLOCK, r, p, null);
    return r || p;
  }

  // switchCase*
  private static boolean switchBlock_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchBlock_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!switchCase(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "switchBlock_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // defaultCase?
  private static boolean switchBlock_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchBlock_2")) return false;
    defaultCase(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // ('case' switchCaseExpression (',' switchCaseExpression)* ':')+ switchCaseBlock?
  public static boolean switchCase(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCase")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<switch case>");
    r = switchCase_0(b, l + 1);
    p = r; // pin = 1
    r = r && switchCase_1(b, l + 1);
    exit_section_(b, l, m, SWITCH_CASE, r, p, switch_case_recover_parser_);
    return r || p;
  }

  // ('case' switchCaseExpression (',' switchCaseExpression)* ':')+
  private static boolean switchCase_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCase_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = switchCase_0_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!switchCase_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "switchCase_0", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // 'case' switchCaseExpression (',' switchCaseExpression)* ':'
  private static boolean switchCase_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCase_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KCASE);
    r = r && switchCaseExpression(b, l + 1);
    r = r && switchCase_0_0_2(b, l + 1);
    r = r && consumeToken(b, OCOLON);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' switchCaseExpression)*
  private static boolean switchCase_0_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCase_0_0_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!switchCase_0_0_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "switchCase_0_0_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' switchCaseExpression
  private static boolean switchCase_0_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCase_0_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && switchCaseExpression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // switchCaseBlock?
  private static boolean switchCase_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCase_1")) return false;
    switchCaseBlock(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (literalExpression ";") | statementList
  public static boolean switchCaseBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCaseBlock")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<switch case block>");
    r = switchCaseBlock_0(b, l + 1);
    if (!r) r = statementList(b, l + 1);
    exit_section_(b, l, m, SWITCH_CASE_BLOCK, r, false, null);
    return r;
  }

  // literalExpression ";"
  private static boolean switchCaseBlock_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCaseBlock_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = literalExpression(b, l + 1);
    r = r && consumeToken(b, OSEMI);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expression ('if' '(' expression ')')?
  public static boolean switchCaseExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCaseExpression")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _COLLAPSE_, "<switch case expression>");
    r = expression(b, l + 1);
    p = r; // pin = 1
    r = r && switchCaseExpression_1(b, l + 1);
    exit_section_(b, l, m, SWITCH_CASE_EXPRESSION, r, p, null);
    return r || p;
  }

  // ('if' '(' expression ')')?
  private static boolean switchCaseExpression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCaseExpression_1")) return false;
    switchCaseExpression_1_0(b, l + 1);
    return true;
  }

  // 'if' '(' expression ')'
  private static boolean switchCaseExpression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchCaseExpression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KIF);
    r = r && consumeToken(b, PLPAREN);
    r = r && expression(b, l + 1);
    r = r && consumeToken(b, PRPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'switch' expression switchBlock
  public static boolean switchStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switchStatement")) return false;
    if (!nextTokenIs(b, KSWITCH)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KSWITCH);
    p = r; // pin = 1
    r = r && report_error_(b, expression(b, l + 1));
    r = p && switchBlock(b, l + 1) && r;
    exit_section_(b, l, m, SWITCH_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // !('case' | 'default' | '}' | ID)
  static boolean switch_case_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switch_case_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !switch_case_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // 'case' | 'default' | '}' | ID
  private static boolean switch_case_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "switch_case_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KCASE);
    if (!r) r = consumeToken(b, KDEFAULT);
    if (!r) r = consumeToken(b, PRCURLY);
    if (!r) r = consumeToken(b, ID);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '?' expression ':' ternaryExpressionWrapper
  public static boolean ternaryExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ternaryExpression")) return false;
    if (!nextTokenIs(b, OQUEST)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, OQUEST);
    p = r; // pin = 1
    r = r && report_error_(b, expression(b, l + 1));
    r = p && report_error_(b, consumeToken(b, OCOLON)) && r;
    r = p && ternaryExpressionWrapper(b, l + 1) && r;
    exit_section_(b, l, m, TERNARY_EXPRESSION, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // logicOrExpressionWrapper ternaryExpression?
  static boolean ternaryExpressionWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ternaryExpressionWrapper")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = logicOrExpressionWrapper(b, l + 1);
    r = r && ternaryExpressionWrapper_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ternaryExpression?
  private static boolean ternaryExpressionWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "ternaryExpressionWrapper_1")) return false;
    ternaryExpression(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // 'this'
  public static boolean thisExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "thisExpression")) return false;
    if (!nextTokenIs(b, KTHIS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KTHIS);
    exit_section_(b, m, THIS_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // 'throw' expression ';'?
  public static boolean throwStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "throwStatement")) return false;
    if (!nextTokenIs(b, KTHROW)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KTHROW);
    p = r; // pin = 1
    r = r && report_error_(b, expression(b, l + 1));
    r = p && throwStatement_2(b, l + 1) && r;
    exit_section_(b, l, m, THROW_STATEMENT, r, p, null);
    return r || p;
  }

  // ';'?
  private static boolean throwStatement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "throwStatement_2")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // importStatementAll | usingStatement | topLevelDeclaration
  static boolean topLevel(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "topLevel")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = importStatementAll(b, l + 1);
    if (!r) r = usingStatement(b, l + 1);
    if (!r) r = topLevelDeclaration(b, l + 1);
    exit_section_(b, l, m, null, r, false, top_level_recover_parser_);
    return r;
  }

  /* ********************************************************** */
  // classDeclaration
  //                               | interfaceDeclaration
  //                               | externClassDeclaration
  //                               | externInterfaceDeclaration
  //                               | abstractClassDeclaration
  //                               | enumDeclaration
  //                               | typedefDeclaration
  static boolean topLevelDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "topLevelDeclaration")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = classDeclaration(b, l + 1);
    if (!r) r = interfaceDeclaration(b, l + 1);
    if (!r) r = externClassDeclaration(b, l + 1);
    if (!r) r = externInterfaceDeclaration(b, l + 1);
    if (!r) r = abstractClassDeclaration(b, l + 1);
    if (!r) r = enumDeclaration(b, l + 1);
    if (!r) r = typedefDeclaration(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // topLevel*
  static boolean topLevelList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "topLevelList")) return false;
    int c = current_position_(b);
    while (true) {
      if (!topLevel(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "topLevelList", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // !(ppToken | metaKeyWord | 'abstract' | 'class'  | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef')
  static boolean top_level_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !top_level_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ppToken | metaKeyWord | 'abstract' | 'class'  | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef'
  private static boolean top_level_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = ppToken(b, l + 1);
    if (!r) r = metaKeyWord(b, l + 1);
    if (!r) r = consumeToken(b, KABSTRACT);
    if (!r) r = consumeToken(b, KCLASS);
    if (!r) r = consumeToken(b, KENUM);
    if (!r) r = consumeToken(b, KEXTERN);
    if (!r) r = consumeToken(b, KIMPORT);
    if (!r) r = consumeToken(b, KUSING);
    if (!r) r = consumeToken(b, KINTERFACE);
    if (!r) r = consumeToken(b, KPRIVATE);
    if (!r) r = consumeToken(b, KTYPEDEF);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'try' statement ';'? catchStatement*
  public static boolean tryStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tryStatement")) return false;
    if (!nextTokenIs(b, KTRY)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KTRY);
    p = r; // pin = 1
    r = r && report_error_(b, statement(b, l + 1));
    r = p && report_error_(b, tryStatement_2(b, l + 1)) && r;
    r = p && tryStatement_3(b, l + 1) && r;
    exit_section_(b, l, m, TRY_STATEMENT, r, p, null);
    return r || p;
  }

  // ';'?
  private static boolean tryStatement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tryStatement_2")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  // catchStatement*
  private static boolean tryStatement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tryStatement_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!catchStatement(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "tryStatement_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpression* typeParam?
  public static boolean type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type")) return false;
    if (!nextTokenIs(b, ID)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = referenceExpression(b, l + 1);
    r = r && type_1(b, l + 1);
    r = r && type_2(b, l + 1);
    exit_section_(b, m, TYPE, r);
    return r;
  }

  // qualifiedReferenceExpression*
  private static boolean type_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!qualifiedReferenceExpression(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "type_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // typeParam?
  private static boolean type_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_2")) return false;
    typeParam(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '>' type
  public static boolean typeExtends(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeExtends")) return false;
    if (!nextTokenIs(b, OGREATER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OGREATER);
    r = r && type(b, l + 1);
    exit_section_(b, m, TYPE_EXTENDS, r);
    return r;
  }

  /* ********************************************************** */
  // typeListPart (',' typeListPart)*
  public static boolean typeList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeList")) return false;
    if (!nextTokenIs(b, "<type list>", PLPAREN, PLCURLY, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<type list>");
    r = typeListPart(b, l + 1);
    r = r && typeList_1(b, l + 1);
    exit_section_(b, l, m, TYPE_LIST, r, false, null);
    return r;
  }

  // (',' typeListPart)*
  private static boolean typeList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeList_1")) return false;
    int c = current_position_(b);
    while (true) {
      if (!typeList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "typeList_1", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // ',' typeListPart
  private static boolean typeList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeList_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOMMA);
    r = r && typeListPart(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // functionTypeWrapper
  public static boolean typeListPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeListPart")) return false;
    if (!nextTokenIs(b, "<type list part>", PLPAREN, PLCURLY, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<type list part>");
    r = functionTypeWrapper(b, l + 1);
    exit_section_(b, l, m, TYPE_LIST_PART, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // type | anonymousType
  public static boolean typeOrAnonymous(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeOrAnonymous")) return false;
    if (!nextTokenIs(b, "<type or anonymous>", PLCURLY, ID)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<type or anonymous>");
    r = type(b, l + 1);
    if (!r) r = anonymousType(b, l + 1);
    exit_section_(b, l, m, TYPE_OR_ANONYMOUS, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '<' typeList '>'
  public static boolean typeParam(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeParam")) return false;
    if (!nextTokenIs(b, OLESS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OLESS);
    r = r && typeList(b, l + 1);
    r = r && consumeToken(b, OGREATER);
    exit_section_(b, m, TYPE_PARAM, r);
    return r;
  }

  /* ********************************************************** */
  // ':' functionTypeWrapper
  public static boolean typeTag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typeTag")) return false;
    if (!nextTokenIs(b, OCOLON)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OCOLON);
    r = r && functionTypeWrapper(b, l + 1);
    exit_section_(b, m, TYPE_TAG, r);
    return r;
  }

  /* ********************************************************** */
  // macroClassList? externOrPrivate? 'typedef' componentName genericParam? '=' functionTypeWrapper ';'?
  public static boolean typedefDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typedefDeclaration")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<typedef declaration>");
    r = typedefDeclaration_0(b, l + 1);
    r = r && typedefDeclaration_1(b, l + 1);
    r = r && consumeToken(b, KTYPEDEF);
    r = r && componentName(b, l + 1);
    r = r && typedefDeclaration_4(b, l + 1);
    p = r; // pin = 5
    r = r && report_error_(b, consumeToken(b, OASSIGN));
    r = p && report_error_(b, functionTypeWrapper(b, l + 1)) && r;
    r = p && typedefDeclaration_7(b, l + 1) && r;
    exit_section_(b, l, m, TYPEDEF_DECLARATION, r, p, null);
    return r || p;
  }

  // macroClassList?
  private static boolean typedefDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typedefDeclaration_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // externOrPrivate?
  private static boolean typedefDeclaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typedefDeclaration_1")) return false;
    externOrPrivate(b, l + 1);
    return true;
  }

  // genericParam?
  private static boolean typedefDeclaration_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typedefDeclaration_4")) return false;
    genericParam(b, l + 1);
    return true;
  }

  // ';'?
  private static boolean typedefDeclaration_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typedefDeclaration_7")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // '@:unreflective'
  public static boolean unreflectiveMeta(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unreflectiveMeta")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<unreflective meta>");
    r = consumeToken(b, "@:unreflective");
    exit_section_(b, l, m, UNREFLECTIVE_META, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '>' '>' '>'
  public static boolean unsignedShiftRightOperator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unsignedShiftRightOperator")) return false;
    if (!nextTokenIs(b, OGREATER)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OGREATER);
    r = r && consumeToken(b, OGREATER);
    r = r && consumeToken(b, OGREATER);
    exit_section_(b, m, UNSIGNED_SHIFT_RIGHT_OPERATOR, r);
    return r;
  }

  /* ********************************************************** */
  // 'using' simpleQualifiedReferenceExpression ';'
  public static boolean usingStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "usingStatement")) return false;
    if (!nextTokenIs(b, KUSING)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KUSING);
    p = r; // pin = 1
    r = r && report_error_(b, simpleQualifiedReferenceExpression(b, l + 1));
    r = p && consumeToken(b, OSEMI) && r;
    exit_section_(b, l, m, USING_STATEMENT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // macroClassList? ('untyped' expression
  //                                                    | 'macro' expression
  //                                                    | (literalExpression qualifiedReferenceTail?)
  //                                                    | ifStatement
  //                                                    | castExpression qualifiedReferenceTail?
  //                                                    | newExpressionOrCall
  //                                                    | parenthesizedExpressionOrCall
  //                                                    | callOrArrayAccess
  //                                                    | tryStatement
  //                                                    | switchStatement)
  static boolean value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value_0(b, l + 1);
    r = r && value_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // macroClassList?
  private static boolean value_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_0")) return false;
    macroClassList(b, l + 1);
    return true;
  }

  // 'untyped' expression
  //                                                    | 'macro' expression
  //                                                    | (literalExpression qualifiedReferenceTail?)
  //                                                    | ifStatement
  //                                                    | castExpression qualifiedReferenceTail?
  //                                                    | newExpressionOrCall
  //                                                    | parenthesizedExpressionOrCall
  //                                                    | callOrArrayAccess
  //                                                    | tryStatement
  //                                                    | switchStatement
  private static boolean value_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value_1_0(b, l + 1);
    if (!r) r = value_1_1(b, l + 1);
    if (!r) r = value_1_2(b, l + 1);
    if (!r) r = ifStatement(b, l + 1);
    if (!r) r = value_1_4(b, l + 1);
    if (!r) r = newExpressionOrCall(b, l + 1);
    if (!r) r = parenthesizedExpressionOrCall(b, l + 1);
    if (!r) r = callOrArrayAccess(b, l + 1);
    if (!r) r = tryStatement(b, l + 1);
    if (!r) r = switchStatement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // 'untyped' expression
  private static boolean value_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KUNTYPED);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // 'macro' expression
  private static boolean value_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KMACRO2);
    r = r && expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // literalExpression qualifiedReferenceTail?
  private static boolean value_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = literalExpression(b, l + 1);
    r = r && value_1_2_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // qualifiedReferenceTail?
  private static boolean value_1_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_1_2_1")) return false;
    qualifiedReferenceTail(b, l + 1);
    return true;
  }

  // castExpression qualifiedReferenceTail?
  private static boolean value_1_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_1_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = castExpression(b, l + 1);
    r = r && value_1_4_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // qualifiedReferenceTail?
  private static boolean value_1_4_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value_1_4_1")) return false;
    qualifiedReferenceTail(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (macroMember | declarationAttribute)* 'var' varDeclarationPartList ';'
  public static boolean varDeclaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varDeclaration")) return false;
    if (!nextTokenIs(b, "<var declaration>", KAUTOBUILD, KBUILD,
      KDEBUG, KGETTER, KKEEP, KMACRO, KMETA, KNATIVE,
      KNODEBUG, KNS, KPROTECTED, KREQUIRE, KSETTER, KDYNAMIC,
      KINLINE, KMACRO2, KOVERRIDE, KPRIVATE, KPUBLIC, KSTATIC, KVAR, MACRO_ID)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<var declaration>");
    r = varDeclaration_0(b, l + 1);
    r = r && consumeToken(b, KVAR);
    r = r && varDeclarationPartList(b, l + 1);
    p = r; // pin = 3
    r = r && consumeToken(b, OSEMI);
    exit_section_(b, l, m, VAR_DECLARATION, r, p, null);
    return r || p;
  }

  // (macroMember | declarationAttribute)*
  private static boolean varDeclaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varDeclaration_0")) return false;
    int c = current_position_(b);
    while (true) {
      if (!varDeclaration_0_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "varDeclaration_0", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // macroMember | declarationAttribute
  private static boolean varDeclaration_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varDeclaration_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = macroMember(b, l + 1);
    if (!r) r = declarationAttribute(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // componentName propertyDeclaration? typeTag? varInit?
  public static boolean varDeclarationPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varDeclarationPart")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<var declaration part>");
    r = componentName(b, l + 1);
    r = r && varDeclarationPart_1(b, l + 1);
    r = r && varDeclarationPart_2(b, l + 1);
    r = r && varDeclarationPart_3(b, l + 1);
    exit_section_(b, l, m, VAR_DECLARATION_PART, r, false, var_declaration_part_recover_parser_);
    return r;
  }

  // propertyDeclaration?
  private static boolean varDeclarationPart_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varDeclarationPart_1")) return false;
    propertyDeclaration(b, l + 1);
    return true;
  }

  // typeTag?
  private static boolean varDeclarationPart_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varDeclarationPart_2")) return false;
    typeTag(b, l + 1);
    return true;
  }

  // varInit?
  private static boolean varDeclarationPart_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varDeclarationPart_3")) return false;
    varInit(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // varDeclarationPart
  static boolean varDeclarationPartList(PsiBuilder b, int l) {
    return varDeclarationPart(b, l + 1);
  }

  /* ********************************************************** */
  // '=' expression
  public static boolean varInit(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varInit")) return false;
    if (!nextTokenIs(b, OASSIGN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, OASSIGN);
    p = r; // pin = 1
    r = r && expression(b, l + 1);
    exit_section_(b, l, m, VAR_INIT, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // !(';' | ',')
  static boolean var_declaration_part_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_declaration_part_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !var_declaration_part_recover_0(b, l + 1);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // ';' | ','
  private static boolean var_declaration_part_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "var_declaration_part_recover_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OSEMI);
    if (!r) r = consumeToken(b, OCOMMA);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // 'while' '(' expression ')' statement ';'?
  public static boolean whileStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "whileStatement")) return false;
    if (!nextTokenIs(b, KWHILE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = consumeToken(b, KWHILE);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, PLPAREN));
    r = p && report_error_(b, expression(b, l + 1)) && r;
    r = p && report_error_(b, consumeToken(b, PRPAREN)) && r;
    r = p && report_error_(b, statement(b, l + 1)) && r;
    r = p && whileStatement_5(b, l + 1) && r;
    exit_section_(b, l, m, WHILE_STATEMENT, r, p, null);
    return r || p;
  }

  // ';'?
  private static boolean whileStatement_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "whileStatement_5")) return false;
    consumeToken(b, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // '.' '*'
  public static boolean wildcard(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "wildcard")) return false;
    if (!nextTokenIs(b, ODOT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _LEFT_, null);
    r = consumeToken(b, ODOT);
    r = r && consumeToken(b, OMUL);
    exit_section_(b, l, m, WILDCARD, r, false, null);
    return r;
  }

  final static Parser class_body_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return class_body_part_recover(b, l + 1);
    }
  };
  final static Parser enum_value_declaration_recovery_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return enum_value_declaration_recovery(b, l + 1);
    }
  };
  final static Parser expression_list_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return expression_list_recover(b, l + 1);
    }
  };
  final static Parser expression_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return expression_recover(b, l + 1);
    }
  };
  final static Parser extern_class_body_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return extern_class_body_part_recover(b, l + 1);
    }
  };
  final static Parser inherit_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return inherit_recover(b, l + 1);
    }
  };
  final static Parser interface_body_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return interface_body_part_recover(b, l + 1);
    }
  };
  final static Parser local_var_declaration_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return local_var_declaration_part_recover(b, l + 1);
    }
  };
  final static Parser object_literal_list_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return object_literal_list_recover(b, l + 1);
    }
  };
  final static Parser object_literal_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return object_literal_part_recover(b, l + 1);
    }
  };
  final static Parser parameterListRecovery_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return parameterListRecovery(b, l + 1);
    }
  };
  final static Parser statement_recovery_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return statement_recovery(b, l + 1);
    }
  };
  final static Parser switch_case_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return switch_case_recover(b, l + 1);
    }
  };
  final static Parser top_level_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return top_level_recover(b, l + 1);
    }
  };
  final static Parser var_declaration_part_recover_parser_ = new Parser() {
    public boolean parse(PsiBuilder b, int l) {
      return var_declaration_part_recover(b, l + 1);
    }
  };
}
