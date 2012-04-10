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
  public ASTNode parse(final IElementType root_, final PsiBuilder builder_) {
    int level_ = 0;
    boolean result_;
    if (root_ == HAXE_ACCESS) {
      result_ = access(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ADDITIVEEXPRESSION) {
      result_ = additiveExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUSTYPE) {
      result_ = anonymousType(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUSTYPEBODY) {
      result_ = anonymousTypeBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUSTYPEFIELD) {
      result_ = anonymousTypeField(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ANONYMOUSTYPEFIELDLIST) {
      result_ = anonymousTypeFieldList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ARRAYACCESSEXPRESSION) {
      result_ = arrayAccessExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ARRAYLITERAL) {
      result_ = arrayLiteral(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ASSIGNEXPRESSION) {
      result_ = assignExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ASSIGNOPERATION) {
      result_ = assignOperation(builder_, level_ + 1);
    }
    else if (root_ == HAXE_AUTOBUILDMACRO) {
      result_ = autoBuildMacro(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BITOPERATION) {
      result_ = bitOperation(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BITMAPMETA) {
      result_ = bitmapMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BITWISEEXPRESSION) {
      result_ = bitwiseExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BLOCKSTATEMENT) {
      result_ = blockStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BREAKSTATEMENT) {
      result_ = breakStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_BUILDMACRO) {
      result_ = buildMacro(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CALLEXPRESSION) {
      result_ = callExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CASESTATEMENT) {
      result_ = caseStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CASTEXPRESSION) {
      result_ = castExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CATCHEXPRESSION) {
      result_ = catchExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CATCHSTATEMENT) {
      result_ = catchStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CLASSBODY) {
      result_ = classBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CLASSDECLARATION) {
      result_ = classDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_COMPAREEXPRESSION) {
      result_ = compareExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_COMPAREOPERATION) {
      result_ = compareOperation(builder_, level_ + 1);
    }
    else if (root_ == HAXE_COMPONENTNAME) {
      result_ = componentName(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CONTINUESTATEMENT) {
      result_ = continueStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_CUSTOMMETA) {
      result_ = customMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DECLARATIONATTRIBUTE) {
      result_ = declarationAttribute(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DECLARATIONATTRIBUTELIST) {
      result_ = declarationAttributeList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DEFAULTSTATEMENT) {
      result_ = defaultStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_DOWHILESTATEMENT) {
      result_ = doWhileStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUMBODY) {
      result_ = enumBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUMCONSTRUCTORPARAMETERS) {
      result_ = enumConstructorParameters(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUMDECLARATION) {
      result_ = enumDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ENUMVALUEDECLARATION) {
      result_ = enumValueDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXPRESSION) {
      result_ = expression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXPRESSIONLIST) {
      result_ = expressionList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERNCLASSDECLARATION) {
      result_ = externClassDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERNCLASSDECLARATIONBODY) {
      result_ = externClassDeclarationBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERNFUNCTIONDECLARATION) {
      result_ = externFunctionDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_EXTERNORPRIVATE) {
      result_ = externOrPrivate(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FAKEENUMMETA) {
      result_ = fakeEnumMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FORSTATEMENT) {
      result_ = forStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTIONDECLARATIONWITHATTRIBUTES) {
      result_ = functionDeclarationWithAttributes(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTIONLITERAL) {
      result_ = functionLiteral(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTIONPROTOTYPEDECLARATIONWITHATTRIBUTES) {
      result_ = functionPrototypeDeclarationWithAttributes(builder_, level_ + 1);
    }
    else if (root_ == HAXE_FUNCTIONTYPE) {
      result_ = functionType(builder_, level_ + 1);
    }
    else if (root_ == HAXE_GENERICLISTPART) {
      result_ = genericListPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_GENERICPARAM) {
      result_ = genericParam(builder_, level_ + 1);
    }
    else if (root_ == HAXE_GETTERMETA) {
      result_ = getterMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_IDENTIFIER) {
      result_ = identifier(builder_, level_ + 1);
    }
    else if (root_ == HAXE_IFEXPRESSION) {
      result_ = ifExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_IFSTATEMENT) {
      result_ = ifStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_IMPORTSTATEMENT) {
      result_ = importStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INHERIT) {
      result_ = inherit(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INHERITLIST) {
      result_ = inheritList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INTERFACEBODY) {
      result_ = interfaceBody(builder_, level_ + 1);
    }
    else if (root_ == HAXE_INTERFACEDECLARATION) {
      result_ = interfaceDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ITERABLE) {
      result_ = iterable(builder_, level_ + 1);
    }
    else if (root_ == HAXE_ITERATOREXPRESSION) {
      result_ = iteratorExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LITERALEXPRESSION) {
      result_ = literalExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOCALFUNCTIONDECLARATION) {
      result_ = localFunctionDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOCALVARDECLARATION) {
      result_ = localVarDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOCALVARDECLARATIONPART) {
      result_ = localVarDeclarationPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOGICANDEXPRESSION) {
      result_ = logicAndExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_LOGICOREXPRESSION) {
      result_ = logicOrExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_METAKEYVALUE) {
      result_ = metaKeyValue(builder_, level_ + 1);
    }
    else if (root_ == HAXE_METAMETA) {
      result_ = metaMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_MULTIPLICATIVEEXPRESSION) {
      result_ = multiplicativeExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_NATIVEMETA) {
      result_ = nativeMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_NEWEXPRESSION) {
      result_ = newExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_NSMETA) {
      result_ = nsMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_OBJECTLITERAL) {
      result_ = objectLiteral(builder_, level_ + 1);
    }
    else if (root_ == HAXE_OBJECTLITERALELEMENT) {
      result_ = objectLiteralElement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PACKAGESTATEMENT) {
      result_ = packageStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PARAMETER) {
      result_ = parameter(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PARAMETERLIST) {
      result_ = parameterList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PARENTHESIZEDEXPRESSION) {
      result_ = parenthesizedExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PP) {
      result_ = pp(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PPELSE) {
      result_ = ppElse(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PPELSEIF) {
      result_ = ppElseIf(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PPEND) {
      result_ = ppEnd(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PPERROR) {
      result_ = ppError(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PPIF) {
      result_ = ppIf(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PPIFVALUE) {
      result_ = ppIfValue(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PREFIXEXPRESSION) {
      result_ = prefixExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PROPERTYACCESSOR) {
      result_ = propertyAccessor(builder_, level_ + 1);
    }
    else if (root_ == HAXE_PROPERTYDECLARATION) {
      result_ = propertyDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_REFERENCEEXPRESSION) {
      result_ = referenceExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_REQUIREMETA) {
      result_ = requireMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_RETURNSTATEMENT) {
      result_ = returnStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_RETURNSTATEMENTWITHOUTSEMICOLON) {
      result_ = returnStatementWithoutSemicolon(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SETTERMETA) {
      result_ = setterMeta(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SHIFTEXPRESSION) {
      result_ = shiftExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SHIFTOPERATOR) {
      result_ = shiftOperator(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SHIFTRIGHTOPERATOR) {
      result_ = shiftRightOperator(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SUFFIXEXPRESSION) {
      result_ = suffixExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SUPEREXPRESSION) {
      result_ = superExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SWITCHEXPRESSION) {
      result_ = switchExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_SWITCHSTATEMENT) {
      result_ = switchStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TERNARYEXPRESSION) {
      result_ = ternaryExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_THISEXPRESSION) {
      result_ = thisExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_THROWSTATEMENT) {
      result_ = throwStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TRYEXPRESSION) {
      result_ = tryExpression(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TRYSTATEMENT) {
      result_ = tryStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPE) {
      result_ = type(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPEEXTENDS) {
      result_ = typeExtends(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPELIST) {
      result_ = typeList(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPELISTPART) {
      result_ = typeListPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPEPARAM) {
      result_ = typeParam(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPETAG) {
      result_ = typeTag(builder_, level_ + 1);
    }
    else if (root_ == HAXE_TYPEDEFDECLARATION) {
      result_ = typedefDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_UNSIGNEDSHIFTRIGHTOPERATOR) {
      result_ = unsignedShiftRightOperator(builder_, level_ + 1);
    }
    else if (root_ == HAXE_USINGSTATEMENT) {
      result_ = usingStatement(builder_, level_ + 1);
    }
    else if (root_ == HAXE_VARDECLARATION) {
      result_ = varDeclaration(builder_, level_ + 1);
    }
    else if (root_ == HAXE_VARDECLARATIONPART) {
      result_ = varDeclarationPart(builder_, level_ + 1);
    }
    else if (root_ == HAXE_VARINIT) {
      result_ = varInit(builder_, level_ + 1);
    }
    else if (root_ == HAXE_WHILESTATEMENT) {
      result_ = whileStatement(builder_, level_ + 1);
    }
    else {
      Marker marker_ = builder_.mark();
      result_ = haxeFile(builder_, level_ + 1);
      while (builder_.getTokenType() != null) {
        builder_.advanceLexer();
      }
      marker_.done(root_);
    }
    return builder_.getTreeBuilt();
  }

  private static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    TokenSet.create(HAXE_ADDITIVEEXPRESSION, HAXE_ARRAYACCESSEXPRESSION, HAXE_ARRAYLITERAL, HAXE_ASSIGNEXPRESSION,
      HAXE_BITWISEEXPRESSION, HAXE_CALLEXPRESSION, HAXE_CASTEXPRESSION, HAXE_CATCHEXPRESSION,
      HAXE_COMPAREEXPRESSION, HAXE_EXPRESSION, HAXE_FUNCTIONLITERAL, HAXE_IFEXPRESSION,
      HAXE_ITERATOREXPRESSION, HAXE_LITERALEXPRESSION, HAXE_LOGICANDEXPRESSION, HAXE_LOGICOREXPRESSION,
      HAXE_MULTIPLICATIVEEXPRESSION, HAXE_NEWEXPRESSION, HAXE_OBJECTLITERAL, HAXE_PARENTHESIZEDEXPRESSION,
      HAXE_PREFIXEXPRESSION, HAXE_REFERENCEEXPRESSION, HAXE_SHIFTEXPRESSION, HAXE_SUFFIXEXPRESSION,
      HAXE_SUPEREXPRESSION, HAXE_SWITCHEXPRESSION, HAXE_TERNARYEXPRESSION, HAXE_THISEXPRESSION,
      HAXE_TRYEXPRESSION),
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
    if (!nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KPUBLIC)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KPUBLIC);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (result_) {
      marker_.done(HAXE_ACCESS);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // ('+' | '-') multiplicativeExpressionWrapper
  public static boolean additiveExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "additiveExpression")) return false;
    if (!nextTokenIs(builder_, OPLUS) && !nextTokenIs(builder_, OMINUS)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "additiveExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = additiveExpression_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && multiplicativeExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ADDITIVEEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
  // '{' anonymousTypeBody '}'
  public static boolean anonymousType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousType")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PLCURLY);
    result_ = result_ && anonymousTypeBody(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRCURLY);
    pinned_ = result_; // pin = 3
    if (result_ || pinned_) {
      marker_.done(HAXE_ANONYMOUSTYPE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // extendedAnonymousTypeBody | simpleAnonymousTypeBody | interfaceBody
  public static boolean anonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeBody")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = extendedAnonymousTypeBody(builder_, level_ + 1);
    if (!result_) result_ = simpleAnonymousTypeBody(builder_, level_ + 1);
    if (!result_) result_ = interfaceBody(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ANONYMOUSTYPEBODY);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // componentName typeTag
  public static boolean anonymousTypeField(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "anonymousTypeField")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && typeTag(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ANONYMOUSTYPEFIELD);
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
    final Marker marker_ = builder_.mark();
    result_ = anonymousTypeField(builder_, level_ + 1);
    result_ = result_ && anonymousTypeFieldList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ANONYMOUSTYPEFIELDLIST);
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
    final Marker marker_ = builder_.mark();
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
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "arrayAccessExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PLBRACK);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, arrayAccessExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRBRACK) && result_;
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ARRAYACCESSEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLBRACK);
    result_ = result_ && arrayLiteral_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRBRACK);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_ARRAYLITERAL)) {
      marker_.drop();
    }
    else if (result_) {
      marker_.done(HAXE_ARRAYLITERAL);
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
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "assignExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = assignOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && iteratorExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ASSIGNEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // iteratorExpressionWrapper assignExpression*
  static boolean assignExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "assignExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
      marker_.done(HAXE_ASSIGNOPERATION);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:autoBuild' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean autoBuildMacro(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "autoBuildMacro")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KAUTOBUILD);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, referenceExpression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, autoBuildMacro_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_AUTOBUILDMACRO);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, OBIT_OR) && !nextTokenIs(builder_, OBIT_XOR)
        && !nextTokenIs(builder_, OBIT_AND)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OBIT_OR);
    if (!result_) result_ = consumeToken(builder_, OBIT_AND);
    if (!result_) result_ = consumeToken(builder_, OBIT_XOR);
    if (result_) {
      marker_.done(HAXE_BITOPERATION);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '@:bitmap' '(' LITSTRING ')'
  public static boolean bitmapMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitmapMeta")) return false;
    if (!nextTokenIs(builder_, KBITMAP)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KBITMAP);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, LITSTRING)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_BITMAPMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // bitOperation shiftExpressionWrapper
  public static boolean bitwiseExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpression")) return false;
    if (!nextTokenIs(builder_, OBIT_OR) && !nextTokenIs(builder_, OBIT_XOR)
        && !nextTokenIs(builder_, OBIT_AND)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "bitwiseExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = bitOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && shiftExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_BITWISEEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // shiftExpressionWrapper bitwiseExpression*
  static boolean bitwiseExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "bitwiseExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PLCURLY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, blockStatement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_BLOCKSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KBREAK);
    pinned_ = result_; // pin = 1
    result_ = result_ && consumeToken(builder_, OSEMI);
    if (result_ || pinned_) {
      marker_.done(HAXE_BREAKSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:build' '(' referenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)* ')'
  public static boolean buildMacro(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "buildMacro")) return false;
    if (!nextTokenIs(builder_, KBUILD)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KBUILD);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, referenceExpression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, buildMacro_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_BUILDMACRO);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "callExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, callExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_CALLEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // 'case' expressionList ':'
  public static boolean caseStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "caseStatement")) return false;
    if (!nextTokenIs(builder_, KCASE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KCASE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expressionList(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OCOLON) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_CASESTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'cast' (('(' expression ',' functionTypeWrapper ')')  | expression)
  public static boolean castExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "castExpression")) return false;
    if (!nextTokenIs(builder_, KCAST)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KCAST);
    pinned_ = result_; // pin = 1
    result_ = result_ && castExpression_1(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_CASTEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_CASTEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // 'catch' '(' parameter ')' expression
  public static boolean catchExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "catchExpression")) return false;
    if (!nextTokenIs(builder_, KCATCH)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KCATCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, parameter(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && expression(builder_, level_ + 1) && result_;
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_CATCHEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_CATCHEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'catch' '(' parameter ')' blockStatement
  public static boolean catchStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "catchStatement")) return false;
    if (!nextTokenIs(builder_, KCATCH)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KCATCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, parameter(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && blockStatement(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_CATCHSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // classBodyPart*
  public static boolean classBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classBody")) return false;
    final Marker marker_ = builder_.mark();
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
    marker_.done(HAXE_CLASSBODY);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionDeclarationWithAttributes | pp
  static boolean classBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classBodyPart")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = functionDeclarationWithAttributes(builder_, level_ + 1);
    if (!result_) result_ = pp(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, class_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // macroClass* 'private'? 'class' componentName genericParam? inheritList? '{' classBody '}'
  public static boolean classDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "classDeclaration")) return false;
    if (!nextTokenIs(builder_, KBIND) && !nextTokenIs(builder_, KMETA)
        && !nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNATIVE) && !nextTokenIs(builder_, KCLASS)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, MACRO_ID)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KCOREAPI)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KHACK)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
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
      marker_.done(HAXE_CLASSDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  static boolean class_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !class_body_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  private static boolean class_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover_0")) return false;
    return class_body_part_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID
  private static boolean class_body_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "class_body_part_recover_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, OEQ) && !nextTokenIs(builder_, ONOT_EQ)
        && !nextTokenIs(builder_, OGREATER_OR_EQUAL) && !nextTokenIs(builder_, OLESS)
        && !nextTokenIs(builder_, OLESS_OR_EQUAL) && !nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "compareExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = compareOperation(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && bitwiseExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_COMPAREEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // bitwiseExpressionWrapper compareExpression*
  static boolean compareExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "compareExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, OEQ) && !nextTokenIs(builder_, ONOT_EQ)
        && !nextTokenIs(builder_, OGREATER_OR_EQUAL) && !nextTokenIs(builder_, OLESS)
        && !nextTokenIs(builder_, OLESS_OR_EQUAL) && !nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OEQ);
    if (!result_) result_ = consumeToken(builder_, ONOT_EQ);
    if (!result_) result_ = consumeToken(builder_, OLESS_OR_EQUAL);
    if (!result_) result_ = consumeToken(builder_, OLESS);
    if (!result_) result_ = consumeToken(builder_, OGREATER);
    if (!result_) result_ = consumeToken(builder_, OGREATER_OR_EQUAL);
    if (result_) {
      marker_.done(HAXE_COMPAREOPERATION);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // identifier
  public static boolean componentName(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "componentName")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = identifier(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_COMPONENTNAME);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KCONTINUE);
    pinned_ = result_; // pin = 1
    result_ = result_ && consumeToken(builder_, OSEMI);
    if (result_ || pinned_) {
      marker_.done(HAXE_CONTINUESTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // MACRO_ID ('(' customMetaLiterals ')')?
  public static boolean customMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "customMeta")) return false;
    if (!nextTokenIs(builder_, MACRO_ID)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, MACRO_ID);
    pinned_ = result_; // pin = 1
    result_ = result_ && customMeta_1(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_CUSTOMMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, LITFLOAT)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KINLINE) && !nextTokenIs(builder_, KOVERRIDE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KSTATIC)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KSTATIC);
    if (!result_) result_ = consumeToken(builder_, KINLINE);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KOVERRIDE);
    if (!result_) result_ = access(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_DECLARATIONATTRIBUTE);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // declarationAttribute+
  public static boolean declarationAttributeList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "declarationAttributeList")) return false;
    if (!nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KINLINE) && !nextTokenIs(builder_, KOVERRIDE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KSTATIC)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
      marker_.done(HAXE_DECLARATIONATTRIBUTELIST);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'default' ':'
  public static boolean defaultStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "defaultStatement")) return false;
    if (!nextTokenIs(builder_, KDEFAULT)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KDEFAULT);
    pinned_ = result_; // pin = 1
    result_ = result_ && consumeToken(builder_, OCOLON);
    if (result_ || pinned_) {
      marker_.done(HAXE_DEFAULTSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'do' statement 'while' '(' expression ')'
  public static boolean doWhileStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doWhileStatement")) return false;
    if (!nextTokenIs(builder_, KDO)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KDO);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, statement(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, KWHILE)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_DOWHILESTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // (pp | enumValueDeclaration)*
  public static boolean enumBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumBody")) return false;
    final Marker marker_ = builder_.mark();
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!enumBody_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "enumBody");
        break;
      }
      offset_ = next_offset_;
    }
    marker_.done(HAXE_ENUMBODY);
    return true;
  }

  // (pp | enumValueDeclaration)
  private static boolean enumBody_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumBody_0")) return false;
    return enumBody_0_0(builder_, level_ + 1);
  }

  // pp | enumValueDeclaration
  private static boolean enumBody_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumBody_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = pp(builder_, level_ + 1);
    if (!result_) result_ = enumValueDeclaration(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // '(' parameterList? ')'
  public static boolean enumConstructorParameters(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumConstructorParameters")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, enumConstructorParameters_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_ENUMCONSTRUCTORPARAMETERS);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    if (!nextTokenIs(builder_, KBIND) && !nextTokenIs(builder_, KMETA)
        && !nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KENUM)
        && !nextTokenIs(builder_, KMACRO) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KEXTERN) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, MACRO_ID) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KBITMAP)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KAUTOBUILD)
        && !nextTokenIs(builder_, KHACK)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
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
      marker_.done(HAXE_ENUMDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && enumValueDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OSEMI);
    if (result_) {
      marker_.done(HAXE_ENUMVALUEDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, enum_value_declaration_recovery_parser_);
    return result_;
  }

  // enumConstructorParameters?
  private static boolean enumValueDeclaration_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enumValueDeclaration_1")) return false;
    enumConstructorParameters(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | ID | '}')
  static boolean enum_value_declaration_recovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enum_value_declaration_recovery")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !enum_value_declaration_recovery_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | ID | '}')
  private static boolean enum_value_declaration_recovery_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enum_value_declaration_recovery_0")) return false;
    return enum_value_declaration_recovery_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | ID | '}'
  private static boolean enum_value_declaration_recovery_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "enum_value_declaration_recovery_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, ID);
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
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
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
    return result_;
  }

  /* ********************************************************** */
  // expression (',' expression)*
  public static boolean expressionList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expressionList")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && expressionList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_EXPRESSIONLIST);
    }
    else {
      marker_.rollbackTo();
    }
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
    final Marker marker_ = builder_.mark();
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
  // typeExtends (',' anonymousTypeFieldList)? (',' interfaceBody)?
  static boolean extendedAnonymousTypeBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extendedAnonymousTypeBody")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // macroClass* 'extern' 'class' componentName genericParam? inheritList? '{' externClassDeclarationBody '}'
  public static boolean externClassDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclaration")) return false;
    if (!nextTokenIs(builder_, KBIND) && !nextTokenIs(builder_, KMETA)
        && !nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNATIVE) && !nextTokenIs(builder_, KEXTERN)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, MACRO_ID)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KCOREAPI)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KAUTOBUILD)
        && !nextTokenIs(builder_, KHACK)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = externClassDeclaration_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KEXTERN);
    result_ = result_ && consumeToken(builder_, KCLASS);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, externClassDeclaration_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, externClassDeclaration_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLCURLY)) && result_;
    result_ = pinned_ && report_error_(builder_, externClassDeclarationBody(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRCURLY) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_EXTERNCLASSDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    marker_.done(HAXE_EXTERNCLASSDECLARATIONBODY);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | externFunctionDeclaration | pp
  static boolean externClassDeclarationBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externClassDeclarationBodyPart")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = externFunctionDeclaration(builder_, level_ + 1);
    if (!result_) result_ = pp(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, extern_class_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // macroMember* declarationAttributeList? 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? (functionCommonBody | ';')
  public static boolean externFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KSETTER) && !nextTokenIs(builder_, KINLINE)
        && !nextTokenIs(builder_, KMACRO) && !nextTokenIs(builder_, KKEEP)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, KOVERRIDE) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, MACRO_ID) && !nextTokenIs(builder_, KDEBUG)
        && !nextTokenIs(builder_, KPROTECTED) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KSTATIC) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KAUTOBUILD)
        && !nextTokenIs(builder_, KGETTER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
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
      marker_.done(HAXE_EXTERNFUNCTIONDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroMember*
  private static boolean externFunctionDeclaration_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "externFunctionDeclaration_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroMember(builder_, level_ + 1)) break;
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, KEXTERN) && !nextTokenIs(builder_, KPRIVATE)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KEXTERN);
    if (!result_) result_ = consumeToken(builder_, KPRIVATE);
    if (result_) {
      marker_.done(HAXE_EXTERNORPRIVATE);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  static boolean extern_class_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !extern_class_body_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  private static boolean extern_class_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover_0")) return false;
    return extern_class_body_part_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID
  private static boolean extern_class_body_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "extern_class_body_part_recover_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KFAKEENUM);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, type(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_FAKEENUMMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'for' '(' componentName 'in' iterable')' statement
  public static boolean forStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "forStatement")) return false;
    if (!nextTokenIs(builder_, KFOR)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KFOR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, componentName(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OIN)) && result_;
    result_ = pinned_ && report_error_(builder_, iterable(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && statement(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_FORSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // blockStatement | returnStatementWithoutSemicolon | expression
  static boolean functionCommonBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionCommonBody")) return false;
    if (!nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, OMINUS_MINUS)
        && !nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OMINUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITINT)
        && !nextTokenIs(builder_, KRETURN) && !nextTokenIs(builder_, OPLUS_PLUS)
        && !nextTokenIs(builder_, KTRY) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, OCOMPLEMENT)
        && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = blockStatement(builder_, level_ + 1);
    if (!result_) result_ = returnStatementWithoutSemicolon(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // macroMember* declarationAttributeList? 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean functionDeclarationWithAttributes(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes")) return false;
    if (!nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KSETTER) && !nextTokenIs(builder_, KINLINE)
        && !nextTokenIs(builder_, KMACRO) && !nextTokenIs(builder_, KKEEP)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, KOVERRIDE) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, MACRO_ID) && !nextTokenIs(builder_, KDEBUG)
        && !nextTokenIs(builder_, KPROTECTED) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KSTATIC) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KAUTOBUILD)
        && !nextTokenIs(builder_, KGETTER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
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
      marker_.done(HAXE_FUNCTIONDECLARATIONWITHATTRIBUTES);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroMember*
  private static boolean functionDeclarationWithAttributes_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionDeclarationWithAttributes_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroMember(builder_, level_ + 1)) break;
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KFUNCTION);
    result_ = result_ && consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, functionLiteral_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionLiteral_4(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, functionLiteral_5(builder_, level_ + 1)) && result_;
    result_ = pinned_ && functionCommonBody(builder_, level_ + 1) && result_;
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_FUNCTIONLITERAL)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_FUNCTIONLITERAL);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
  // macroMember* declarationAttributeList? 'function' ('new' | componentName genericParam?) '(' parameterList? ')' typeTag? 'untyped'? ';'
  public static boolean functionPrototypeDeclarationWithAttributes(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes")) return false;
    if (!nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KSETTER) && !nextTokenIs(builder_, KINLINE)
        && !nextTokenIs(builder_, KMACRO) && !nextTokenIs(builder_, KKEEP)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, KOVERRIDE) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, MACRO_ID) && !nextTokenIs(builder_, KDEBUG)
        && !nextTokenIs(builder_, KPROTECTED) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KSTATIC) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KAUTOBUILD)
        && !nextTokenIs(builder_, KGETTER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
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
      marker_.done(HAXE_FUNCTIONPROTOTYPEDECLARATIONWITHATTRIBUTES);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // macroMember*
  private static boolean functionPrototypeDeclarationWithAttributes_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionPrototypeDeclarationWithAttributes_0")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!macroMember(builder_, level_ + 1)) break;
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // '->' (type | anonymousType)
  public static boolean functionType(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionType")) return false;
    if (!nextTokenIs(builder_, OARROW)) return false;
    boolean result_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "functionType")) return false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OARROW);
    result_ = result_ && functionType_1(builder_, level_ + 1);
    if (result_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_FUNCTIONTYPE);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  // (type | anonymousType)
  private static boolean functionType_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionType_1")) return false;
    return functionType_1_0(builder_, level_ + 1);
  }

  // type | anonymousType
  private static boolean functionType_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionType_1_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = type(builder_, level_ + 1);
    if (!result_) result_ = anonymousType(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // (type | anonymousType) functionType*
  static boolean functionTypeWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeWrapper")) return false;
    if (!nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = functionTypeWrapper_0(builder_, level_ + 1);
    result_ = result_ && functionTypeWrapper_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (type | anonymousType)
  private static boolean functionTypeWrapper_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeWrapper_0")) return false;
    return functionTypeWrapper_0_0(builder_, level_ + 1);
  }

  // type | anonymousType
  private static boolean functionTypeWrapper_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "functionTypeWrapper_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = type(builder_, level_ + 1);
    if (!result_) result_ = anonymousType(builder_, level_ + 1);
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
  // componentName (':' '(' typeList ')')?
  public static boolean genericListPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && genericListPart_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_GENERICLISTPART);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  // (':' '(' typeList ')')?
  private static boolean genericListPart_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1")) return false;
    genericListPart_1_0(builder_, level_ + 1);
    return true;
  }

  // (':' '(' typeList ')')
  private static boolean genericListPart_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0")) return false;
    return genericListPart_1_0_0(builder_, level_ + 1);
  }

  // ':' '(' typeList ')'
  private static boolean genericListPart_1_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "genericListPart_1_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOLON);
    result_ = result_ && consumeToken(builder_, PLPAREN);
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
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OLESS);
    result_ = result_ && genericListPart(builder_, level_ + 1);
    result_ = result_ && genericParam_2(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_GENERICPARAM);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KGETTER);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, referenceExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_GETTERMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // packageStatement? topLevelList
  static boolean haxeFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "haxeFile")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // 'if' '(' expression ')' expression ('else' expression)?
  public static boolean ifExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifExpression")) return false;
    if (!nextTokenIs(builder_, KIF)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KIF);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && ifExpression_5(builder_, level_ + 1) && result_;
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_IFEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_IFEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ('else' expression)?
  private static boolean ifExpression_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifExpression_5")) return false;
    ifExpression_5_0(builder_, level_ + 1);
    return true;
  }

  // ('else' expression)
  private static boolean ifExpression_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifExpression_5_0")) return false;
    return ifExpression_5_0_0(builder_, level_ + 1);
  }

  // 'else' expression
  private static boolean ifExpression_5_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifExpression_5_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KELSE);
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
  // 'if' '(' expression ')' statement ('else' statement)?
  public static boolean ifStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement")) return false;
    if (!nextTokenIs(builder_, KIF)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KIF);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, statement(builder_, level_ + 1)) && result_;
    result_ = pinned_ && ifStatement_5(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_IFSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // ('else' statement)?
  private static boolean ifStatement_5(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_5")) return false;
    ifStatement_5_0(builder_, level_ + 1);
    return true;
  }

  // ('else' statement)
  private static boolean ifStatement_5_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_5_0")) return false;
    return ifStatement_5_0_0(builder_, level_ + 1);
  }

  // 'else' statement
  private static boolean ifStatement_5_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ifStatement_5_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KELSE);
    result_ = result_ && statement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'import' simpleQualifiedReferenceExpression ';'
  public static boolean importStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "importStatement")) return false;
    if (!nextTokenIs(builder_, KIMPORT)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KIMPORT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, simpleQualifiedReferenceExpression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_IMPORTSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // ('extends' | 'implements') type
  public static boolean inherit(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit")) return false;
    if (!nextTokenIs(builder_, KIMPLEMENTS) && !nextTokenIs(builder_, KEXTENDS)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = inherit_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && type(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_INHERIT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, KIMPLEMENTS) && !nextTokenIs(builder_, KEXTENDS)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = inherit(builder_, level_ + 1);
    result_ = result_ && inheritList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_INHERITLIST);
    }
    else {
      marker_.rollbackTo();
    }
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
    final Marker marker_ = builder_.mark();
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
  // interfaceBodyPart*
  public static boolean interfaceBody(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceBody")) return false;
    final Marker marker_ = builder_.mark();
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
    marker_.done(HAXE_INTERFACEBODY);
    return true;
  }

  /* ********************************************************** */
  // varDeclaration | functionPrototypeDeclarationWithAttributes | pp
  static boolean interfaceBodyPart(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceBodyPart")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = varDeclaration(builder_, level_ + 1);
    if (!result_) result_ = functionPrototypeDeclarationWithAttributes(builder_, level_ + 1);
    if (!result_) result_ = pp(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, interface_body_part_recover_parser_);
    return result_;
  }

  /* ********************************************************** */
  // macroClass* externOrPrivate? 'interface' componentName genericParam? inheritList? '{' interfaceBody '}'
  public static boolean interfaceDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interfaceDeclaration")) return false;
    if (!nextTokenIs(builder_, KBIND) && !nextTokenIs(builder_, KMETA)
        && !nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNATIVE) && !nextTokenIs(builder_, KEXTERN)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, MACRO_ID)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KCOREAPI)
        && !nextTokenIs(builder_, KBITMAP) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KINTERFACE) && !nextTokenIs(builder_, KAUTOBUILD)
        && !nextTokenIs(builder_, KHACK)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
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
      marker_.done(HAXE_INTERFACEDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  static boolean interface_body_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !interface_body_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID)
  private static boolean interface_body_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover_0")) return false;
    return interface_body_part_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:build' | '@:autoBuild' | '@:debug' | '@:getter' | '@:keep' | '@:macro' | '@:nodebug' | '@:ns' | '@:protected' | '@:require' | '@:setter' | 'dynamic' | 'function' | 'inline' | 'override' | 'private' | 'public' | 'static' | 'var' | '}' | MACRO_ID
  private static boolean interface_body_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "interface_body_part_recover_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = expression(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_ITERABLE);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '...' ternaryExpressionWrapper
  public static boolean iteratorExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpression")) return false;
    if (!nextTokenIs(builder_, OTRIPLE_DOT)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "iteratorExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, OTRIPLE_DOT);
    pinned_ = result_; // pin = 1
    result_ = result_ && ternaryExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_ITERATOREXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // ternaryExpressionWrapper iteratorExpression?
  static boolean iteratorExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "iteratorExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
  //                     | 'null' | 'true' | 'false'
  //                     | functionLiteral
  //                     | arrayLiteral
  //                     | objectLiteral
  public static boolean literalExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literalExpression")) return false;
    if (!nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, KFALSE)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, LITFLOAT)) return false;
    boolean result_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITSTRING);
    if (!result_) result_ = consumeToken(builder_, LITCHAR);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KTRUE);
    if (!result_) result_ = consumeToken(builder_, KFALSE);
    if (!result_) result_ = functionLiteral(builder_, level_ + 1);
    if (!result_) result_ = arrayLiteral(builder_, level_ + 1);
    if (!result_) result_ = objectLiteral(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_LITERALEXPRESSION)) {
      marker_.drop();
    }
    else if (result_) {
      marker_.done(HAXE_LITERALEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // 'function' componentName genericParam? '(' parameterList? ')' typeTag? 'untyped'? functionCommonBody
  public static boolean localFunctionDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "localFunctionDeclaration")) return false;
    if (!nextTokenIs(builder_, KFUNCTION)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
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
      marker_.done(HAXE_LOCALFUNCTIONDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KVAR);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, localVarDeclarationPartList(builder_, level_ + 1));
    result_ = pinned_ && localVarDeclaration_2(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_LOCALVARDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_1(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_2(builder_, level_ + 1);
    result_ = result_ && localVarDeclarationPart_3(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_LOCALVARDECLARATIONPART);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, local_var_declaration_part_recover_parser_);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // !(';' | ',' | '!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | '++' | '-' | '--' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'switch' | 'this' | 'super' | 'throw' | 'try' | 'untyped' | 'var' | 'while' | '{' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING)
  static boolean local_var_declaration_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !local_var_declaration_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // (';' | ',' | '!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | '++' | '-' | '--' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'switch' | 'this' | 'super' | 'throw' | 'try' | 'untyped' | 'var' | 'while' | '{' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING)
  private static boolean local_var_declaration_part_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover_0")) return false;
    return local_var_declaration_part_recover_0_0(builder_, level_ + 1);
  }

  // ';' | ',' | '!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '(' | '++' | '-' | '--' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'for' | 'function' | 'if' | 'new' | 'null' | 'return' | 'switch' | 'this' | 'super' | 'throw' | 'try' | 'untyped' | 'var' | 'while' | '{' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING
  private static boolean local_var_declaration_part_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "local_var_declaration_part_recover_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OSEMI);
    if (!result_) result_ = consumeToken(builder_, OCOMMA);
    if (!result_) result_ = consumeToken(builder_, ONOT);
    if (!result_) result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, PLPAREN);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, PLBRACK);
    if (!result_) result_ = consumeToken(builder_, KBREAK);
    if (!result_) result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KCAST);
    if (!result_) result_ = consumeToken(builder_, KCONTINUE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, KDO);
    if (!result_) result_ = consumeToken(builder_, KFOR);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KIF);
    if (!result_) result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KRETURN);
    if (!result_) result_ = consumeToken(builder_, KSWITCH);
    if (!result_) result_ = consumeToken(builder_, KTHIS);
    if (!result_) result_ = consumeToken(builder_, KSUPER);
    if (!result_) result_ = consumeToken(builder_, KTHROW);
    if (!result_) result_ = consumeToken(builder_, KTRY);
    if (!result_) result_ = consumeToken(builder_, KUNTYPED);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, KWHILE);
    if (!result_) result_ = consumeToken(builder_, PLCURLY);
    if (!result_) result_ = consumeToken(builder_, OCOMPLEMENT);
    if (!result_) result_ = consumeToken(builder_, ID);
    if (!result_) result_ = consumeToken(builder_, LITCHAR);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, LITSTRING);
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
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "logicAndExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, OCOND_AND);
    pinned_ = result_; // pin = 1
    result_ = result_ && compareExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_LOGICANDEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // compareExpressionWrapper logicAndExpression*
  static boolean logicAndExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicAndExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "logicOrExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, OCOND_OR);
    pinned_ = result_; // pin = 1
    result_ = result_ && logicAndExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_LOGICOREXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // logicAndExpressionWrapper logicOrExpression*
  static boolean logicOrExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "logicOrExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
  // '@:final' | '@:core_api' | '@:bind' | '@:macro' | '@:hack'
  //                       | requireMeta | fakeEnumMeta | nativeMeta | bitmapMeta | nsMeta | customMeta | metaMeta | buildMacro | autoBuildMacro
  static boolean macroClass(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroClass")) return false;
    if (!nextTokenIs(builder_, KBIND) && !nextTokenIs(builder_, KMETA)
        && !nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KMACRO)
        && !nextTokenIs(builder_, KNATIVE) && !nextTokenIs(builder_, KREQUIRE)
        && !nextTokenIs(builder_, MACRO_ID) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KBITMAP)
        && !nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KHACK)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KFINAL);
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
  // '@:macro' | '@:keep' | '@:protected' | '@:debug' | '@:nodebug'
  //                        | requireMeta | nsMeta | getterMeta | setterMeta | customMeta | buildMacro | autoBuildMacro
  static boolean macroMember(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "macroMember")) return false;
    if (!nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KSETTER)
        && !nextTokenIs(builder_, KPROTECTED) && !nextTokenIs(builder_, KBUILD)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, MACRO_ID)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KDEBUG)
        && !nextTokenIs(builder_, KGETTER) && !nextTokenIs(builder_, KKEEP)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KMACRO)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KMACRO);
    if (!result_) result_ = consumeToken(builder_, KKEEP);
    if (!result_) result_ = consumeToken(builder_, KPROTECTED);
    if (!result_) result_ = consumeToken(builder_, KDEBUG);
    if (!result_) result_ = consumeToken(builder_, KNODEBUG);
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
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ID);
    result_ = result_ && consumeToken(builder_, OASSIGN);
    result_ = result_ && consumeToken(builder_, LITSTRING);
    if (result_) {
      marker_.done(HAXE_METAKEYVALUE);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KMETA);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, ID)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PLPAREN)) && result_;
    result_ = pinned_ && report_error_(builder_, metaPartList(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_METAMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // metaKeyValue (',' metaKeyValue)*
  static boolean metaPartList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metaPartList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, OQUOTIENT) && !nextTokenIs(builder_, OMUL)
        && !nextTokenIs(builder_, OREMAINDER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "multiplicativeExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = multiplicativeExpression_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && multiplicativeExpression_1(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_MULTIPLICATIVEEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KNATIVE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, LITSTRING)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_NATIVEMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'new' type '(' expressionList? ')'
  public static boolean newExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "newExpression")) return false;
    if (!nextTokenIs(builder_, ONEW)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, ONEW);
    result_ = result_ && type(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, newExpression_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_NEWEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_NEWEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
  // ('untyped' statement)
  //                             | localVarDeclaration
  //                             | localFunctionDeclaration
  //                             | ifStatement
  //                             | forStatement
  //                             | whileStatement
  //                             | doWhileStatement
  //                             | returnStatement
  //                             | breakStatement
  //                             | continueStatement
  //                             | caseStatement
  //                             | defaultStatement
  //                             | switchStatement
  //                             | throwStatement
  //                             | tryStatement
  //                             | (expression ';'?)
  //                             | pp
  static boolean notBlockStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement")) return false;
    if (!nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, ID)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, KWHILE)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, KRETURN)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KDEFAULT)
        && !nextTokenIs(builder_, KCONTINUE) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, KIF) && !nextTokenIs(builder_, PPELSEIF)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, PPERROR)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, PPELSE) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KUNTYPED) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, KDO) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTHROW) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, PPEND)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KVAR)
        && !nextTokenIs(builder_, KFOR) && !nextTokenIs(builder_, KBREAK)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KCASE)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    if (!result_) result_ = caseStatement(builder_, level_ + 1);
    if (!result_) result_ = defaultStatement(builder_, level_ + 1);
    if (!result_) result_ = switchStatement(builder_, level_ + 1);
    if (!result_) result_ = throwStatement(builder_, level_ + 1);
    if (!result_) result_ = tryStatement(builder_, level_ + 1);
    if (!result_) result_ = notBlockStatement_15(builder_, level_ + 1);
    if (!result_) result_ = pp(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ('untyped' statement)
  private static boolean notBlockStatement_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_0")) return false;
    return notBlockStatement_0_0(builder_, level_ + 1);
  }

  // 'untyped' statement
  private static boolean notBlockStatement_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KUNTYPED);
    result_ = result_ && statement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // (expression ';'?)
  private static boolean notBlockStatement_15(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_15")) return false;
    return notBlockStatement_15_0(builder_, level_ + 1);
  }

  // expression ';'?
  private static boolean notBlockStatement_15_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_15_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = expression(builder_, level_ + 1);
    result_ = result_ && notBlockStatement_15_0_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // ';'?
  private static boolean notBlockStatement_15_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "notBlockStatement_15_0_1")) return false;
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KNS);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, LITSTRING)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_NSMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '{' objectLiteralElementList? '}'
  public static boolean objectLiteral(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteral")) return false;
    if (!nextTokenIs(builder_, PLCURLY)) return false;
    boolean result_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLCURLY);
    result_ = result_ && objectLiteral_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRCURLY);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_OBJECTLITERAL)) {
      marker_.drop();
    }
    else if (result_) {
      marker_.done(HAXE_OBJECTLITERAL);
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
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = identifier(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOLON);
    result_ = result_ && expression(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_OBJECTLITERALELEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // objectLiteralElement (',' objectLiteralElement)*
  static boolean objectLiteralElementList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "objectLiteralElementList")) return false;
    if (!nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = objectLiteralElement(builder_, level_ + 1);
    result_ = result_ && objectLiteralElementList_1(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
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
    final Marker marker_ = builder_.mark();
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
  // 'package' simpleQualifiedReferenceExpression? ';'
  public static boolean packageStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "packageStatement")) return false;
    if (!nextTokenIs(builder_, KPACKAGE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KPACKAGE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, packageStatement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_PACKAGESTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    if (!nextTokenIs(builder_, OQUEST) && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = parameter(builder_, level_ + 1);
    result_ = result_ && parameterList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_PARAMETERLIST);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, parameterListRecovery_parser_);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !consumeToken(builder_, PRPAREN);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  /* ********************************************************** */
  // !(')' | ',')
  static boolean parameter_recovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_recovery")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !parameter_recovery_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // (')' | ',')
  private static boolean parameter_recovery_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_recovery_0")) return false;
    return parameter_recovery_0_0(builder_, level_ + 1);
  }

  // ')' | ','
  private static boolean parameter_recovery_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parameter_recovery_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PRPAREN);
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
  // '(' (expression | statement) ')'
  public static boolean parenthesizedExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "parenthesizedExpression")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PLPAREN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, parenthesizedExpression_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_PARENTHESIZEDEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_PARENTHESIZEDEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // ppIf | ppElseIf | ppElse | ppEnd | ppError
  public static boolean pp(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "pp")) return false;
    if (!nextTokenIs(builder_, PPERROR) && !nextTokenIs(builder_, PPIF)
        && !nextTokenIs(builder_, PPELSEIF) && !nextTokenIs(builder_, PPEND)
        && !nextTokenIs(builder_, PPELSE)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = ppIf(builder_, level_ + 1);
    if (!result_) result_ = ppElseIf(builder_, level_ + 1);
    if (!result_) result_ = ppElse(builder_, level_ + 1);
    if (!result_) result_ = ppEnd(builder_, level_ + 1);
    if (!result_) result_ = ppError(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_PP);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '#else'
  public static boolean ppElse(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppElse")) return false;
    if (!nextTokenIs(builder_, PPELSE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PPELSE);
    pinned_ = result_; // pin = 1
    if (result_ || pinned_) {
      marker_.done(HAXE_PPELSE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '#elseif' ternaryExpressionWrapper
  public static boolean ppElseIf(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppElseIf")) return false;
    if (!nextTokenIs(builder_, PPELSEIF)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PPELSEIF);
    pinned_ = result_; // pin = 1
    result_ = result_ && ternaryExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_PPELSEIF);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '#end'
  public static boolean ppEnd(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppEnd")) return false;
    if (!nextTokenIs(builder_, PPEND)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PPEND);
    pinned_ = result_; // pin = 1
    if (result_ || pinned_) {
      marker_.done(HAXE_PPEND);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '#error'
  public static boolean ppError(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppError")) return false;
    if (!nextTokenIs(builder_, PPERROR)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PPERROR);
    pinned_ = result_; // pin = 1
    if (result_ || pinned_) {
      marker_.done(HAXE_PPERROR);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '#if' ternaryExpressionWrapper
  public static boolean ppIf(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppIf")) return false;
    if (!nextTokenIs(builder_, PPIF)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, PPIF);
    pinned_ = result_; // pin = 1
    result_ = result_ && ternaryExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_PPIF);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // ppIf expression (ppElseIf expression)* ppElse expression ppEnd
  public static boolean ppIfValue(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppIfValue")) return false;
    if (!nextTokenIs(builder_, PPIF)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = ppIf(builder_, level_ + 1);
    result_ = result_ && expression(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, ppIfValue_2(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, ppElse(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && ppEnd(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_PPIFVALUE);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // (ppElseIf expression)*
  private static boolean ppIfValue_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppIfValue_2")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!ppIfValue_2_0(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "ppIfValue_2");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  // (ppElseIf expression)
  private static boolean ppIfValue_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppIfValue_2_0")) return false;
    return ppIfValue_2_0_0(builder_, level_ + 1);
  }

  // ppElseIf expression
  private static boolean ppIfValue_2_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ppIfValue_2_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = ppElseIf(builder_, level_ + 1);
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
  // ('-' | '--' | '++' | '!' | '~') prefixExpression | suffixExpressionWrapper
  public static boolean prefixExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    result_ = prefixExpression_0(builder_, level_ + 1);
    if (!result_) result_ = suffixExpressionWrapper(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_PREFIXEXPRESSION)) {
      marker_.drop();
    }
    else if (result_) {
      marker_.done(HAXE_PREFIXEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  // ('-' | '--' | '++' | '!' | '~') prefixExpression
  private static boolean prefixExpression_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "prefixExpression_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, KNEVER) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, KDEFAULT) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = referenceExpression(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, KDYNAMIC);
    if (!result_) result_ = consumeToken(builder_, KNEVER);
    if (result_) {
      marker_.done(HAXE_PROPERTYACCESSOR);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '(' propertyAccessor ',' propertyAccessor ')'
  public static boolean propertyDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "propertyDeclaration")) return false;
    if (!nextTokenIs(builder_, PLPAREN)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PLPAREN);
    result_ = result_ && propertyAccessor(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OCOMMA);
    result_ = result_ && propertyAccessor(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, PRPAREN);
    if (result_) {
      marker_.done(HAXE_PROPERTYDECLARATION);
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
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "qualifiedReferenceExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, ODOT);
    pinned_ = result_; // pin = 1
    result_ = result_ && referenceExpression(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_REFERENCEEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // qualifiedReferenceExpression (callExpression | arrayAccessExpression | qualifiedReferenceExpression)*
  static boolean qualifiedReferenceTail(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "qualifiedReferenceTail")) return false;
    if (!nextTokenIs(builder_, ODOT)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = identifier(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_REFERENCEEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_REFERENCEEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '@:require' '(' identifier ')'
  public static boolean requireMeta(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "requireMeta")) return false;
    if (!nextTokenIs(builder_, KREQUIRE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KREQUIRE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, identifier(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_REQUIREMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'return' expression? ';'
  public static boolean returnStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement")) return false;
    if (!nextTokenIs(builder_, KRETURN)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KRETURN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, returnStatement_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_RETURNSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // expression?
  private static boolean returnStatement_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatement_1")) return false;
    expression(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // 'return' expression
  public static boolean returnStatementWithoutSemicolon(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "returnStatementWithoutSemicolon")) return false;
    if (!nextTokenIs(builder_, KRETURN)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KRETURN);
    result_ = result_ && expression(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_RETURNSTATEMENTWITHOUTSEMICOLON);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KSETTER);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, referenceExpression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, PRPAREN) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_SETTERMETA);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // shiftOperator additiveExpressionWrapper
  public static boolean shiftExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpression")) return false;
    if (!nextTokenIs(builder_, OSHIFT_LEFT) && !nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "shiftExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = shiftOperator(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && additiveExpressionWrapper(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_SHIFTEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // additiveExpressionWrapper shiftExpression*
  static boolean shiftExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, OSHIFT_LEFT) && !nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = unsignedShiftRightOperator(builder_, level_ + 1);
    if (!result_) result_ = shiftRightOperator(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, OSHIFT_LEFT);
    if (result_) {
      marker_.done(HAXE_SHIFTOPERATOR);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '>' '>'
  public static boolean shiftRightOperator(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "shiftRightOperator")) return false;
    if (!nextTokenIs(builder_, OGREATER)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_SHIFTRIGHTOPERATOR);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = referenceExpression(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && simpleQualifiedReferenceExpression_1(builder_, level_ + 1);
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_REFERENCEEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_REFERENCEEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = blockStatement(builder_, level_ + 1);
    if (!result_) result_ = notBlockStatement(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, statement_recovery_parser_);
    return result_;
  }

  /* ********************************************************** */
  // (statement ';'?)+
  static boolean statementList(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statementList")) return false;
    if (!nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, KFUNCTION)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, ID)
        && !nextTokenIs(builder_, KTHIS) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, KWHILE)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, KRETURN)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KDEFAULT)
        && !nextTokenIs(builder_, KCONTINUE) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, KIF) && !nextTokenIs(builder_, PPELSEIF)
        && !nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, PPERROR)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, PPELSE) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, KUNTYPED) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, KDO) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, KTHROW)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, PPEND)
        && !nextTokenIs(builder_, KCAST) && !nextTokenIs(builder_, KVAR)
        && !nextTokenIs(builder_, KFOR) && !nextTokenIs(builder_, KBREAK)
        && !nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KCASE)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
  // !('!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '('
  //   | '++' | '-' | '--' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'for'
  //   | 'function' | 'if' | 'new' | 'null' | 'return' | 'switch' | 'this' | 'super' | 'throw' | 'try' | 'untyped'
  //   | 'var' | 'while' | '{' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING
  //   | ';' | '}' | ')' | 'else')
  static boolean statement_recovery(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !statement_recovery_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '('
  //   | '++' | '-' | '--' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'for'
  //   | 'function' | 'if' | 'new' | 'null' | 'return' | 'switch' | 'this' | 'super' | 'throw' | 'try' | 'untyped'
  //   | 'var' | 'while' | '{' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING
  //   | ';' | '}' | ')' | 'else')
  private static boolean statement_recovery_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery_0")) return false;
    return statement_recovery_0_0(builder_, level_ + 1);
  }

  // '!' | '#else' | '#elseif' | '#end' | '#error' | '#if' | '('
  //   | '++' | '-' | '--' | '[' | 'break' | 'case' | 'cast' | 'continue' | 'default' | 'do' | 'for'
  //   | 'function' | 'if' | 'new' | 'null' | 'return' | 'switch' | 'this' | 'super' | 'throw' | 'try' | 'untyped'
  //   | 'var' | 'while' | '{' | '~' | ID | LITCHAR | LITFLOAT | LITHEX | LITINT | LITOCT | LITSTRING
  //   | ';' | '}' | ')' | 'else'
  private static boolean statement_recovery_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "statement_recovery_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, ONOT);
    if (!result_) result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, PLPAREN);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS);
    if (!result_) result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, PLBRACK);
    if (!result_) result_ = consumeToken(builder_, KBREAK);
    if (!result_) result_ = consumeToken(builder_, KCASE);
    if (!result_) result_ = consumeToken(builder_, KCAST);
    if (!result_) result_ = consumeToken(builder_, KCONTINUE);
    if (!result_) result_ = consumeToken(builder_, KDEFAULT);
    if (!result_) result_ = consumeToken(builder_, KDO);
    if (!result_) result_ = consumeToken(builder_, KFOR);
    if (!result_) result_ = consumeToken(builder_, KFUNCTION);
    if (!result_) result_ = consumeToken(builder_, KIF);
    if (!result_) result_ = consumeToken(builder_, ONEW);
    if (!result_) result_ = consumeToken(builder_, KNULL);
    if (!result_) result_ = consumeToken(builder_, KRETURN);
    if (!result_) result_ = consumeToken(builder_, KSWITCH);
    if (!result_) result_ = consumeToken(builder_, KTHIS);
    if (!result_) result_ = consumeToken(builder_, KSUPER);
    if (!result_) result_ = consumeToken(builder_, KTHROW);
    if (!result_) result_ = consumeToken(builder_, KTRY);
    if (!result_) result_ = consumeToken(builder_, KUNTYPED);
    if (!result_) result_ = consumeToken(builder_, KVAR);
    if (!result_) result_ = consumeToken(builder_, KWHILE);
    if (!result_) result_ = consumeToken(builder_, PLCURLY);
    if (!result_) result_ = consumeToken(builder_, OCOMPLEMENT);
    if (!result_) result_ = consumeToken(builder_, ID);
    if (!result_) result_ = consumeToken(builder_, LITCHAR);
    if (!result_) result_ = consumeToken(builder_, LITFLOAT);
    if (!result_) result_ = consumeToken(builder_, LITHEX);
    if (!result_) result_ = consumeToken(builder_, LITINT);
    if (!result_) result_ = consumeToken(builder_, LITOCT);
    if (!result_) result_ = consumeToken(builder_, LITSTRING);
    if (!result_) result_ = consumeToken(builder_, OSEMI);
    if (!result_) result_ = consumeToken(builder_, PRCURLY);
    if (!result_) result_ = consumeToken(builder_, PRPAREN);
    if (!result_) result_ = consumeToken(builder_, KELSE);
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
    if (!nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, OMINUS_MINUS)) return false;
    boolean result_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "suffixExpression_0")) return false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OMINUS_MINUS);
    if (!result_) result_ = consumeToken(builder_, OPLUS_PLUS);
    if (result_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_SUFFIXEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // value suffixExpression*
  static boolean suffixExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "suffixExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, KIF) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ID)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, KSUPER)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, PLBRACK) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, KFALSE)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KSUPER);
    pinned_ = result_; // pin = 1
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_SUPEREXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_SUPEREXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'switch' '(' expression ')' blockStatement
  public static boolean switchExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchExpression")) return false;
    if (!nextTokenIs(builder_, KSWITCH)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KSWITCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && blockStatement(builder_, level_ + 1) && result_;
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_SWITCHEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_SWITCHEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'switch' '(' expression ')' blockStatement
  public static boolean switchStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "switchStatement")) return false;
    if (!nextTokenIs(builder_, KSWITCH)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KSWITCH);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && blockStatement(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_SWITCHSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '?' expression ':' ternaryExpressionWrapper
  public static boolean ternaryExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternaryExpression")) return false;
    if (!nextTokenIs(builder_, OQUEST)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker left_marker_ = (Marker)builder_.getLatestDoneMarker();
    if (!invalid_left_marker_guard_(builder_, left_marker_, "ternaryExpression")) return false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, OQUEST);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OCOLON)) && result_;
    result_ = pinned_ && ternaryExpressionWrapper(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.drop();
      left_marker_.precede().done(HAXE_TERNARYEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // logicOrExpressionWrapper ternaryExpression?
  static boolean ternaryExpressionWrapper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "ternaryExpressionWrapper")) return false;
    if (!nextTokenIs(builder_, KSUPER) && !nextTokenIs(builder_, LITOCT)
        && !nextTokenIs(builder_, LITHEX) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, OMINUS_MINUS) && !nextTokenIs(builder_, LITCHAR)
        && !nextTokenIs(builder_, KTRUE) && !nextTokenIs(builder_, KSWITCH)
        && !nextTokenIs(builder_, OPLUS_PLUS) && !nextTokenIs(builder_, KIF)
        && !nextTokenIs(builder_, ONOT) && !nextTokenIs(builder_, PLBRACK)
        && !nextTokenIs(builder_, LITFLOAT) && !nextTokenIs(builder_, PLPAREN)
        && !nextTokenIs(builder_, ID) && !nextTokenIs(builder_, LITSTRING)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ONEW)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, KNULL) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, KFALSE) && !nextTokenIs(builder_, OMINUS)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, OCOMPLEMENT) && !nextTokenIs(builder_, KUNTYPED)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KTHIS);
    pinned_ = result_; // pin = 1
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_THISEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_THISEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'throw' expression ';'
  public static boolean throwStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "throwStatement")) return false;
    if (!nextTokenIs(builder_, KTHROW)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KTHROW);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_THROWSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // importStatement | usingStatement | pp | topLevelDeclaration
  static boolean topLevel(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "topLevel")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = importStatement(builder_, level_ + 1);
    if (!result_) result_ = usingStatement(builder_, level_ + 1);
    if (!result_) result_ = pp(builder_, level_ + 1);
    if (!result_) result_ = topLevelDeclaration(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, top_level_recover_parser_);
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
    if (!nextTokenIs(builder_, KBIND) && !nextTokenIs(builder_, KMETA)
        && !nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KFAKEENUM)
        && !nextTokenIs(builder_, KFINAL) && !nextTokenIs(builder_, KENUM)
        && !nextTokenIs(builder_, KMACRO) && !nextTokenIs(builder_, KNATIVE)
        && !nextTokenIs(builder_, KTYPEDEF) && !nextTokenIs(builder_, KCLASS)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, MACRO_ID)
        && !nextTokenIs(builder_, KEXTERN) && !nextTokenIs(builder_, KNS)
        && !nextTokenIs(builder_, KCOREAPI) && !nextTokenIs(builder_, KBITMAP)
        && !nextTokenIs(builder_, KPRIVATE) && !nextTokenIs(builder_, KINTERFACE)
        && !nextTokenIs(builder_, KAUTOBUILD) && !nextTokenIs(builder_, KHACK)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
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
  // !('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:bind' | '@:bitmap' | '@:build' | '@:autoBuild' | '@:core_api' | '@:fakeEnum' | '@:final' | '@:hack' | '@:macro' | '@:meta' | '@:native' | '@:ns' | '@:require' | 'class' | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef' | MACRO_ID)
  static boolean top_level_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !top_level_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
    return result_;
  }

  // ('#else' | '#elseif' | '#end' | '#error' | '#if' | '@:bind' | '@:bitmap' | '@:build' | '@:autoBuild' | '@:core_api' | '@:fakeEnum' | '@:final' | '@:hack' | '@:macro' | '@:meta' | '@:native' | '@:ns' | '@:require' | 'class' | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef' | MACRO_ID)
  private static boolean top_level_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover_0")) return false;
    return top_level_recover_0_0(builder_, level_ + 1);
  }

  // '#else' | '#elseif' | '#end' | '#error' | '#if' | '@:bind' | '@:bitmap' | '@:build' | '@:autoBuild' | '@:core_api' | '@:fakeEnum' | '@:final' | '@:hack' | '@:macro' | '@:meta' | '@:native' | '@:ns' | '@:require' | 'class' | 'enum' | 'extern' | 'import' | 'using' | 'interface' | 'private' | 'typedef' | MACRO_ID
  private static boolean top_level_recover_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "top_level_recover_0_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, PPELSE);
    if (!result_) result_ = consumeToken(builder_, PPELSEIF);
    if (!result_) result_ = consumeToken(builder_, PPEND);
    if (!result_) result_ = consumeToken(builder_, PPERROR);
    if (!result_) result_ = consumeToken(builder_, PPIF);
    if (!result_) result_ = consumeToken(builder_, KBIND);
    if (!result_) result_ = consumeToken(builder_, KBITMAP);
    if (!result_) result_ = consumeToken(builder_, KBUILD);
    if (!result_) result_ = consumeToken(builder_, KAUTOBUILD);
    if (!result_) result_ = consumeToken(builder_, KCOREAPI);
    if (!result_) result_ = consumeToken(builder_, KFAKEENUM);
    if (!result_) result_ = consumeToken(builder_, KFINAL);
    if (!result_) result_ = consumeToken(builder_, KHACK);
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
  // 'try' expression catchExpression*
  public static boolean tryExpression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryExpression")) return false;
    if (!nextTokenIs(builder_, KTRY)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final int start_ = builder_.getCurrentOffset();
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KTRY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression(builder_, level_ + 1));
    result_ = pinned_ && tryExpression_2(builder_, level_ + 1) && result_;
    LighterASTNode last_ = result_? builder_.getLatestDoneMarker() : null;
    if (last_ != null && last_.getStartOffset() == start_ && type_extends_(last_.getTokenType(), HAXE_TRYEXPRESSION)) {
      marker_.drop();
    }
    else if (result_ || pinned_) {
      marker_.done(HAXE_TRYEXPRESSION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // catchExpression*
  private static boolean tryExpression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryExpression_2")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!catchExpression(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "tryExpression_2");
        break;
      }
      offset_ = next_offset_;
    }
    return true;
  }

  /* ********************************************************** */
  // 'try' blockStatement catchStatement*
  public static boolean tryStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement")) return false;
    if (!nextTokenIs(builder_, KTRY)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KTRY);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, blockStatement(builder_, level_ + 1));
    result_ = pinned_ && tryStatement_2(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_TRYSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  // catchStatement*
  private static boolean tryStatement_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tryStatement_2")) return false;
    int offset_ = builder_.getCurrentOffset();
    while (true) {
      if (!catchStatement(builder_, level_ + 1)) break;
      int next_offset_ = builder_.getCurrentOffset();
      if (offset_ == next_offset_) {
        empty_element_parsed_guard_(builder_, offset_, "tryStatement_2");
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && type(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPEEXTENDS);
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
    if (!nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = typeListPart(builder_, level_ + 1);
    result_ = result_ && typeList_1(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPELIST);
    }
    else {
      marker_.rollbackTo();
    }
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
    final Marker marker_ = builder_.mark();
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
    if (!nextTokenIs(builder_, PLCURLY) && !nextTokenIs(builder_, ID)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = functionTypeWrapper(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPELISTPART);
    }
    else {
      marker_.rollbackTo();
    }
    return result_;
  }

  /* ********************************************************** */
  // '<' typeList '>'
  public static boolean typeParam(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "typeParam")) return false;
    if (!nextTokenIs(builder_, OLESS)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OLESS);
    result_ = result_ && typeList(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_TYPEPARAM);
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
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OCOLON);
    result_ = result_ && functionTypeWrapper(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_TYPETAG);
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
        && !nextTokenIs(builder_, KTYPEDEF)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = typedefDeclaration_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KTYPEDEF);
    pinned_ = result_; // pin = 2
    result_ = result_ && report_error_(builder_, componentName(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, typedefDeclaration_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, OASSIGN)) && result_;
    result_ = pinned_ && report_error_(builder_, functionTypeWrapper(builder_, level_ + 1)) && result_;
    result_ = pinned_ && typedefDeclaration_6(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_TYPEDEFDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    result_ = result_ && consumeToken(builder_, OGREATER);
    if (result_) {
      marker_.done(HAXE_UNSIGNEDSHIFTRIGHTOPERATOR);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KUSING);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, simpleQualifiedReferenceExpression(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_USINGSTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // 'untyped' value
  //                 | (literalExpression qualifiedReferenceTail?)
  //                 | ifExpression
  //                 | switchExpression
  //                 | tryExpression
  //                 | ppIfValue
  //                 | castExpression
  //                 | newExpressionOrCall
  //                 | parenthesizedExpressionOrCall
  //                 | callOrArrayAccess
  static boolean value(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value")) return false;
    if (!nextTokenIs(builder_, LITCHAR) && !nextTokenIs(builder_, LITHEX)
        && !nextTokenIs(builder_, LITOCT) && !nextTokenIs(builder_, KUNTYPED)
        && !nextTokenIs(builder_, KIF) && !nextTokenIs(builder_, KTRUE)
        && !nextTokenIs(builder_, KSWITCH) && !nextTokenIs(builder_, KCAST)
        && !nextTokenIs(builder_, PPIF) && !nextTokenIs(builder_, ID)
        && !nextTokenIs(builder_, KFUNCTION) && !nextTokenIs(builder_, KNULL)
        && !nextTokenIs(builder_, LITSTRING) && !nextTokenIs(builder_, LITFLOAT)
        && !nextTokenIs(builder_, PLPAREN) && !nextTokenIs(builder_, KSUPER)
        && !nextTokenIs(builder_, LITINT) && !nextTokenIs(builder_, KTHIS)
        && !nextTokenIs(builder_, PLBRACK) && !nextTokenIs(builder_, KTRY)
        && !nextTokenIs(builder_, ONEW) && !nextTokenIs(builder_, PLCURLY)
        && !nextTokenIs(builder_, KFALSE)) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = value_0(builder_, level_ + 1);
    if (!result_) result_ = value_1(builder_, level_ + 1);
    if (!result_) result_ = ifExpression(builder_, level_ + 1);
    if (!result_) result_ = switchExpression(builder_, level_ + 1);
    if (!result_) result_ = tryExpression(builder_, level_ + 1);
    if (!result_) result_ = ppIfValue(builder_, level_ + 1);
    if (!result_) result_ = castExpression(builder_, level_ + 1);
    if (!result_) result_ = newExpressionOrCall(builder_, level_ + 1);
    if (!result_) result_ = parenthesizedExpressionOrCall(builder_, level_ + 1);
    if (!result_) result_ = callOrArrayAccess(builder_, level_ + 1);
    if (!result_) {
      marker_.rollbackTo();
    }
    else {
      marker_.drop();
    }
    return result_;
  }

  // 'untyped' value
  private static boolean value_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "value_0")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    result_ = consumeToken(builder_, KUNTYPED);
    result_ = result_ && value(builder_, level_ + 1);
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
    final Marker marker_ = builder_.mark();
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

  /* ********************************************************** */
  // macroMember* declarationAttributeList? 'var' varDeclarationPartList ';'
  public static boolean varDeclaration(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "varDeclaration")) return false;
    if (!nextTokenIs(builder_, KBUILD) && !nextTokenIs(builder_, KDYNAMIC)
        && !nextTokenIs(builder_, KSETTER) && !nextTokenIs(builder_, KINLINE)
        && !nextTokenIs(builder_, KMACRO) && !nextTokenIs(builder_, KKEEP)
        && !nextTokenIs(builder_, KNODEBUG) && !nextTokenIs(builder_, KOVERRIDE)
        && !nextTokenIs(builder_, KREQUIRE) && !nextTokenIs(builder_, MACRO_ID)
        && !nextTokenIs(builder_, KDEBUG) && !nextTokenIs(builder_, KPROTECTED)
        && !nextTokenIs(builder_, KNS) && !nextTokenIs(builder_, KSTATIC)
        && !nextTokenIs(builder_, KVAR) && !nextTokenIs(builder_, KPRIVATE)
        && !nextTokenIs(builder_, KPUBLIC) && !nextTokenIs(builder_, KAUTOBUILD)
        && !nextTokenIs(builder_, KGETTER)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = varDeclaration_0(builder_, level_ + 1);
    result_ = result_ && varDeclaration_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, KVAR);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, varDeclarationPartList(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, OSEMI) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_VARDECLARATION);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_RECOVER_);
    result_ = componentName(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_1(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_2(builder_, level_ + 1);
    result_ = result_ && varDeclarationPart_3(builder_, level_ + 1);
    if (result_) {
      marker_.done(HAXE_VARDECLARATIONPART);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_RECOVER_, var_declaration_part_recover_parser_);
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
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
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, OASSIGN);
    pinned_ = result_; // pin = 1
    result_ = result_ && expression(builder_, level_ + 1);
    if (result_ || pinned_) {
      marker_.done(HAXE_VARINIT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // !(';' | ',')
  static boolean var_declaration_part_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "var_declaration_part_recover")) return false;
    boolean result_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_NOT_);
    result_ = !var_declaration_part_recover_0(builder_, level_ + 1);
    marker_.rollbackTo();
    result_ = exitErrorRecordingSection(builder_, result_, level_, false, _SECTION_NOT_, null);
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
    final Marker marker_ = builder_.mark();
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
  // 'while' '(' expression ')' statement
  public static boolean whileStatement(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "whileStatement")) return false;
    if (!nextTokenIs(builder_, KWHILE)) return false;
    boolean result_ = false;
    boolean pinned_ = false;
    final Marker marker_ = builder_.mark();
    enterErrorRecordingSection(builder_, level_, _SECTION_GENERAL_);
    result_ = consumeToken(builder_, KWHILE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, PLPAREN));
    result_ = pinned_ && report_error_(builder_, expression(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, PRPAREN)) && result_;
    result_ = pinned_ && statement(builder_, level_ + 1) && result_;
    if (result_ || pinned_) {
      marker_.done(HAXE_WHILESTATEMENT);
    }
    else {
      marker_.rollbackTo();
    }
    result_ = exitErrorRecordingSection(builder_, result_, level_, pinned_, _SECTION_GENERAL_, null);
    return result_ || pinned_;
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
  final static Parser extern_class_body_part_recover_parser_ = new Parser() {
      public boolean parse(PsiBuilder builder_, int level_) {
        return extern_class_body_part_recover(builder_, level_ + 1);
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
