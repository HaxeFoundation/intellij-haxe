// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.parser;

import org.jetbrains.annotations.*;
import com.intellij.lang.LighterASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.openapi.diagnostic.Logger;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.plugins.haxe.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HaxeParser implements PsiParser {

  public static Logger LOG_ = Logger.getInstance("com.intellij.plugins.haxe.lang.parser.HaxeParser");

  @NotNull
  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    int level_ = 0;
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this);
    if (root_ == HAXE_ACCESS) {
      result_ = access(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ADDITIVE_EXPRESSION) {
      result_ = additiveExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUS_FUNCTION_DECLARATION) {
      result_ = anonymousFunctionDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUS_TYPE) {
      result_ = anonymousType(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUS_TYPE_BODY) {
      result_ = anonymousTypeBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUS_TYPE_FIELD) {
      result_ = anonymousTypeField(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUS_TYPE_FIELD_LIST) {
      result_ = anonymousTypeFieldList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ARRAY_ACCESS_EXPRESSION) {
      result_ = arrayAccessExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ARRAY_LITERAL) {
      result_ = arrayLiteral(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ASSIGN_EXPRESSION) {
      result_ = assignExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ASSIGN_OPERATION) {
      result_ = assignOperation(builder_, level_ + 1);
    }
    else if (root_ == HAXE_AUTO_BUILD_MACRO) {
      result_ = autoBuildMacro(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BIT_OPERATION) {
      result_ = bitOperation(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BITMAP_META) {
      result_ = bitmapMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BITWISE_EXPRESSION) {
      result_ = bitwiseExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BLOCK_STATEMENT) {
      result_ = blockStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BREAK_STATEMENT) {
      result_ = breakStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BUILD_MACRO) {
      result_ = buildMacro(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CALL_EXPRESSION) {
      result_ = callExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CAST_EXPRESSION) {
      result_ = castExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CATCH_STATEMENT) {
      result_ = catchStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CLASS_BODY) {
      result_ = classBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CLASS_DECLARATION) {
      result_ = classDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_COMPARE_EXPRESSION) {
      result_ = compareExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_COMPARE_OPERATION) {
      result_ = compareOperation(builder_, level_ + 1);
    }
    else if (root_ == HAXE_COMPONENT_NAME) {
      result_ = componentName(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CONTINUE_STATEMENT) {
      result_ = continueStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CUSTOM_META) {
      result_ = customMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DECLARATION_ATTRIBUTE) {
      result_ = declarationAttribute(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DECLARATION_ATTRIBUTE_LIST) {
      result_ = declarationAttributeList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DEFAULT_CASE) {
      result_ = defaultCase(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DO_WHILE_STATEMENT) {
      result_ = doWhileStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUM_BODY) {
      result_ = enumBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUM_CONSTRUCTOR_PARAMETERS) {
      result_ = enumConstructorParameters(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUM_DECLARATION) {
      result_ = enumDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUM_VALUE_DECLARATION) {
      result_ = enumValueDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXPRESSION) {
      result_ = expression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXPRESSION_LIST) {
      result_ = expressionList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERN_CLASS_DECLARATION) {
      result_ = externClassDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERN_CLASS_DECLARATION_BODY) {
      result_ = externClassDeclarationBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERN_FUNCTION_DECLARATION) {
      result_ = externFunctionDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERN_OR_PRIVATE) {
      result_ = externOrPrivate(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FAKE_ENUM_META) {
      result_ = fakeEnumMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FOR_STATEMENT) {
      result_ = forStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTION_DECLARATION_WITH_ATTRIBUTES) {
      result_ = functionDeclarationWithAttributes(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTION_LITERAL) {
      result_ = functionLiteral(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES) {
      result_ = functionPrototypeDeclarationWithAttributes(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTION_TYPE) {
      result_ = functionType(builder_, level_ + 1);
    }
    else if (root_ == HAXE_GENERIC_LIST_PART) {
      result_ = genericListPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_GENERIC_PARAM) {
      result_ = genericParam(builder_, level_ + 1);
    }
    else if (root_ == HAXE_GETTER_META) {
      result_ = getterMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_IDENTIFIER) {
      result_ = identifier(builder_, level_ + 1);
    }
    else if (root_ == HAXE_IF_STATEMENT) {
      result_ = ifStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_IMPORT_STATEMENT) {
      result_ = importStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INHERIT) {
      result_ = inherit(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INHERIT_LIST) {
      result_ = inheritList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INTERFACE_BODY) {
      result_ = interfaceBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INTERFACE_DECLARATION) {
      result_ = interfaceDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ITERABLE) {
      result_ = iterable(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ITERATOR_EXPRESSION) {
      result_ = iteratorExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LITERAL_EXPRESSION) {
      result_ = literalExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOCAL_FUNCTION_DECLARATION) {
      result_ = localFunctionDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOCAL_VAR_DECLARATION) {
      result_ = localVarDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOCAL_VAR_DECLARATION_PART) {
      result_ = localVarDeclarationPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOGIC_AND_EXPRESSION) {
      result_ = logicAndExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOGIC_OR_EXPRESSION) {
      result_ = logicOrExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_META_KEY_VALUE) {
      result_ = metaKeyValue(builder_, level_ + 1);
    }
    else if (root_ == HAXE_META_META) {
      result_ = metaMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_MULTIPLICATIVE_EXPRESSION) {
      result_ = multiplicativeExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_NATIVE_META) {
      result_ = nativeMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_NEW_EXPRESSION) {
      result_ = newExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_NS_META) {
      result_ = nsMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_OBJECT_LITERAL) {
      result_ = objectLiteral(builder_, level_ + 1);
    }
    else if (root_ == HAXE_OBJECT_LITERAL_ELEMENT) {
      result_ = objectLiteralElement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_OVERLOAD_META) {
      result_ = overloadMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PACKAGE_STATEMENT) {
      result_ = packageStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PARAMETER) {
      result_ = parameter(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PARAMETER_LIST) {
      result_ = parameterList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PARENTHESIZED_EXPRESSION) {
      result_ = parenthesizedExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PREFIX_EXPRESSION) {
      result_ = prefixExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PROPERTY_ACCESSOR) {
      result_ = propertyAccessor(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PROPERTY_DECLARATION) {
      result_ = propertyDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_REFERENCE_EXPRESSION) {
      result_ = referenceExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_REGULAR_EXPRESSION_LITERAL) {
      result_ = regularExpressionLiteral(builder_, level_ + 1);
    }
    else if (root_ == HAXE_REQUIRE_META) {
      result_ = requireMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_RETURN_STATEMENT) {
      result_ = returnStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_RETURN_STATEMENT_WITHOUT_SEMICOLON) {
      result_ = returnStatementWithoutSemicolon(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SETTER_META) {
      result_ = setterMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SHIFT_EXPRESSION) {
      result_ = shiftExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SHIFT_OPERATOR) {
      result_ = shiftOperator(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SHIFT_RIGHT_OPERATOR) {
      result_ = shiftRightOperator(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SUFFIX_EXPRESSION) {
      result_ = suffixExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SUPER_EXPRESSION) {
      result_ = superExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SWITCH_BLOCK) {
      result_ = switchBlock(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SWITCH_CASE) {
      result_ = switchCase(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SWITCH_CASE_BLOCK) {
      result_ = switchCaseBlock(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SWITCH_STATEMENT) {
      result_ = switchStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TERNARY_EXPRESSION) {
      result_ = ternaryExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_THIS_EXPRESSION) {
      result_ = thisExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_THROW_STATEMENT) {
      result_ = throwStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TRY_STATEMENT) {
      result_ = tryStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE) {
      result_ = type(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE_EXTENDS) {
      result_ = typeExtends(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE_LIST) {
      result_ = typeList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE_LIST_PART) {
      result_ = typeListPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE_OR_ANONYMOUS) {
      result_ = typeOrAnonymous(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE_PARAM) {
      result_ = typeParam(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE_TAG) {
      result_ = typeTag(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPEDEF_DECLARATION) {
      result_ = typedefDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_UNSIGNED_SHIFT_RIGHT_OPERATOR) {
      result_ = unsignedShiftRightOperator(builder_, level_ + 1);
    }
    else if (root_ == HAXE_USING_STATEMENT) {
      result_ = usingStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_VAR_DECLARATION) {
      result_ = varDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_VAR_DECLARATION_PART) {
      result_ = varDeclarationPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_VAR_INIT) {
      result_ = varInit(builder_, level_ + 1);
    }
    else if (root_ == HAXE_WHILE_STATEMENT) {
      result_ = whileStatement(builder_, level_ + 1);
    }
    else {
      Marker marker_ = builder_.mark();
      result_ = parse_root_(root_, builder_, level_);
      while (builder_.getTokenType() != null) {
        builder_.advanceLexer();
      }
      marker_.done(root_);
    }
    return builder_.getTreeBuilt();
  }

  protected boolean parse_root_(final IElementType root_, final PsiBuilder builder_, final int level_) {
    return haxeFile(builder_, level_ + 1);
  }

  private static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    TokenSet.create(HAXE_ADDITIVE_EXPRESSION, HAXE_ARRAY_ACCESS_EXPRESSION, HAXE_ARRAY_LITERAL, HAXE_ASSIGN_EXPRESSION,
      HAXE_BITWISE_EXPRESSION, HAXE_CALL_EXPRESSION, HAXE_CAST_EXPRESSION, HAXE_COMPARE_EXPRESSION,
      HAXE_EXPRESSION, HAXE_FUNCTION_LITERAL, HAXE_ITERATOR_EXPRESSION, HAXE_LITERAL_EXPRESSION,
      HAXE_LOGIC_AND_EXPRESSION, HAXE_LOGIC_OR_EXPRESSION, HAXE_MULTIPLICATIVE_EXPRESSION, HAXE_NEW_EXPRESSION,
      HAXE_OBJECT_LITERAL, HAXE_PARENTHESIZED_EXPRESSION, HAXE_PREFIX_EXPRESSION, HAXE_REFERENCE_EXPRESSION,
      HAXE_REGULAR_EXPRESSION_LITERAL, HAXE_SHIFT_EXPRESSION, HAXE_SUFFIX_EXPRESSION, HAXE_SUPER_EXPRESSION,
      HAXE_TERNARY_EXPRESSION, HAXE_THIS_EXPRESSION),
    TokenSet.create(HAXE_LITERAL_EXPRESSION, HAXE_REGULAR_EXPRESSION_LITERAL),
  };
  public static boolean type_extends_(IElementType child_, IElementType parent_) {
    for (TokenSet set : EXTENDS_SETS_) {
      if (set.contains(child_) && set.contains(parent_)) return true;
    }
    return false;
  }

  /* ********************************************************** */
  // 'public' | 'private'
  public static boolean access(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "access")) return false;
    if (!nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KPUBLIC)
        && replaceVariants(builder_, 2, "<access>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<access>");
    result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (result_) {
      marker_.done(HAXE_ACCESS);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // ('+' | '-') multiplicativeExpressionWrapper
  public static boolean additiveExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression")) return false;
    if (!nextTokenIs(builder_, OPLUS) && !nextTokenIs(builder_, OMINUS)
        && replaceVariants(builder_, 2, "<additive expression>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "additiveExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<additive expression>");
    result_ = additiveExpression_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && multiplicativeExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ADDITIVE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ('+' | '-')
  private static boolean additiveExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression_0")) return false;
    return additiveExpression_0_0(builder_, level_ + 1);
  }

  // '+' | '-'
  private static boolean additiveExpression_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OPLUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // multiplicativeExpressionWrapper additiveExpression*
  static boolean additiveExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = multiplicativeExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && additiveExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // additiveExpression*
  private static boolean additiveExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!additiveExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "additiveExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // 'function' '(' parameterList? ')' typeTag? 'untyped'? '{' '}'
  public static boolean anonymousFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, KFUNCTION)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KFUNCTION);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, anonymousFunctionDeclaration_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, anonymousFunctionDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, anonymousFunctionDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_ANONYMOUS_FUNCTION_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, PLCURLY);
    result_ = result_ && anonymousTypeBody(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRCURLY);
    pinned_ = result_; // pin = 3
    if (result_ || pinned_) {
      marker_.done(HAXE_ANONYMOUS_TYPE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // extendedAnonymousTypeBody | simpleAnonymousTypeBody | interfaceBody
  public static boolean anonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeBody")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<anonymous type body>");
    result_ = extendedAnonymousTypeBody(builder_, level_ + 1);
    if (!result_) result_ = simpleAnonymousTypeBody(builder_, level_ + 1);
    if (!result_) result_ = interfaceBody(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ANONYMOUS_TYPE_BODY);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // componentName typeTag
  public static boolean anonymousTypeField(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeField")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && typeTag(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ANONYMOUS_TYPE_FIELD);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // anonymousTypeField (',' anonymousTypeField)*
  public static boolean anonymousTypeFieldList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeFieldList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = anonymousTypeField(builder_, level_ + 1);
    result_ = result_ && anonymousTypeFieldList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ANONYMOUS_TYPE_FIELD_LIST);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  // (',' anonymousTypeField)*
  private static boolean anonymousTypeFieldList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeFieldList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!anonymousTypeFieldList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "anonymousTypeFieldList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' anonymousTypeField)
  private static boolean anonymousTypeFieldList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeFieldList_1_0")) return false;
    return anonymousTypeFieldList_1_0_0(builder_, level_ + 1);
  }

  // ',' anonymousTypeField
  private static boolean anonymousTypeFieldList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeFieldList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && anonymousTypeField(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '[' expression? ']'
  public static boolean arrayAccessExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "arrayAccessExpression")) return false;
    if (!nextTokenIs(builder_, PLBRACK)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "arrayAccessExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, PLBRACK);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, arrayAccessExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRBRACK) && result_;
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ARRAY_ACCESS_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLBRACK);
    result_ = result_ && arrayLiteral_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRBRACK);
    if (result_) {
      marker_.done(HAXE_ARRAY_LITERAL);
    }
    else {
      marker_.rollbackTo();
    }
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
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "assignExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<assign expression>");
    result_ = assignOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && iteratorExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ASSIGN_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // iteratorExpressionWrapper assignExpression*
  static boolean assignExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = iteratorExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && assignExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // assignExpression*
  private static boolean assignExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!assignExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "assignExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // '=' | '+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' | '<<=' | '>>=' | '>>>='
  public static boolean assignOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignOperation")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<assign operation>");
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
    if (result_) {
      marker_.done(HAXE_ASSIGN_OPERATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // '@:autoBuild' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean autoBuildMacro(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KAUTOBUILD);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, referenceExpression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, autoBuildMacro_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_AUTO_BUILD_MACRO);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean autoBuildMacro_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro_3")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!autoBuildMacro_3_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "autoBuildMacro_3");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)
  private static boolean autoBuildMacro_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro_3_0")) return false;
    return autoBuildMacro_3_0_0(builder_, level_ + 1);
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean autoBuildMacro_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro_3_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '|' | '&' | '^'
  public static boolean bitOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitOperation")) return false;
    if (!nextTokenIs(builder_, OBIT_AND) && !nextTokenIs(builder_, OBIT_XOR)
        && !nextTokenIs(builder_, OBIT_OR) && replaceVariants(builder_, 3, "<bit operation>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<bit operation>");
    result_ = consumeToken(builder_, OBIT_OR);
    if (!result_) result_ = consumeToken(builder_, OBIT_AND);
    if (!result_) result_ = consumeToken(builder_, OBIT_XOR);
    if (result_) {
      marker_.done(HAXE_BIT_OPERATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // '@:bitmap' '(' LITSTRING ')'
  public static boolean bitmapMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitmapMeta")) return false;
    if (!nextTokenIs(builder_, KBITMAP)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KBITMAP);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, LITSTRING)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_BITMAP_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // bitOperation shiftExpressionWrapper
  public static boolean bitwiseExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpression")) return false;
    if (!nextTokenIs(builder_, OBIT_AND) && !nextTokenIs(builder_, OBIT_XOR)
        && !nextTokenIs(builder_, OBIT_OR) && replaceVariants(builder_, 3, "<bitwise expression>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "bitwiseExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<bitwise expression>");
    result_ = bitOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && shiftExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_BITWISE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // shiftExpressionWrapper bitwiseExpression*
  static boolean bitwiseExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = shiftExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && bitwiseExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // bitwiseExpression*
  private static boolean bitwiseExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!bitwiseExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "bitwiseExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // '{' statementList? '}'
  public static boolean blockStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "blockStatement")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, PLCURLY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, blockStatement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_BLOCK_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KBREAK);
    pinned_ = result_; // pin = 1
    result_ = result_ && consumeToken(builder_, OSEMI);
    if (result_ || pinned_) {
      marker_.done(HAXE_BREAK_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:build' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean buildMacro(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro")) return false;
    if (!nextTokenIs(builder_, KBUILD)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KBUILD);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, referenceExpression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, buildMacro_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_BUILD_MACRO);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean buildMacro_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro_3")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!buildMacro_3_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "buildMacro_3");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)
  private static boolean buildMacro_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro_3_0")) return false;
    return buildMacro_3_0_0(builder_, level_ + 1);
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean buildMacro_3_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro_3_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '(' expressionList? ')'
  public static boolean callExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callExpression")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "callExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, callExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_CALL_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = callOrArrayAccess_0(builder_, level_ + 1);
    result_ = result_ && callOrArrayAccess_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (referenceExpression | thisExpression | superExpression)
  private static boolean callOrArrayAccess_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_0")) return false;
    return callOrArrayAccess_0_0(builder_, level_ + 1);
  }

  // referenceExpression | thisExpression | superExpression
  private static boolean callOrArrayAccess_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = referenceExpression(builder_, level_ + 1);
    if (!result_) result_ = thisExpression(builder_, level_ + 1);
    if (!result_) result_ = superExpression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean callOrArrayAccess_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!callOrArrayAccess_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "callOrArrayAccess_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)
  private static boolean callOrArrayAccess_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_1_0")) return false;
    return callOrArrayAccess_1_0_0(builder_, level_ + 1);
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean callOrArrayAccess_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "callOrArrayAccess_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'cast' (('(' expression ',' functionTypeWrapper ')')  | expression)
  public static boolean castExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression")) return false;
    if (!nextTokenIs(builder_, KCAST)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KCAST);
    pinned_ = result_; // pin = 1
    result_ = result_ && castExpression_1(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_CAST_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // (('(' expression ',' functionTypeWrapper ')')  | expression)
  private static boolean castExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression_1")) return false;
    return castExpression_1_0(builder_, level_ + 1);
  }

  // ('(' expression ',' functionTypeWrapper ')')  | expression
  private static boolean castExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression_1_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = castExpression_1_0_0(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ('(' expression ',' functionTypeWrapper ')')
  private static boolean castExpression_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression_1_0_0")) return false;
    return castExpression_1_0_0_0(builder_, level_ + 1);
  }

  // '(' expression ',' functionTypeWrapper ')'
  private static boolean castExpression_1_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression_1_0_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOMMA);
    result_ = result_ && functionTypeWrapper(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'catch' '(' parameter ')' statement ';'?
  public static boolean catchStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "catchStatement")) return false;
    if (!nextTokenIs(builder_, KCATCH)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KCATCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, parameter(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && catchStatement_5(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_CATCH_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<class body>");
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!classBodyPart(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "classBody");
        break;
      }
      offset_ = next_offset_;
    }
    marker_.done(HAXE_CLASS_BODY);
    exitErrorRecordingSection(builder_, level_, true, false, _SECTION_GENERAL_, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionDeclarationWithAttributes
  static boolean classBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classBodyPart")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, null);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = functionDeclarationWithAttributes(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, class_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // macroClass* 'private'? 'class' componentName genericParam? inheritList? '{' classBody '}'
  public static boolean classDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBIND)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KHACK)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KMETA) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, KCLASS) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, MACRO_ID) && replaceVariants(builder_, 17, "<class declaration>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<class declaration>");
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
    if (result_ || pinned_) {
      marker_.done(HAXE_CLASS_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroClass*
  private static boolean classDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroClass(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "classDeclaration_0");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // 'private'?
  private static boolean classDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration_1")) return false;
    consumeToken(builder_, KPRIVATE);
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
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  static boolean class_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !class_body_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  private static boolean class_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover_0")) return false;
    return class_body_part_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID
  private static boolean class_body_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, KBUILD);
    if (!result_) result_ = consumeToken(builder_, KAUTOBUILD);
    if (!result_) result_ = consumeToken(builder_, KDEBUG);
    if (!result_) result_ = consumeToken(builder_, KGETTER);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KOVERLOAD);
    if (!result_) result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KNODEBUG);
    if (!result_) result_ = consumeToken(builder_, KNS);
    if (!result_) result_ = consumeToken(builder_, KPROTECTED);
    if (!result_) result_ = consumeToken(builder_, KREQUIRE);
    if (!result_) result_ = consumeToken(builder_, KSETTER);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, MACRO_ID);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // compareOperation bitwiseExpressionWrapper
  public static boolean compareExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpression")) return false;
    if (!nextTokenIs(builder_, ONOT_EQ) && !nextTokenIs(builder_, OLESS)
        && !nextTokenIs(builder_, OLESS_OR_EQUAL) && !nextTokenIs(builder_, OEQ)
        && !nextTokenIs(builder_, OGREATER) && !nextTokenIs(builder_, OGREATER_OR_EQUAL)
        && replaceVariants(builder_, 6, "<compare expression>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "compareExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<compare expression>");
    result_ = compareOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && bitwiseExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_COMPARE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // bitwiseExpressionWrapper compareExpression*
  static boolean compareExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = bitwiseExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && compareExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // compareExpression*
  private static boolean compareExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!compareExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "compareExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // '==' | '!=' | '<=' | '<' | '>' | '>='
  public static boolean compareOperation(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareOperation")) return false;
    if (!nextTokenIs(builder_, ONOT_EQ) && !nextTokenIs(builder_, OLESS)
        && !nextTokenIs(builder_, OLESS_OR_EQUAL) && !nextTokenIs(builder_, OEQ)
        && !nextTokenIs(builder_, OGREATER) && !nextTokenIs(builder_, OGREATER_OR_EQUAL)
        && replaceVariants(builder_, 6, "<compare operation>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<compare operation>");
    result_ = consumeToken(builder_, OEQ);
    if (!result_) result_ = consumeToken(builder_, ONOT_EQ);
    if (!result_) result_ = consumeToken(builder_, OLESS_OR_EQUAL);
    if (!result_) result_ = consumeToken(builder_, OLESS);
    if (!result_) result_ = consumeToken(builder_, OGREATER);
    if (!result_) result_ = consumeToken(builder_, OGREATER_OR_EQUAL);
    if (result_) {
      marker_.done(HAXE_COMPARE_OPERATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean componentName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "componentName")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = identifier(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_COMPONENT_NAME);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'continue' ';'
  public static boolean continueStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "continueStatement")) return false;
    if (!nextTokenIs(builder_, KCONTINUE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KCONTINUE);
    pinned_ = result_; // pin = 1
    result_ = result_ && consumeToken(builder_, OSEMI);
    if (result_ || pinned_) {
      marker_.done(HAXE_CONTINUE_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // MACRO_ID ('(' customMetaLiterals ')')?
  public static boolean customMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta")) return false;
    if (!nextTokenIs(builder_, MACRO_ID)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, MACRO_ID);
    pinned_ = result_; // pin = 1
    result_ = result_ && customMeta_1(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_CUSTOM_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ('(' customMetaLiterals ')')?
  private static boolean customMeta_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta_1")) return false;
    customMeta_1_0(builder_, level_ + 1);
    return true;
  }

  // ('(' customMetaLiterals ')')
  private static boolean customMeta_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta_1_0")) return false;
    return customMeta_1_0_0(builder_, level_ + 1);
  }

  // '(' customMetaLiterals ')'
  private static boolean customMeta_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && customMetaLiterals(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // literalExpression (',' literalExpression)*
  static boolean customMetaLiterals(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMetaLiterals")) return false;
    if (!nextTokenIs(builder_, PLBRACK) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, LITINT)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = literalExpression(builder_, level_ + 1);
    result_ = result_ && customMetaLiterals_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' literalExpression)*
  private static boolean customMetaLiterals_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMetaLiterals_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!customMetaLiterals_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "customMetaLiterals_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' literalExpression)
  private static boolean customMetaLiterals_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMetaLiterals_1_0")) return false;
    return customMetaLiterals_1_0_0(builder_, level_ + 1);
  }

  // ',' literalExpression
  private static boolean customMetaLiterals_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMetaLiterals_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && literalExpression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'static' | 'inline' | 'dynamic' | 'override' | access
  public static boolean declarationAttribute(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "declarationAttribute")) return false;
    if (!nextTokenIs(builder_, KDYNAMIC) && !nextTokenIs(builder_, KINLINE)
        && !nextTokenIs(builder_, KOVERRIDE) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KSTATIC)
        && replaceVariants(builder_, 6, "<declaration attribute>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<declaration attribute>");
    result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = access(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_DECLARATION_ATTRIBUTE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // declarationAttribute+
  public static boolean declarationAttributeList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "declarationAttributeList")) return false;
    if (!nextTokenIs(builder_, KDYNAMIC) && !nextTokenIs(builder_, KINLINE)
        && !nextTokenIs(builder_, KOVERRIDE) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KSTATIC)
        && replaceVariants(builder_, 6, "<declaration attribute list>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<declaration attribute list>");
    result_ = declarationAttribute(builder_, level_ + 1);
    int offset_ = builder_.getCurrentOffset();
    while (result_) {
      if (!declarationAttribute(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "declarationAttributeList");
        break;
      }
      offset_ = next_offset_;
    }
    if (result_) {
      marker_.done(HAXE_DECLARATION_ATTRIBUTE_LIST);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // 'default' ':' switchCaseBlock?
  public static boolean defaultCase(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "defaultCase")) return false;
    if (!nextTokenIs(builder_, KDEFAULT)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KDEFAULT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, OCOLON));
    result_ = pinned_ && defaultCase_2(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_DEFAULT_CASE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KDO);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, statement(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, KWHILE)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_DO_WHILE_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // enumValueDeclaration*
  public static boolean enumBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumBody")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<enum body>");
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!enumValueDeclaration(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "enumBody");
        break;
      }
      offset_ = next_offset_;
    }
    marker_.done(HAXE_ENUM_BODY);
    exitErrorRecordingSection(builder_, level_, true, false, _SECTION_GENERAL_, null);
    return true;
  }

  /* ********************************************************** */
  // '(' parameterList? ')'
  public static boolean enumConstructorParameters(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumConstructorParameters")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, enumConstructorParameters_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_ENUM_CONSTRUCTOR_PARAMETERS);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // parameterList?
  private static boolean enumConstructorParameters_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumConstructorParameters_1")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // macroClass* externOrPrivate? 'enum' componentName genericParam? '{' enumBody '}'
  public static boolean enumDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumDeclaration")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBIND)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KHACK)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KMETA) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, KENUM) && !nextTokenIs(builder_, KEXTERN)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, MACRO_ID)
        && replaceVariants(builder_, 18, "<enum declaration>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<enum declaration>");
    result_ = enumDeclaration_0(builder_, level_ + 1);
    result_ = result_ && enumDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KENUM);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, enumDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && report_error_(builder_, enumBody(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_ENUM_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroClass*
  private static boolean enumDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumDeclaration_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroClass(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "enumDeclaration_0");
        break;
      }
      offset_ = next_offset_;
    }
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<enum value declaration>");
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && enumValueDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OSEMI);
    if (result_) {
      marker_.done(HAXE_ENUM_VALUE_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, enum_value_declaration_recovery_parser_);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !enum_value_declaration_recovery_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // (ID | '}')
  private static boolean enum_value_declaration_recovery_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enum_value_declaration_recovery_0")) return false;
    return enum_value_declaration_recovery_0_0(builder_, level_ + 1);
  }

  // ID | '}'
  private static boolean enum_value_declaration_recovery_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enum_value_declaration_recovery_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ID);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // assignExpressionWrapper
  public static boolean expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression")) return false;
    boolean result_ = false;
    int start_ = builder_.getCurrentOffset();
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<expression>");
    result_ = assignExpressionWrapper(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_EXPRESSION)) {
      marker_.drop();
    }
    else if (result_) {
      marker_.done(HAXE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, expression_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // expression (',' expression)*
  public static boolean expressionList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<expression list>");
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && expressionList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_EXPRESSION_LIST);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, expression_list_recover_parser_);
    return result_;
  }

  // (',' expression)*
  private static boolean expressionList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!expressionList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "expressionList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' expression)
  private static boolean expressionList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList_1_0")) return false;
    return expressionList_1_0_0(builder_, level_ + 1);
  }

  // ',' expression
  private static boolean expressionList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && expression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // !(')' | ']')
  static boolean expression_list_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_list_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !expression_list_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // (')' | ']')
  private static boolean expression_list_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_list_recover_0")) return false;
    return expression_list_recover_0_0(builder_, level_ + 1);
  }

  // ')' | ']'
  private static boolean expression_list_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_list_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PRPAREN);
    if (!result_) result_ = consumeToken(builder_, PRBRACK);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // !('!' | '!=' | '%' | '%=' | '&&' | '&' | '&=' | '(' | ')' | '*' | '*=' | '+' | '++' | '+=' | ',' | '-' | '--' | '-=' | '.' | '...' | '/' | '/=' | ':' | ';' | '<' | '<<' | '<<=' | '<=' | '=' | '==' | '>' | '>=' | '>>=' | '>>>=' | '?' | '@:autoBuild' | '@:build' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:overload' | '@:protected' | '@:require' | '@:setter' | '[' | ']' | '^' | '^=' | 'break' | 'case' | 'cast' | 'catch' | 'continue' | 'default' | 'do' | 'dynamic' | 'else' | 'false' | 'for' | 'function' | 'if' | 'inline' | 'new' | 'null' | 'override' | 'private' | 'public' | 'return' | 'static' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '|' | '|=' | '||' | '}' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING | MACRO_ID | REG_EXP)
  static boolean expression_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !expression_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('!' | '!=' | '%' | '%=' | '&&' | '&' | '&=' | '(' | ')' | '*' | '*=' | '+' | '++' | '+=' | ',' | '-' | '--' | '-=' | '.' | '...' | '/' | '/=' | ':' | ';' | '<' | '<<' | '<<=' | '<=' | '=' | '==' | '>' | '>=' | '>>=' | '>>>=' | '?' | '@:autoBuild' | '@:build' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:overload' | '@:protected' | '@:require' | '@:setter' | '[' | ']' | '^' | '^=' | 'break' | 'case' | 'cast' | 'catch' | 'continue' | 'default' | 'do' | 'dynamic' | 'else' | 'false' | 'for' | 'function' | 'if' | 'inline' | 'new' | 'null' | 'override' | 'private' | 'public' | 'return' | 'static' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '|' | '|=' | '||' | '}' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING | MACRO_ID | REG_EXP)
  private static boolean expression_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_recover_0")) return false;
    return expression_recover_0_0(builder_, level_ + 1);
  }

  // '!' | '!=' | '%' | '%=' | '&&' | '&' | '&=' | '(' | ')' | '*' | '*=' | '+' | '++' | '+=' | ',' | '-' | '--' | '-=' | '.' | '...' | '/' | '/=' | ':' | ';' | '<' | '<<' | '<<=' | '<=' | '=' | '==' | '>' | '>=' | '>>=' | '>>>=' | '?' | '@:autoBuild' | '@:build' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:overload' | '@:protected' | '@:require' | '@:setter' | '[' | ']' | '^' | '^=' | 'break' | 'case' | 'cast' | 'catch' | 'continue' | 'default' | 'do' | 'dynamic' | 'else' | 'false' | 'for' | 'function' | 'if' | 'inline' | 'new' | 'null' | 'override' | 'private' | 'public' | 'return' | 'static' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '|' | '|=' | '||' | '}' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING | MACRO_ID | REG_EXP
  private static boolean expression_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
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
    if (!result_) result_ = consumeToken(builder_, KAUTOBUILD);
    if (!result_) result_ = consumeToken(builder_, KBUILD);
    if (!result_) result_ = consumeToken(builder_, KDEBUG);
    if (!result_) result_ = consumeToken(builder_, KGETTER);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KNODEBUG);
    if (!result_) result_ = consumeToken(builder_, KNS);
    if (!result_) result_ = consumeToken(builder_, KOVERLOAD);
    if (!result_) result_ = consumeToken(builder_, KPROTECTED);
    if (!result_) result_ = consumeToken(builder_, KREQUIRE);
    if (!result_) result_ = consumeToken(builder_, KSETTER);
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
    if (!result_) result_ = consumeToken(builder_, LITCHAR);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, LITSTRING);
    if (!result_) result_ = consumeToken(builder_, MACRO_ID);
    if (!result_) result_ = consumeToken(builder_, REG_EXP);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // typeExtends (',' anonymousTypeFieldList)? (',' interfaceBody)?
  static boolean extendedAnonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = typeExtends(builder_, level_ + 1);
    result_ = result_ && extendedAnonymousTypeBody_1(builder_, level_ + 1);
    result_ = result_ && extendedAnonymousTypeBody_2(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' anonymousTypeFieldList)?
  private static boolean extendedAnonymousTypeBody_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_1")) return false;
    extendedAnonymousTypeBody_1_0(builder_, level_ + 1);
    return true;
  }

  // (',' anonymousTypeFieldList)
  private static boolean extendedAnonymousTypeBody_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_1_0")) return false;
    return extendedAnonymousTypeBody_1_0_0(builder_, level_ + 1);
  }

  // ',' anonymousTypeFieldList
  private static boolean extendedAnonymousTypeBody_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && anonymousTypeFieldList(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' interfaceBody)?
  private static boolean extendedAnonymousTypeBody_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_2")) return false;
    extendedAnonymousTypeBody_2_0(builder_, level_ + 1);
    return true;
  }

  // (',' interfaceBody)
  private static boolean extendedAnonymousTypeBody_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_2_0")) return false;
    return extendedAnonymousTypeBody_2_0_0(builder_, level_ + 1);
  }

  // ',' interfaceBody
  private static boolean extendedAnonymousTypeBody_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody_2_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && interfaceBody(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // macroClass* externOrPrivate* 'class' componentName genericParam? inheritList? '{' externClassDeclarationBody '}'
  public static boolean externClassDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBIND)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KHACK)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KMETA) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, KCLASS) && !nextTokenIs(builder_, KEXTERN)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, MACRO_ID)
        && replaceVariants(builder_, 18, "<extern class declaration>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<extern class declaration>");
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
    if (result_ || pinned_) {
      marker_.done(HAXE_EXTERN_CLASS_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroClass*
  private static boolean externClassDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroClass(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "externClassDeclaration_0");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // externOrPrivate*
  private static boolean externClassDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!externOrPrivate(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "externClassDeclaration_1");
        break;
      }
      offset_ = next_offset_;
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
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<extern class declaration body>");
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!externClassDeclarationBodyPart(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "externClassDeclarationBody");
        break;
      }
      offset_ = next_offset_;
    }
    marker_.done(HAXE_EXTERN_CLASS_DECLARATION_BODY);
    exitErrorRecordingSection(builder_, level_, true, false, _SECTION_GENERAL_, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | externFunctionDeclaration
  static boolean externClassDeclarationBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclarationBodyPart")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, null);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = externFunctionDeclaration(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, extern_class_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // functionMacroMember* declarationAttributeList? 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? (functionCommonBody | ';')
  public static boolean externFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KDEBUG) && !nextTokenIs(builder_, KGETTER)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KOVERLOAD) && !nextTokenIs(builder_, KPROTECTED)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, KSETTER)
        && !nextTokenIs(builder_, KDYNAMIC) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, KINLINE) && !nextTokenIs(builder_, KOVERRIDE)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KPUBLIC)
        && !nextTokenIs(builder_, KSTATIC) && !nextTokenIs(builder_, MACRO_ID)
        && replaceVariants(builder_, 20, "<extern function declaration>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<extern function declaration>");
    result_ = externFunctionDeclaration_0(builder_, level_ + 1);
    result_ = result_ && externFunctionDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KFUNCTION);
    result_ = result_ && externFunctionDeclaration_3(builder_, level_ + 1);
    pinned_ = result_; // pin = 4
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, externFunctionDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, externFunctionDeclaration_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, externFunctionDeclaration_8(builder_, level_ + 1)) && result_;
    result_ = pinned_ && externFunctionDeclaration_9(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_EXTERN_FUNCTION_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // functionMacroMember*
  private static boolean externFunctionDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!functionMacroMember(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "externFunctionDeclaration_0");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // declarationAttributeList?
  private static boolean externFunctionDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_1")) return false;
    declarationAttributeList(builder_, level_ + 1);
    return true;
  }

  // ('new' | componentName genericParam?)
  private static boolean externFunctionDeclaration_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_3")) return false;
    return externFunctionDeclaration_3_0(builder_, level_ + 1);
  }

  // 'new' | componentName genericParam?
  private static boolean externFunctionDeclaration_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_3_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = externFunctionDeclaration_3_0_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // componentName genericParam?
  private static boolean externFunctionDeclaration_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_3_0_1")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && externFunctionDeclaration_3_0_1_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // genericParam?
  private static boolean externFunctionDeclaration_3_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_3_0_1_1")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // parameterList?
  private static boolean externFunctionDeclaration_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_5")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean externFunctionDeclaration_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_7")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean externFunctionDeclaration_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_8")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  // (functionCommonBody | ';')
  private static boolean externFunctionDeclaration_9(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_9")) return false;
    return externFunctionDeclaration_9_0(builder_, level_ + 1);
  }

  // functionCommonBody | ';'
  private static boolean externFunctionDeclaration_9_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_9_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = functionCommonBody(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, OSEMI);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'extern' | 'private'
  public static boolean externOrPrivate(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externOrPrivate")) return false;
    if (!nextTokenIs(builder_, KEXTERN) && !nextTokenIs(builder_, KPRIVATE)
        && replaceVariants(builder_, 2, "<extern or private>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<extern or private>");
    result_ = consumeToken(builder_, KEXTERN);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (result_) {
      marker_.done(HAXE_EXTERN_OR_PRIVATE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  static boolean extern_class_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !extern_class_body_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  private static boolean extern_class_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover_0")) return false;
    return extern_class_body_part_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID
  private static boolean extern_class_body_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, KBUILD);
    if (!result_) result_ = consumeToken(builder_, KAUTOBUILD);
    if (!result_) result_ = consumeToken(builder_, KDEBUG);
    if (!result_) result_ = consumeToken(builder_, KGETTER);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KOVERLOAD);
    if (!result_) result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KNODEBUG);
    if (!result_) result_ = consumeToken(builder_, KNS);
    if (!result_) result_ = consumeToken(builder_, KPROTECTED);
    if (!result_) result_ = consumeToken(builder_, KREQUIRE);
    if (!result_) result_ = consumeToken(builder_, KSETTER);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, MACRO_ID);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:fakeEnum' '(' type ')'
  public static boolean fakeEnumMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "fakeEnumMeta")) return false;
    if (!nextTokenIs(builder_, KFAKEENUM)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KFAKEENUM);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, type(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_FAKE_ENUM_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'for' '(' componentName 'in' iterable')' statement ';'?
  public static boolean forStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forStatement")) return false;
    if (!nextTokenIs(builder_, KFOR)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KFOR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, componentName(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OIN)) && result_;
    result_ = pinned_ && report_error_(builder_, iterable(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && forStatement_7(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_FOR_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean forStatement_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forStatement_7")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // blockStatement | returnStatementWithoutSemicolon | expression | throwStatement
  static boolean functionCommonBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionCommonBody")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KRETURN) && !nextTokenIs(builder_, KSUPER)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KTHROW) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = blockStatement(builder_, level_ + 1);
    if (!result_) result_ = returnStatementWithoutSemicolon(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    if (!result_) result_ = throwStatement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // functionMacroMember* declarationAttributeList? 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean functionDeclarationWithAttributes(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KDEBUG) && !nextTokenIs(builder_, KGETTER)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KOVERLOAD) && !nextTokenIs(builder_, KPROTECTED)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, KSETTER)
        && !nextTokenIs(builder_, KDYNAMIC) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, KINLINE) && !nextTokenIs(builder_, KOVERRIDE)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KPUBLIC)
        && !nextTokenIs(builder_, KSTATIC) && !nextTokenIs(builder_, MACRO_ID)
        && replaceVariants(builder_, 20, "<function declaration with attributes>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<function declaration with attributes>");
    result_ = functionDeclarationWithAttributes_0(builder_, level_ + 1);
    result_ = result_ && functionDeclarationWithAttributes_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KFUNCTION);
    result_ = result_ && functionDeclarationWithAttributes_3(builder_, level_ + 1);
    pinned_ = result_; // pin = 4
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, functionDeclarationWithAttributes_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionDeclarationWithAttributes_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, functionDeclarationWithAttributes_8(builder_, level_ + 1)) && result_;
    result_ = pinned_ && functionCommonBody(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_FUNCTION_DECLARATION_WITH_ATTRIBUTES);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // functionMacroMember*
  private static boolean functionDeclarationWithAttributes_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!functionMacroMember(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "functionDeclarationWithAttributes_0");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // declarationAttributeList?
  private static boolean functionDeclarationWithAttributes_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_1")) return false;
    declarationAttributeList(builder_, level_ + 1);
    return true;
  }

  // ('new' | componentName genericParam?)
  private static boolean functionDeclarationWithAttributes_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_3")) return false;
    return functionDeclarationWithAttributes_3_0(builder_, level_ + 1);
  }

  // 'new' | componentName genericParam?
  private static boolean functionDeclarationWithAttributes_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_3_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = functionDeclarationWithAttributes_3_0_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // componentName genericParam?
  private static boolean functionDeclarationWithAttributes_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_3_0_1")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && functionDeclarationWithAttributes_3_0_1_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // genericParam?
  private static boolean functionDeclarationWithAttributes_3_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_3_0_1_1")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // parameterList?
  private static boolean functionDeclarationWithAttributes_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_5")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean functionDeclarationWithAttributes_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_7")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionDeclarationWithAttributes_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_8")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // 'function' '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean functionLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionLiteral")) return false;
    if (!nextTokenIs(builder_, KFUNCTION)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KFUNCTION);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, functionLiteral_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionLiteral_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, functionLiteral_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && functionCommonBody(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_FUNCTION_LITERAL);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
  // macroMember | overloadMeta
  static boolean functionMacroMember(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionMacroMember")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KDEBUG) && !nextTokenIs(builder_, KGETTER)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KOVERLOAD) && !nextTokenIs(builder_, KPROTECTED)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, KSETTER)
        && !nextTokenIs(builder_, MACRO_ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = macroMember(builder_, level_ + 1);
    if (!result_) result_ = overloadMeta(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // functionMacroMember* declarationAttributeList? 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? ';'
  public static boolean functionPrototypeDeclarationWithAttributes(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KDEBUG) && !nextTokenIs(builder_, KGETTER)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KOVERLOAD) && !nextTokenIs(builder_, KPROTECTED)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, KSETTER)
        && !nextTokenIs(builder_, KDYNAMIC) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, KINLINE) && !nextTokenIs(builder_, KOVERRIDE)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KPUBLIC)
        && !nextTokenIs(builder_, KSTATIC) && !nextTokenIs(builder_, MACRO_ID)
        && replaceVariants(builder_, 20, "<function prototype declaration with attributes>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<function prototype declaration with attributes>");
    result_ = functionPrototypeDeclarationWithAttributes_0(builder_, level_ + 1);
    result_ = result_ && functionPrototypeDeclarationWithAttributes_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KFUNCTION);
    result_ = result_ && functionPrototypeDeclarationWithAttributes_3(builder_, level_ + 1);
    pinned_ = result_; // pin = 4
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, functionPrototypeDeclarationWithAttributes_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionPrototypeDeclarationWithAttributes_7(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, functionPrototypeDeclarationWithAttributes_8(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // functionMacroMember*
  private static boolean functionPrototypeDeclarationWithAttributes_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!functionMacroMember(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "functionPrototypeDeclarationWithAttributes_0");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // declarationAttributeList?
  private static boolean functionPrototypeDeclarationWithAttributes_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_1")) return false;
    declarationAttributeList(builder_, level_ + 1);
    return true;
  }

  // ('new' | componentName genericParam?)
  private static boolean functionPrototypeDeclarationWithAttributes_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_3")) return false;
    return functionPrototypeDeclarationWithAttributes_3_0(builder_, level_ + 1);
  }

  // 'new' | componentName genericParam?
  private static boolean functionPrototypeDeclarationWithAttributes_3_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_3_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = functionPrototypeDeclarationWithAttributes_3_0_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // componentName genericParam?
  private static boolean functionPrototypeDeclarationWithAttributes_3_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_3_0_1")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && functionPrototypeDeclarationWithAttributes_3_0_1_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // genericParam?
  private static boolean functionPrototypeDeclarationWithAttributes_3_0_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_3_0_1_1")) return false;
    genericParam(builder_, level_ + 1);
    return true;
  }

  // parameterList?
  private static boolean functionPrototypeDeclarationWithAttributes_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_5")) return false;
    parameterList(builder_, level_ + 1);
    return true;
  }

  // typeTag?
  private static boolean functionPrototypeDeclarationWithAttributes_7(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_7")) return false;
    typeTag(builder_, level_ + 1);
    return true;
  }

  // 'untyped'?
  private static boolean functionPrototypeDeclarationWithAttributes_8(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_8")) return false;
    consumeToken(builder_, KUNTYPED);
    return true;
  }

  /* ********************************************************** */
  // '->' typeOrAnonymous
  public static boolean functionType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionType")) return false;
    if (!nextTokenIs(builder_, OARROW)) return false;
    boolean result_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "functionType")) return false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OARROW);
    result_ = result_ && typeOrAnonymous(builder_, level_ + 1);
    if (result_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_FUNCTION_TYPE);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // typeOrAnonymous | '(' functionTypeWrapper ')'
  static boolean functionTypeOrWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeOrWrapper")) return false;
    if (!nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = typeOrAnonymous(builder_, level_ + 1);
    if (!result_) result_ = functionTypeOrWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // '(' functionTypeWrapper ')'
  private static boolean functionTypeOrWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeOrWrapper_1")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && functionTypeWrapper(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // functionTypeOrWrapper functionType*
  static boolean functionTypeWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeWrapper")) return false;
    if (!nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = functionTypeOrWrapper(builder_, level_ + 1);
    result_ = result_ && functionTypeWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // functionType*
  private static boolean functionTypeWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!functionType(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "functionTypeWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // componentName (':' ('(' typeList ')' | typeListPart))?
  public static boolean genericListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && genericListPart_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_GENERIC_LIST_PART);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  // (':' ('(' typeList ')' | typeListPart))?
  private static boolean genericListPart_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1")) return false;
    genericListPart_1_0(builder_, level_ + 1);
    return true;
  }

  // (':' ('(' typeList ')' | typeListPart))
  private static boolean genericListPart_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0")) return false;
    return genericListPart_1_0_0(builder_, level_ + 1);
  }

  // ':' ('(' typeList ')' | typeListPart)
  private static boolean genericListPart_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOLON);
    result_ = result_ && genericListPart_1_0_0_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ('(' typeList ')' | typeListPart)
  private static boolean genericListPart_1_0_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0_0_1")) return false;
    return genericListPart_1_0_0_1_0(builder_, level_ + 1);
  }

  // '(' typeList ')' | typeListPart
  private static boolean genericListPart_1_0_0_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0_0_1_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = genericListPart_1_0_0_1_0_0(builder_, level_ + 1);
    if (!result_) result_ = typeListPart(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // '(' typeList ')'
  private static boolean genericListPart_1_0_0_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0_0_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && typeList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '<' genericListPart (',' genericListPart)* '>'
  public static boolean genericParam(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericParam")) return false;
    if (!nextTokenIs(builder_, OLESS)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OLESS);
    result_ = result_ && genericListPart(builder_, level_ + 1);
    result_ = result_ && genericParam_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_GENERIC_PARAM);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  // (',' genericListPart)*
  private static boolean genericParam_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericParam_2")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!genericParam_2_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "genericParam_2");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' genericListPart)
  private static boolean genericParam_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericParam_2_0")) return false;
    return genericParam_2_0_0(builder_, level_ + 1);
  }

  // ',' genericListPart
  private static boolean genericParam_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericParam_2_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && genericListPart(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:getter' '(' referenceExpression ')'
  public static boolean getterMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "getterMeta")) return false;
    if (!nextTokenIs(builder_, KGETTER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KGETTER);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, referenceExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_GETTER_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // packageStatement? topLevelList
  static boolean haxeFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "haxeFile")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = haxeFile_0(builder_, level_ + 1);
    result_ = result_ && topLevelList(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ID);
    if (result_) {
      marker_.done(HAXE_IDENTIFIER);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'if' '(' expression ')' statement ';'? ('else' statement ';'?)?
  public static boolean ifStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement")) return false;
    if (!nextTokenIs(builder_, KIF)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KIF);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, ifStatement_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && ifStatement_6(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_IF_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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

  // ('else' statement ';'?)
  private static boolean ifStatement_6_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_6_0")) return false;
    return ifStatement_6_0_0(builder_, level_ + 1);
  }

  // 'else' statement ';'?
  private static boolean ifStatement_6_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_6_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KELSE);
    result_ = result_ && statement(builder_, level_ + 1);
    result_ = result_ && ifStatement_6_0_0_2(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ';'?
  private static boolean ifStatement_6_0_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_6_0_0_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpression ';'
  public static boolean importStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importStatement")) return false;
    if (!nextTokenIs(builder_, KIMPORT)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KIMPORT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, simpleQualifiedReferenceExpression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_IMPORT_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // ('extends' | 'implements') type
  public static boolean inherit(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<inherit>");
    result_ = inherit_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && type(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_INHERIT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_RECOVER_, inherit_recover_parser_);
    return result_ || pinned_;
  }

  // ('extends' | 'implements')
  private static boolean inherit_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_0")) return false;
    return inherit_0_0(builder_, level_ + 1);
  }

  // 'extends' | 'implements'
  private static boolean inherit_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KEXTENDS);
    if (!result_) result_ = consumeToken(builder_, KIMPLEMENTS);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // inherit (',' inherit)*
  public static boolean inheritList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList")) return false;
    if (!nextTokenIs(builder_, KEXTENDS) && !nextTokenIs(builder_, KIMPLEMENTS)
        && replaceVariants(builder_, 2, "<inherit list>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<inherit list>");
    result_ = inherit(builder_, level_ + 1);
    result_ = result_ && inheritList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_INHERIT_LIST);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  // (',' inherit)*
  private static boolean inheritList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!inheritList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "inheritList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' inherit)
  private static boolean inheritList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList_1_0")) return false;
    return inheritList_1_0_0(builder_, level_ + 1);
  }

  // ',' inherit
  private static boolean inheritList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inheritList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && inherit(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // !(',' | '{')
  static boolean inherit_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !inherit_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // (',' | '{')
  private static boolean inherit_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_recover_0")) return false;
    return inherit_recover_0_0(builder_, level_ + 1);
  }

  // ',' | '{'
  private static boolean inherit_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    if (!result_) result_ = consumeToken(builder_, PLCURLY);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // interfaceBodyPart*
  public static boolean interfaceBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceBody")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<interface body>");
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!interfaceBodyPart(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "interfaceBody");
        break;
      }
      offset_ = next_offset_;
    }
    marker_.done(HAXE_INTERFACE_BODY);
    exitErrorRecordingSection(builder_, level_, true, false, _SECTION_GENERAL_, null);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionPrototypeDeclarationWithAttributes
  static boolean interfaceBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceBodyPart")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, null);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = functionPrototypeDeclarationWithAttributes(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, interface_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // macroClass* externOrPrivate? 'interface' componentName genericParam? inheritList? '{' interfaceBody '}'
  public static boolean interfaceDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBIND)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KHACK)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KMETA) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, KEXTERN) && !nextTokenIs(builder_, KINTERFACE)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, MACRO_ID)
        && replaceVariants(builder_, 18, "<interface declaration>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<interface declaration>");
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
    if (result_ || pinned_) {
      marker_.done(HAXE_INTERFACE_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroClass*
  private static boolean interfaceDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroClass(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "interfaceDeclaration_0");
        break;
      }
      offset_ = next_offset_;
    }
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
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  static boolean interface_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !interface_body_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  private static boolean interface_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover_0")) return false;
    return interface_body_part_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:overload' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID
  private static boolean interface_body_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, KBUILD);
    if (!result_) result_ = consumeToken(builder_, KAUTOBUILD);
    if (!result_) result_ = consumeToken(builder_, KDEBUG);
    if (!result_) result_ = consumeToken(builder_, KGETTER);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KOVERLOAD);
    if (!result_) result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KNODEBUG);
    if (!result_) result_ = consumeToken(builder_, KNS);
    if (!result_) result_ = consumeToken(builder_, KPROTECTED);
    if (!result_) result_ = consumeToken(builder_, KREQUIRE);
    if (!result_) result_ = consumeToken(builder_, KSETTER);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, MACRO_ID);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // expression
  public static boolean iterable(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iterable")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)
        && replaceVariants(builder_, 28, "<iterable>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<iterable>");
    result_ = expression(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ITERABLE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // '...' ternaryExpressionWrapper
  public static boolean iteratorExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpression")) return false;
    if (!nextTokenIs(builder_, OTRIPLE_DOT)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "iteratorExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, OTRIPLE_DOT);
    pinned_ = result_; // pin = 1
    result_ = result_ && ternaryExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ITERATOR_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // ternaryExpressionWrapper iteratorExpression?
  static boolean iteratorExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = ternaryExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && iteratorExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // iteratorExpression?
  private static boolean iteratorExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpressionWrapper_1")) return false;
    iteratorExpression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // LITINT | LITHEX | LITOCT | LITFLOAT | LITSTRING | LITCHAR
  //                     | regularExpressionLiteral
  //                     | 'null' | 'true' | 'false'
  //                     | functionLiteral
  //                     | arrayLiteral
  //                     | objectLiteral
  public static boolean literalExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literalExpression")) return false;
    if (!nextTokenIs(builder_, PLBRACK) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, LITINT)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, REG_EXP) && replaceVariants(builder_, 13, "<literal expression>")) return false;
    boolean result_ = false;
    int start_ = builder_.getCurrentOffset();
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<literal expression>");
    result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITSTRING);
    if (!result_) result_ = consumeToken(builder_, LITCHAR);
    if (!result_) result_ = regularExpressionLiteral(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KTRUE);
    if (!result_) result_ = consumeToken(builder_, KFALSE);
    if (!result_) result_ = functionLiteral(builder_, level_ + 1);
    if (!result_) result_ = arrayLiteral(builder_, level_ + 1);
    if (!result_) result_ = objectLiteral(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_LITERAL_EXPRESSION)) {
      marker_.drop();
    }
    else if (result_) {
      marker_.done(HAXE_LITERAL_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // 'function' componentName genericParam? '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean localFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, KFUNCTION)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
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
    if (result_ || pinned_) {
      marker_.done(HAXE_LOCAL_FUNCTION_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KVAR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, localVarDeclarationPartList(builder_, level_ + 1));
    result_ = pinned_ && localVarDeclaration_2(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_LOCAL_VAR_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<local var declaration part>");
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_1(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_2(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_3(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_LOCAL_VAR_DECLARATION_PART);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, local_var_declaration_part_recover_parser_);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = localVarDeclarationPart(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPartList_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' localVarDeclarationPart)*
  private static boolean localVarDeclarationPartList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPartList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!localVarDeclarationPartList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "localVarDeclarationPartList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' localVarDeclarationPart)
  private static boolean localVarDeclarationPartList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPartList_1_0")) return false;
    return localVarDeclarationPartList_1_0_0(builder_, level_ + 1);
  }

  // ',' localVarDeclarationPart
  private static boolean localVarDeclarationPartList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localVarDeclarationPartList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && localVarDeclarationPart(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // !('!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | ')' | '++' | ',' | '-' | '--' | ';' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'else' | 'false' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '}' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING | REG_EXP)
  static boolean local_var_declaration_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !local_var_declaration_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | ')' | '++' | ',' | '-' | '--' | ';' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'else' | 'false' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '}' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING | REG_EXP)
  private static boolean local_var_declaration_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover_0")) return false;
    return local_var_declaration_part_recover_0_0(builder_, level_ + 1);
  }

  // '!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | ')' | '++' | ',' | '-' | '--' | ';' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'else' | 'false' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'super' | 'switch' | 'this' | 'throw' | 'true' | 'try' | 'untyped' | 'var' | 'while' | '{' | '}' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING | REG_EXP
  private static boolean local_var_declaration_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
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
    if (!result_) result_ = consumeToken(builder_, LITCHAR);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, LITSTRING);
    if (!result_) result_ = consumeToken(builder_, REG_EXP);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '&&' compareExpressionWrapper
  public static boolean logicAndExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicAndExpression")) return false;
    if (!nextTokenIs(builder_, OCOND_AND)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "logicAndExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, OCOND_AND);
    pinned_ = result_; // pin = 1
    result_ = result_ && compareExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_LOGIC_AND_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // compareExpressionWrapper logicAndExpression*
  static boolean logicAndExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicAndExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = compareExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && logicAndExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // logicAndExpression*
  private static boolean logicAndExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicAndExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!logicAndExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "logicAndExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // '||' logicAndExpressionWrapper
  public static boolean logicOrExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicOrExpression")) return false;
    if (!nextTokenIs(builder_, OCOND_OR)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "logicOrExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, OCOND_OR);
    pinned_ = result_; // pin = 1
    result_ = result_ && logicAndExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_LOGIC_OR_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // logicAndExpressionWrapper logicOrExpression*
  static boolean logicOrExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicOrExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = logicAndExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && logicOrExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // logicOrExpression*
  private static boolean logicOrExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicOrExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!logicOrExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "logicOrExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // '@:final' | '@:keep' | '@:core_api' | '@:bind' | '@:macro' | '@:hack'
  //                       | requireMeta | fakeEnumMeta | nativeMeta | bitmapMeta | nsMeta | customMeta | metaMeta | buildMacro | autoBuildMacro
  static boolean macroClass(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroClass")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBIND)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KHACK)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KMETA) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, MACRO_ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KFINAL);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KCOREAPI);
    if (!result_) result_ = consumeToken(builder_, KBIND);
    if (!result_) result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KHACK);
    if (!result_) result_ = requireMeta(builder_, level_ + 1);
    if (!result_) result_ = fakeEnumMeta(builder_, level_ + 1);
    if (!result_) result_ = nativeMeta(builder_, level_ + 1);
    if (!result_) result_ = bitmapMeta(builder_, level_ + 1);
    if (!result_) result_ = nsMeta(builder_, level_ + 1);
    if (!result_) result_ = customMeta(builder_, level_ + 1);
    if (!result_) result_ = metaMeta(builder_, level_ + 1);
    if (!result_) result_ = buildMacro(builder_, level_ + 1);
    if (!result_) result_ = autoBuildMacro(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:macro' | '@:protected' | '@:debug' | '@:nodebug' | '@:keep'
  //                        | requireMeta | nsMeta | getterMeta | setterMeta | customMeta | buildMacro | autoBuildMacro
  static boolean macroMember(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroMember")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KDEBUG) && !nextTokenIs(builder_, KGETTER)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KPROTECTED) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, KSETTER) && !nextTokenIs(builder_, MACRO_ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KPROTECTED);
    if (!result_) result_ = consumeToken(builder_, KDEBUG);
    if (!result_) result_ = consumeToken(builder_, KNODEBUG);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = requireMeta(builder_, level_ + 1);
    if (!result_) result_ = nsMeta(builder_, level_ + 1);
    if (!result_) result_ = getterMeta(builder_, level_ + 1);
    if (!result_) result_ = setterMeta(builder_, level_ + 1);
    if (!result_) result_ = customMeta(builder_, level_ + 1);
    if (!result_) result_ = buildMacro(builder_, level_ + 1);
    if (!result_) result_ = autoBuildMacro(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // ID '=' LITSTRING
  public static boolean metaKeyValue(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaKeyValue")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ID);
    result_ = result_ && consumeToken(builder_, OASSIGN);
    result_ = result_ && consumeToken(builder_, LITSTRING);
    if (result_) {
      marker_.done(HAXE_META_KEY_VALUE);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:meta' '(' ID '(' metaPartList ')' ')'
  public static boolean metaMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaMeta")) return false;
    if (!nextTokenIs(builder_, KMETA)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KMETA);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, ID)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, metaPartList(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_META_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // metaKeyValue (',' metaKeyValue)*
  static boolean metaPartList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = metaKeyValue(builder_, level_ + 1);
    result_ = result_ && metaPartList_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' metaKeyValue)*
  private static boolean metaPartList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!metaPartList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "metaPartList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' metaKeyValue)
  private static boolean metaPartList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList_1_0")) return false;
    return metaPartList_1_0_0(builder_, level_ + 1);
  }

  // ',' metaKeyValue
  private static boolean metaPartList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && metaKeyValue(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // ('*' | '/' | '%') (prefixExpression | suffixExpressionWrapper)
  public static boolean multiplicativeExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression")) return false;
    if (!nextTokenIs(builder_, OREMAINDER) && !nextTokenIs(builder_, OMUL)
        && !nextTokenIs(builder_, OQUOTIENT) && replaceVariants(builder_, 3, "<multiplicative expression>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "multiplicativeExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<multiplicative expression>");
    result_ = multiplicativeExpression_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && multiplicativeExpression_1(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_MULTIPLICATIVE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ('*' | '/' | '%')
  private static boolean multiplicativeExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_0")) return false;
    return multiplicativeExpression_0_0(builder_, level_ + 1);
  }

  // '*' | '/' | '%'
  private static boolean multiplicativeExpression_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OMUL);
    if (!result_) result_ = consumeToken(builder_, OQUOTIENT);
    if (!result_) result_ = consumeToken(builder_, OREMAINDER);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (prefixExpression | suffixExpressionWrapper)
  private static boolean multiplicativeExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_1")) return false;
    return multiplicativeExpression_1_0(builder_, level_ + 1);
  }

  // prefixExpression | suffixExpressionWrapper
  private static boolean multiplicativeExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpression_1_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = prefixExpression(builder_, level_ + 1);
    if (!result_) result_ = suffixExpressionWrapper(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // prefixExpression multiplicativeExpression*
  static boolean multiplicativeExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = prefixExpression(builder_, level_ + 1);
    result_ = result_ && multiplicativeExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // multiplicativeExpression*
  private static boolean multiplicativeExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "multiplicativeExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!multiplicativeExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "multiplicativeExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // '@:native' '(' LITSTRING ')'
  public static boolean nativeMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nativeMeta")) return false;
    if (!nextTokenIs(builder_, KNATIVE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KNATIVE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, LITSTRING)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_NATIVE_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'new' type '(' expressionList? ')'
  public static boolean newExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "newExpression")) return false;
    if (!nextTokenIs(builder_, ONEW)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, ONEW);
    result_ = result_ && type(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, newExpression_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_NEW_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = newExpression(builder_, level_ + 1);
    result_ = result_ && newExpressionOrCall_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // qualifiedReferenceTail?
  private static boolean newExpressionOrCall_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "newExpressionOrCall_1")) return false;
    qualifiedReferenceTail(builder_, level_ + 1);
    return true;
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
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KBREAK) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, KCONTINUE) && !nextTokenIs(builder_, KDO)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, KFOR)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KRETURN) && !nextTokenIs(builder_, KSUPER)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KTHROW) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, KVAR) && !nextTokenIs(builder_, KWHILE)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
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
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ('untyped' statement ';'?)
  private static boolean notBlockStatement_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_0")) return false;
    return notBlockStatement_0_0(builder_, level_ + 1);
  }

  // 'untyped' statement ';'?
  private static boolean notBlockStatement_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KUNTYPED);
    result_ = result_ && statement(builder_, level_ + 1);
    result_ = result_ && notBlockStatement_0_0_2(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ';'?
  private static boolean notBlockStatement_0_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_0_0_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // '@:ns' '(' LITSTRING ')'
  public static boolean nsMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "nsMeta")) return false;
    if (!nextTokenIs(builder_, KNS)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KNS);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, LITSTRING)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_NS_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '{' objectLiteralElementList? '}'
  public static boolean objectLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteral")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLCURLY);
    result_ = result_ && objectLiteral_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRCURLY);
    if (result_) {
      marker_.done(HAXE_OBJECT_LITERAL);
    }
    else {
      marker_.rollbackTo();
    }
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<object literal element>");
    result_ = identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOLON);
    result_ = result_ && expression(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_OBJECT_LITERAL_ELEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, object_literal_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // objectLiteralElement (',' objectLiteralElement)*
  static boolean objectLiteralElementList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, null);
    result_ = objectLiteralElement(builder_, level_ + 1);
    result_ = result_ && objectLiteralElementList_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, object_literal_list_recover_parser_);
    return result_;
  }

  // (',' objectLiteralElement)*
  private static boolean objectLiteralElementList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!objectLiteralElementList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "objectLiteralElementList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' objectLiteralElement)
  private static boolean objectLiteralElementList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList_1_0")) return false;
    return objectLiteralElementList_1_0_0(builder_, level_ + 1);
  }

  // ',' objectLiteralElement
  private static boolean objectLiteralElementList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && objectLiteralElement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // !('}')
  static boolean object_literal_list_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_list_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !object_literal_list_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('}')
  private static boolean object_literal_list_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_list_recover_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PRCURLY);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // !(',' | '}')
  static boolean object_literal_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_part_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !object_literal_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // (',' | '}')
  private static boolean object_literal_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_part_recover_0")) return false;
    return object_literal_part_recover_0_0(builder_, level_ + 1);
  }

  // ',' | '}'
  private static boolean object_literal_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "object_literal_part_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:overload' '(' anonymousFunctionDeclaration ')'
  public static boolean overloadMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "overloadMeta")) return false;
    if (!nextTokenIs(builder_, KOVERLOAD)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KOVERLOAD);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, anonymousFunctionDeclaration(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_OVERLOAD_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'package' simpleQualifiedReferenceExpression? ';'
  public static boolean packageStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageStatement")) return false;
    if (!nextTokenIs(builder_, KPACKAGE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KPACKAGE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, packageStatement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_PACKAGE_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    if (!nextTokenIs(builder_, OQUEST) && !nextTokenIs(builder_, ID)
        && replaceVariants(builder_, 2, "<parameter>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<parameter>");
    result_ = parameter_0(builder_, level_ + 1);
    result_ = result_ && componentName(builder_, level_ + 1);
    result_ = result_ && parameter_2(builder_, level_ + 1);
    result_ = result_ && parameter_3(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_PARAMETER);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<parameter list>");
    result_ = parameter(builder_, level_ + 1);
    result_ = result_ && parameterList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_PARAMETER_LIST);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, parameterListRecovery_parser_);
    return result_;
  }

  // (',' parameter)*
  private static boolean parameterList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!parameterList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "parameterList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' parameter)
  private static boolean parameterList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterList_1_0")) return false;
    return parameterList_1_0_0(builder_, level_ + 1);
  }

  // ',' parameter
  private static boolean parameterList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && parameter(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // !')'
  static boolean parameterListRecovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameterListRecovery")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !consumeToken(builder_, PRPAREN);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  /* ********************************************************** */
  // '(' (expression | statement) ')'
  public static boolean parenthesizedExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, parenthesizedExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_PARENTHESIZED_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // (expression | statement)
  private static boolean parenthesizedExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression_1")) return false;
    return parenthesizedExpression_1_0(builder_, level_ + 1);
  }

  // expression | statement
  private static boolean parenthesizedExpression_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression_1_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = expression(builder_, level_ + 1);
    if (!result_) result_ = statement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // parenthesizedExpression qualifiedReferenceTail?
  static boolean parenthesizedExpressionOrCall(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpressionOrCall")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = parenthesizedExpression(builder_, level_ + 1);
    result_ = result_ && parenthesizedExpressionOrCall_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
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
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)
        && replaceVariants(builder_, 28, "<prefix expression>")) return false;
    boolean result_ = false;
    int start_ = builder_.getCurrentOffset();
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<prefix expression>");
    result_ = prefixExpression_0(builder_, level_ + 1);
    if (!result_) result_ = suffixExpressionWrapper(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_PREFIX_EXPRESSION)) {
      marker_.drop();
    }
    else if (result_) {
      marker_.done(HAXE_PREFIX_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  // ('-' | '--' | '++' | '!' | '~') prefixExpression
  private static boolean prefixExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = prefixExpression_0_0(builder_, level_ + 1);
    result_ = result_ && prefixExpression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ('-' | '--' | '++' | '!' | '~')
  private static boolean prefixExpression_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression_0_0")) return false;
    return prefixExpression_0_0_0(builder_, level_ + 1);
  }

  // '-' | '--' | '++' | '!' | '~'
  private static boolean prefixExpression_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression_0_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OMINUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (!result_) result_ = consumeToken(builder_, ONOT);
    if (!result_) result_ = consumeToken(builder_, OCOMPLEMENT);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression | 'null' | 'default' | 'dynamic' | 'never'
  public static boolean propertyAccessor(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyAccessor")) return false;
    if (!nextTokenIs(builder_, KDEFAULT) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KNEVER) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, ID) && replaceVariants(builder_, 5, "<property accessor>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<property accessor>");
    result_ = referenceExpression(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KNEVER);
    if (result_) {
      marker_.done(HAXE_PROPERTY_ACCESSOR);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // '(' propertyAccessor ',' propertyAccessor ')'
  public static boolean propertyDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyDeclaration")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && propertyAccessor(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOMMA);
    result_ = result_ && propertyAccessor(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    if (result_) {
      marker_.done(HAXE_PROPERTY_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '.' referenceExpression
  public static boolean qualifiedReferenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceExpression")) return false;
    if (!nextTokenIs(builder_, ODOT)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "qualifiedReferenceExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, ODOT);
    pinned_ = result_; // pin = 1
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_REFERENCE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // qualifiedReferenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  static boolean qualifiedReferenceTail(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail")) return false;
    if (!nextTokenIs(builder_, ODOT)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    result_ = result_ && qualifiedReferenceTail_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  private static boolean qualifiedReferenceTail_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!qualifiedReferenceTail_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "qualifiedReferenceTail_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (callExpression | arrayAccessExpression | qualifiedReferenceExpression)
  private static boolean qualifiedReferenceTail_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail_1_0")) return false;
    return qualifiedReferenceTail_1_0_0(builder_, level_ + 1);
  }

  // callExpression | arrayAccessExpression | qualifiedReferenceExpression
  private static boolean qualifiedReferenceTail_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = callExpression(builder_, level_ + 1);
    if (!result_) result_ = arrayAccessExpression(builder_, level_ + 1);
    if (!result_) result_ = qualifiedReferenceExpression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean referenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "referenceExpression")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = identifier(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    if (result_ || pinned_) {
      marker_.done(HAXE_REFERENCE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // REG_EXP
  public static boolean regularExpressionLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "regularExpressionLiteral")) return false;
    if (!nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, REG_EXP);
    if (result_) {
      marker_.done(HAXE_REGULAR_EXPRESSION_LITERAL);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:require' '(' identifier ')'
  public static boolean requireMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "requireMeta")) return false;
    if (!nextTokenIs(builder_, KREQUIRE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KREQUIRE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, identifier(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_REQUIRE_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'return' expression? ';'?
  public static boolean returnStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement")) return false;
    if (!nextTokenIs(builder_, KRETURN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KRETURN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, returnStatement_1(builder_, level_ + 1));
    result_ = pinned_ && returnStatement_2(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_RETURN_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KRETURN);
    result_ = result_ && expression(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_RETURN_STATEMENT_WITHOUT_SEMICOLON);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:setter' '(' referenceExpression ')'
  public static boolean setterMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "setterMeta")) return false;
    if (!nextTokenIs(builder_, KSETTER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KSETTER);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, referenceExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_SETTER_META);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // shiftOperator additiveExpressionWrapper
  public static boolean shiftExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpression")) return false;
    if (!nextTokenIs(builder_, OSHIFT_LEFT) && !nextTokenIs(builder_, OGREATER)
        && replaceVariants(builder_, 2, "<shift expression>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "shiftExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<shift expression>");
    result_ = shiftOperator(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && additiveExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_SHIFT_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // additiveExpressionWrapper shiftExpression*
  static boolean shiftExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = additiveExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && shiftExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // shiftExpression*
  private static boolean shiftExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!shiftExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "shiftExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // unsignedShiftRightOperator | shiftRightOperator | '<<'
  public static boolean shiftOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftOperator")) return false;
    if (!nextTokenIs(builder_, OSHIFT_LEFT) && !nextTokenIs(builder_, OGREATER)
        && replaceVariants(builder_, 2, "<shift operator>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<shift operator>");
    result_ = unsignedShiftRightOperator(builder_, level_ + 1);
    if (!result_) result_ = shiftRightOperator(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_LEFT);
    if (result_) {
      marker_.done(HAXE_SHIFT_OPERATOR);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // '>' '>'
  public static boolean shiftRightOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftRightOperator")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_SHIFT_RIGHT_OPERATOR);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // anonymousTypeFieldList (',' interfaceBody)?
  static boolean simpleAnonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleAnonymousTypeBody")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = anonymousTypeFieldList(builder_, level_ + 1);
    result_ = result_ && simpleAnonymousTypeBody_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' interfaceBody)?
  private static boolean simpleAnonymousTypeBody_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleAnonymousTypeBody_1")) return false;
    simpleAnonymousTypeBody_1_0(builder_, level_ + 1);
    return true;
  }

  // (',' interfaceBody)
  private static boolean simpleAnonymousTypeBody_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleAnonymousTypeBody_1_0")) return false;
    return simpleAnonymousTypeBody_1_0_0(builder_, level_ + 1);
  }

  // ',' interfaceBody
  private static boolean simpleAnonymousTypeBody_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleAnonymousTypeBody_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && interfaceBody(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpression *
  public static boolean simpleQualifiedReferenceExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleQualifiedReferenceExpression")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    int start_ = builder_.getCurrentOffset();
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = referenceExpression(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && simpleQualifiedReferenceExpression_1(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_REFERENCE_EXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_REFERENCE_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // qualifiedReferenceExpression *
  private static boolean simpleQualifiedReferenceExpression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "simpleQualifiedReferenceExpression_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!qualifiedReferenceExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "simpleQualifiedReferenceExpression_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // blockStatement | notBlockStatement
  static boolean statement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KBREAK) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, KCONTINUE) && !nextTokenIs(builder_, KDO)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, KFOR)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KRETURN) && !nextTokenIs(builder_, KSUPER)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KTHROW) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, KVAR) && !nextTokenIs(builder_, KWHILE)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = blockStatement(builder_, level_ + 1);
    if (!result_) result_ = notBlockStatement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // (statement ';'?)+
  static boolean statementList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, null);
    result_ = statementList_0(builder_, level_ + 1);
    int offset_ = builder_.getCurrentOffset();
    while (result_) {
      if (!statementList_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "statementList");
        break;
      }
      offset_ = next_offset_;
    }
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, statement_recovery_parser_);
    return result_;
  }

  // (statement ';'?)
  private static boolean statementList_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList_0")) return false;
    return statementList_0_0(builder_, level_ + 1);
  }

  // statement ';'?
  private static boolean statementList_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = statement(builder_, level_ + 1);
    result_ = result_ && statementList_0_0_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ';'?
  private static boolean statementList_0_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList_0_0_1")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // !('case' | 'default' | '}')
  static boolean statement_recovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !statement_recovery_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('case' | 'default' | '}')
  private static boolean statement_recovery_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery_0")) return false;
    return statement_recovery_0_0(builder_, level_ + 1);
  }

  // 'case' | 'default' | '}'
  private static boolean statement_recovery_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // ('--' | '++')
  public static boolean suffixExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpression")) return false;
    return suffixExpression_0(builder_, level_ + 1);
  }

  // '--' | '++'
  private static boolean suffixExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpression_0")) return false;
    if (!nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS_MINUS)
        && replaceVariants(builder_, 2, "<suffix expression>")) return false;
    boolean result_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "suffixExpression_0")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<suffix expression>");
    result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (result_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_SUFFIX_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // value suffixExpression*
  static boolean suffixExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, ID)
        && !nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, LITINT)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = value(builder_, level_ + 1);
    result_ = result_ && suffixExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // suffixExpression*
  private static boolean suffixExpressionWrapper_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpressionWrapper_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!suffixExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "suffixExpressionWrapper_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // 'super'
  public static boolean superExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "superExpression")) return false;
    if (!nextTokenIs(builder_, KSUPER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KSUPER);
    pinned_ = result_; // pin = 1
    if (result_ || pinned_) {
      marker_.done(HAXE_SUPER_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '{'  switchCase* defaultCase? '}'
  public static boolean switchBlock(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchBlock")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, PLCURLY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, switchBlock_1(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, switchBlock_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_SWITCH_BLOCK);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // switchCase*
  private static boolean switchBlock_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchBlock_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!switchCase(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "switchBlock_1");
        break;
      }
      offset_ = next_offset_;
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
  // ('case' expression (',' expression)* ':')+ switchCaseBlock?
  public static boolean switchCase(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<switch case>");
    result_ = switchCase_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && switchCase_1(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_SWITCH_CASE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_RECOVER_, switch_case_recover_parser_);
    return result_ || pinned_;
  }

  // ('case' expression (',' expression)* ':')+
  private static boolean switchCase_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = switchCase_0_0(builder_, level_ + 1);
    int offset_ = builder_.getCurrentOffset();
    while (result_) {
      if (!switchCase_0_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "switchCase_0");
        break;
      }
      offset_ = next_offset_;
    }
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ('case' expression (',' expression)* ':')
  private static boolean switchCase_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0")) return false;
    return switchCase_0_0_0(builder_, level_ + 1);
  }

  // 'case' expression (',' expression)* ':'
  private static boolean switchCase_0_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KCASE);
    result_ = result_ && expression(builder_, level_ + 1);
    result_ = result_ && switchCase_0_0_0_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOLON);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' expression)*
  private static boolean switchCase_0_0_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0_0_2")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!switchCase_0_0_0_2_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "switchCase_0_0_0_2");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' expression)
  private static boolean switchCase_0_0_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0_0_2_0")) return false;
    return switchCase_0_0_0_2_0_0(builder_, level_ + 1);
  }

  // ',' expression
  private static boolean switchCase_0_0_0_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCase_0_0_0_2_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && expression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
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
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KBREAK) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, KCONTINUE) && !nextTokenIs(builder_, KDO)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, KFOR)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KRETURN) && !nextTokenIs(builder_, KSUPER)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KTHROW) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, KVAR) && !nextTokenIs(builder_, KWHILE)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)
        && replaceVariants(builder_, 36, "<switch case block>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<switch case block>");
    result_ = switchCaseBlock_0(builder_, level_ + 1);
    if (!result_) result_ = statementList(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_SWITCH_CASE_BLOCK);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  // (literalExpression ";")
  private static boolean switchCaseBlock_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCaseBlock_0")) return false;
    return switchCaseBlock_0_0(builder_, level_ + 1);
  }

  // literalExpression ";"
  private static boolean switchCaseBlock_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchCaseBlock_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = literalExpression(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OSEMI);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'switch' expression switchBlock
  public static boolean switchStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchStatement")) return false;
    if (!nextTokenIs(builder_, KSWITCH)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KSWITCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && switchBlock(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_SWITCH_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // !('case' | 'default' | '}' | IDENTIFIER)
  static boolean switch_case_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switch_case_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !switch_case_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('case' | 'default' | '}' | IDENTIFIER)
  private static boolean switch_case_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switch_case_recover_0")) return false;
    return switch_case_recover_0_0(builder_, level_ + 1);
  }

  // 'case' | 'default' | '}' | IDENTIFIER
  private static boolean switch_case_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switch_case_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, IDENTIFIER);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '?' expression ':' ternaryExpressionWrapper
  public static boolean ternaryExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternaryExpression")) return false;
    if (!nextTokenIs(builder_, OQUEST)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "ternaryExpression")) return false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, OQUEST);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OCOLON)) && result_;
    result_ = pinned_ && ternaryExpressionWrapper(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_TERNARY_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // logicOrExpressionWrapper ternaryExpression?
  static boolean ternaryExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternaryExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = logicOrExpressionWrapper(builder_, level_ + 1);
    result_ = result_ && ternaryExpressionWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
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
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KTHIS);
    pinned_ = result_; // pin = 1
    if (result_ || pinned_) {
      marker_.done(HAXE_THIS_EXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'throw' expression ';'?
  public static boolean throwStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "throwStatement")) return false;
    if (!nextTokenIs(builder_, KTHROW)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KTHROW);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && throwStatement_2(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_THROW_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean throwStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "throwStatement_2")) return false;
    consumeToken(builder_, OSEMI);
    return true;
  }

  /* ********************************************************** */
  // importStatement | usingStatement | topLevelDeclaration
  static boolean topLevel(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevel")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, null);
    result_ = importStatement(builder_, level_ + 1);
    if (!result_) result_ = usingStatement(builder_, level_ + 1);
    if (!result_) result_ = topLevelDeclaration(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, top_level_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // classDeclaration
  //                               | externClassDeclaration
  //                               | interfaceDeclaration
  //                               | enumDeclaration
  //                               | typedefDeclaration
  static boolean topLevelDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevelDeclaration")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBIND)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KHACK)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KMETA) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, KCLASS) && !nextTokenIs(builder_, KENUM)
        && !nextTokenIs(builder_, KEXTERN) && !nextTokenIs(builder_, KINTERFACE)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KTYPEDEF)
        && !nextTokenIs(builder_, MACRO_ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = classDeclaration(builder_, level_ + 1);
    if (!result_) result_ = externClassDeclaration(builder_, level_ + 1);
    if (!result_) result_ = interfaceDeclaration(builder_, level_ + 1);
    if (!result_) result_ = enumDeclaration(builder_, level_ + 1);
    if (!result_) result_ = typedefDeclaration(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // topLevel*
  static boolean topLevelList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevelList")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!topLevel(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "topLevelList");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:keep' | '@:bind' | '@:bitmap' | '@:build' | '@:autoBuild' | '@:core_api' | '@:fakeEnum' | '@:final' | '@:hack' | '@:overload' | '@:macro' | '@:meta' | '@:native' | '@:ns' | '@:require' | 'class' | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef' | MACRO_ID)
  static boolean top_level_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !top_level_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:keep' | '@:bind' | '@:bitmap' | '@:build' | '@:autoBuild' | '@:core_api' | '@:fakeEnum' | '@:final' | '@:hack' | '@:overload' | '@:macro' | '@:meta' | '@:native' | '@:ns' | '@:require' | 'class' | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef' | MACRO_ID)
  private static boolean top_level_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover_0")) return false;
    return top_level_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:keep' | '@:bind' | '@:bitmap' | '@:build' | '@:autoBuild' | '@:core_api' | '@:fakeEnum' | '@:final' | '@:hack' | '@:overload' | '@:macro' | '@:meta' | '@:native' | '@:ns' | '@:require' | 'class' | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef' | MACRO_ID
  private static boolean top_level_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KBIND);
    if (!result_) result_ = consumeToken(builder_, KBITMAP);
    if (!result_) result_ = consumeToken(builder_, KBUILD);
    if (!result_) result_ = consumeToken(builder_, KAUTOBUILD);
    if (!result_) result_ = consumeToken(builder_, KCOREAPI);
    if (!result_) result_ = consumeToken(builder_, KFAKEENUM);
    if (!result_) result_ = consumeToken(builder_, KFINAL);
    if (!result_) result_ = consumeToken(builder_, KHACK);
    if (!result_) result_ = consumeToken(builder_, KOVERLOAD);
    if (!result_) result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KMETA);
    if (!result_) result_ = consumeToken(builder_, KNATIVE);
    if (!result_) result_ = consumeToken(builder_, KNS);
    if (!result_) result_ = consumeToken(builder_, KREQUIRE);
    if (!result_) result_ = consumeToken(builder_, KCLASS);
    if (!result_) result_ = consumeToken(builder_, KENUM);
    if (!result_) result_ = consumeToken(builder_, KEXTERN);
    if (!result_) result_ = consumeToken(builder_, KIMPORT);
    if (!result_) result_ = consumeToken(builder_, KUSING);
    if (!result_) result_ = consumeToken(builder_, KINTERFACE);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (!result_) result_ = consumeToken(builder_, KTYPEDEF);
    if (!result_) result_ = consumeToken(builder_, MACRO_ID);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'try' statement ';'? catchStatement*
  public static boolean tryStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement")) return false;
    if (!nextTokenIs(builder_, KTRY)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KTRY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, statement(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, tryStatement_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && tryStatement_3(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_TRY_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!catchStatement(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "tryStatement_3");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // referenceExpression qualifiedReferenceExpression* typeParam?
  public static boolean type(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = referenceExpression(builder_, level_ + 1);
    result_ = result_ && type_1(builder_, level_ + 1);
    result_ = result_ && type_2(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPE);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  // qualifiedReferenceExpression*
  private static boolean type_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "type_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!qualifiedReferenceExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "type_1");
        break;
      }
      offset_ = next_offset_;
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && type(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPE_EXTENDS);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // typeListPart (',' typeListPart)*
  public static boolean typeList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList")) return false;
    if (!nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, ID) && replaceVariants(builder_, 3, "<type list>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<type list>");
    result_ = typeListPart(builder_, level_ + 1);
    result_ = result_ && typeList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPE_LIST);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  // (',' typeListPart)*
  private static boolean typeList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!typeList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "typeList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' typeListPart)
  private static boolean typeList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_1_0")) return false;
    return typeList_1_0_0(builder_, level_ + 1);
  }

  // ',' typeListPart
  private static boolean typeList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && typeListPart(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // functionTypeWrapper
  public static boolean typeListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeListPart")) return false;
    if (!nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, ID) && replaceVariants(builder_, 3, "<type list part>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<type list part>");
    result_ = functionTypeWrapper(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPE_LIST_PART);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // type | anonymousType
  public static boolean typeOrAnonymous(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeOrAnonymous")) return false;
    if (!nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, ID)
        && replaceVariants(builder_, 2, "<type or anonymous>")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<type or anonymous>");
    result_ = type(builder_, level_ + 1);
    if (!result_) result_ = anonymousType(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPE_OR_ANONYMOUS);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_GENERAL_, null);
    return result_;
  }

  /* ********************************************************** */
  // '<' typeList '>'
  public static boolean typeParam(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParam")) return false;
    if (!nextTokenIs(builder_, OLESS)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OLESS);
    result_ = result_ && typeList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_TYPE_PARAM);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // ':' functionTypeWrapper
  public static boolean typeTag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeTag")) return false;
    if (!nextTokenIs(builder_, OCOLON)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOLON);
    result_ = result_ && functionTypeWrapper(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPE_TAG);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // externOrPrivate? 'typedef' componentName genericParam? '=' functionTypeWrapper ';'?
  public static boolean typedefDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typedefDeclaration")) return false;
    if (!nextTokenIs(builder_, KEXTERN) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KTYPEDEF) && replaceVariants(builder_, 3, "<typedef declaration>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<typedef declaration>");
    result_ = typedefDeclaration_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KTYPEDEF);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, typedefDeclaration_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OASSIGN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionTypeWrapper(builder_, level_ + 1)) && result_;
    result_ = pinned_ && typedefDeclaration_6(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_TYPEDEF_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
  // '>' '>' '>'
  public static boolean unsignedShiftRightOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "unsignedShiftRightOperator")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_UNSIGNED_SHIFT_RIGHT_OPERATOR);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'using' simpleQualifiedReferenceExpression ';'
  public static boolean usingStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "usingStatement")) return false;
    if (!nextTokenIs(builder_, KUSING)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KUSING);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, simpleQualifiedReferenceExpression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_USING_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
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
    if (!nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, ID)
        && !nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, LITINT)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, REG_EXP)) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = value_0(builder_, level_ + 1);
    if (!result_) result_ = value_1(builder_, level_ + 1);
    if (!result_) result_ = ifStatement(builder_, level_ + 1);
    if (!result_) result_ = value_3(builder_, level_ + 1);
    if (!result_) result_ = newExpressionOrCall(builder_, level_ + 1);
    if (!result_) result_ = parenthesizedExpressionOrCall(builder_, level_ + 1);
    if (!result_) result_ = callOrArrayAccess(builder_, level_ + 1);
    if (!result_) result_ = tryStatement(builder_, level_ + 1);
    if (!result_) result_ = switchStatement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // 'untyped' expression
  private static boolean value_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KUNTYPED);
    result_ = result_ && expression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (literalExpression qualifiedReferenceTail?)
  private static boolean value_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_1")) return false;
    return value_1_0(builder_, level_ + 1);
  }

  // literalExpression qualifiedReferenceTail?
  private static boolean value_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_1_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = literalExpression(builder_, level_ + 1);
    result_ = result_ && value_1_0_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // qualifiedReferenceTail?
  private static boolean value_1_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_1_0_1")) return false;
    qualifiedReferenceTail(builder_, level_ + 1);
    return true;
  }

  // castExpression qualifiedReferenceTail?
  private static boolean value_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_3")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = castExpression(builder_, level_ + 1);
    result_ = result_ && value_3_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // qualifiedReferenceTail?
  private static boolean value_3_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_3_1")) return false;
    qualifiedReferenceTail(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // macroMember* declarationAttributeList? 'var' varDeclarationPartList ';'
  public static boolean varDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclaration")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KDEBUG) && !nextTokenIs(builder_, KGETTER)
        && !nextTokenIs(builder_, KKEEP) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KPROTECTED) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, KSETTER) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KINLINE) && !nextTokenIs(builder_, KOVERRIDE)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KPUBLIC)
        && !nextTokenIs(builder_, KSTATIC) && !nextTokenIs(builder_, KVAR)
        && !nextTokenIs(builder_, MACRO_ID) && replaceVariants(builder_, 19, "<var declaration>")) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, "<var declaration>");
    result_ = varDeclaration_0(builder_, level_ + 1);
    result_ = result_ && varDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KVAR);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, varDeclarationPartList(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_VAR_DECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroMember*
  private static boolean varDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclaration_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroMember(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "varDeclaration_0");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // declarationAttributeList?
  private static boolean varDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclaration_1")) return false;
    declarationAttributeList(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // componentName propertyDeclaration? typeTag? varInit?
  public static boolean varDeclarationPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPart")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_, "<var declaration part>");
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_1(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_2(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_3(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_VAR_DECLARATION_PART);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_RECOVER_, var_declaration_part_recover_parser_);
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
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = varDeclarationPart(builder_, level_ + 1);
    result_ = result_ && varDeclarationPartList_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (',' varDeclarationPart)*
  private static boolean varDeclarationPartList_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPartList_1")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!varDeclarationPartList_1_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "varDeclarationPartList_1");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (',' varDeclarationPart)
  private static boolean varDeclarationPartList_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPartList_1_0")) return false;
    return varDeclarationPartList_1_0_0(builder_, level_ + 1);
  }

  // ',' varDeclarationPart
  private static boolean varDeclarationPartList_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclarationPartList_1_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOMMA);
    result_ = result_ && varDeclarationPart(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '=' expression
  public static boolean varInit(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varInit")) return false;
    if (!nextTokenIs(builder_, OASSIGN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, OASSIGN);
    pinned_ = result_; // pin = 1
    result_ = result_ && expression(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_VAR_INIT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // !(';' | ',')
  static boolean var_declaration_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "var_declaration_part_recover")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_, null);
    result_ = !var_declaration_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, level_, result_, false, _SECTION_NOT_, null);
    return result_;
  }

  // (';' | ',')
  private static boolean var_declaration_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "var_declaration_part_recover_0")) return false;
    return var_declaration_part_recover_0_0(builder_, level_ + 1);
  }

  // ';' | ','
  private static boolean var_declaration_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "var_declaration_part_recover_0_0")) return false;
    boolean result_ = false;
    Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OSEMI);
    if (!result_) result_ = consumeToken(builder_, OCOMMA);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'while' '(' expression ')' statement ';'?
  public static boolean whileStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whileStatement")) return false;
    if (!nextTokenIs(builder_, KWHILE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_, null);
    result_ = consumeToken(builder_, KWHILE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && whileStatement_5(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_WHILE_STATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, level_, result_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ';'?
  private static boolean whileStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whileStatement_5")) return false;
    consumeToken(builder_, OSEMI);
    return true;
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
