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
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.*;

public interface HaxeTokenTypes {

  IElementType ABSTRACT_CLASS_DECLARATION = new HaxeElementType("ABSTRACT_CLASS_DECLARATION");
  IElementType ACCESS = new HaxeElementType("ACCESS");
  IElementType ADDITIVE_EXPRESSION = new HaxeElementType("ADDITIVE_EXPRESSION");
  IElementType ANONYMOUS_FUNCTION_DECLARATION = new HaxeElementType("ANONYMOUS_FUNCTION_DECLARATION");
  IElementType ANONYMOUS_TYPE = new HaxeElementType("ANONYMOUS_TYPE");
  IElementType ANONYMOUS_TYPE_BODY = new HaxeElementType("ANONYMOUS_TYPE_BODY");
  IElementType ANONYMOUS_TYPE_FIELD = new HaxeElementType("ANONYMOUS_TYPE_FIELD");
  IElementType ANONYMOUS_TYPE_FIELD_LIST = new HaxeElementType("ANONYMOUS_TYPE_FIELD_LIST");
  IElementType ARRAY_ACCESS_EXPRESSION = new HaxeElementType("ARRAY_ACCESS_EXPRESSION");
  IElementType ARRAY_LITERAL = new HaxeElementType("ARRAY_LITERAL");
  IElementType ASSIGN_EXPRESSION = new HaxeElementType("ASSIGN_EXPRESSION");
  IElementType ASSIGN_OPERATION = new HaxeElementType("ASSIGN_OPERATION");
  IElementType AUTO_BUILD_MACRO = new HaxeElementType("AUTO_BUILD_MACRO");
  IElementType BIND_META = new HaxeElementType("BIND_META");
  IElementType BITMAP_META = new HaxeElementType("BITMAP_META");
  IElementType BITWISE_EXPRESSION = new HaxeElementType("BITWISE_EXPRESSION");
  IElementType BIT_OPERATION = new HaxeElementType("BIT_OPERATION");
  IElementType BLOCK_STATEMENT = new HaxeElementType("BLOCK_STATEMENT");
  IElementType BREAK_STATEMENT = new HaxeElementType("BREAK_STATEMENT");
  IElementType BUILD_MACRO = new HaxeElementType("BUILD_MACRO");
  IElementType CALL_EXPRESSION = new HaxeElementType("CALL_EXPRESSION");
  IElementType CAST_EXPRESSION = new HaxeElementType("CAST_EXPRESSION");
  IElementType CATCH_STATEMENT = new HaxeElementType("CATCH_STATEMENT");
  IElementType CLASS_BODY = new HaxeElementType("CLASS_BODY");
  IElementType CLASS_DECLARATION = new HaxeElementType("CLASS_DECLARATION");
  IElementType COMPARE_EXPRESSION = new HaxeElementType("COMPARE_EXPRESSION");
  IElementType COMPARE_OPERATION = new HaxeElementType("COMPARE_OPERATION");
  IElementType COMPONENT_NAME = new HaxeElementType("COMPONENT_NAME");
  IElementType CONDITIONAL = new HaxeElementType("CONDITIONAL");
  IElementType CONSTRUCTOR_NAME = new HaxeElementType("CONSTRUCTOR_NAME");
  IElementType CONTINUE_STATEMENT = new HaxeElementType("CONTINUE_STATEMENT");
  IElementType CORE_API_META = new HaxeElementType("CORE_API_META");
  IElementType CUSTOM_META = new HaxeElementType("CUSTOM_META");
  IElementType DEBUG_META = new HaxeElementType("DEBUG_META");
  IElementType DECLARATION_ATTRIBUTE = new HaxeElementType("DECLARATION_ATTRIBUTE");
  IElementType DEFAULT_CASE = new HaxeElementType("DEFAULT_CASE");
  IElementType DO_WHILE_STATEMENT = new HaxeElementType("DO_WHILE_STATEMENT");
  IElementType ENUM_BODY = new HaxeElementType("ENUM_BODY");
  IElementType ENUM_CONSTRUCTOR_PARAMETERS = new HaxeElementType("ENUM_CONSTRUCTOR_PARAMETERS");
  IElementType ENUM_DECLARATION = new HaxeElementType("ENUM_DECLARATION");
  IElementType ENUM_VALUE_DECLARATION = new HaxeElementType("ENUM_VALUE_DECLARATION");
  IElementType EXPRESSION = new HaxeElementType("EXPRESSION");
  IElementType EXPRESSION_LIST = new HaxeElementType("EXPRESSION_LIST");
  IElementType EXTENDS_DECLARATION = new HaxeElementType("EXTENDS_DECLARATION");
  IElementType EXTERN_CLASS_DECLARATION = new HaxeElementType("EXTERN_CLASS_DECLARATION");
  IElementType EXTERN_CLASS_DECLARATION_BODY = new HaxeElementType("EXTERN_CLASS_DECLARATION_BODY");
  IElementType EXTERN_FUNCTION_DECLARATION = new HaxeElementType("EXTERN_FUNCTION_DECLARATION");
  IElementType EXTERN_OR_PRIVATE = new HaxeElementType("EXTERN_OR_PRIVATE");
  IElementType FAKE_ENUM_META = new HaxeElementType("FAKE_ENUM_META");
  IElementType FAT_ARROW_EXPRESSION = new HaxeElementType("FAT_ARROW_EXPRESSION");
  IElementType FINAL_META = new HaxeElementType("FINAL_META");
  IElementType FOR_STATEMENT = new HaxeElementType("FOR_STATEMENT");
  IElementType FUNCTION_DECLARATION_WITH_ATTRIBUTES = new HaxeElementType("FUNCTION_DECLARATION_WITH_ATTRIBUTES");
  IElementType FUNCTION_LITERAL = new HaxeElementType("FUNCTION_LITERAL");
  IElementType FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES = new HaxeElementType("FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES");
  IElementType FUNCTION_TYPE = new HaxeElementType("FUNCTION_TYPE");
  IElementType GENERIC_LIST_PART = new HaxeElementType("GENERIC_LIST_PART");
  IElementType GENERIC_PARAM = new HaxeElementType("GENERIC_PARAM");
  IElementType GETTER_META = new HaxeElementType("GETTER_META");
  IElementType HACK_META = new HaxeElementType("HACK_META");
  IElementType IDENTIFIER = new HaxeElementType("IDENTIFIER");
  IElementType IF_STATEMENT = new HaxeElementType("IF_STATEMENT");
  IElementType IMPLEMENTS_DECLARATION = new HaxeElementType("IMPLEMENTS_DECLARATION");
  IElementType IMPORT_STATEMENT_REGULAR = new HaxeElementType("IMPORT_STATEMENT_REGULAR");
  IElementType IMPORT_STATEMENT_WITH_IN_SUPPORT = new HaxeElementType("IMPORT_STATEMENT_WITH_IN_SUPPORT");
  IElementType IMPORT_STATEMENT_WITH_WILDCARD = new HaxeElementType("IMPORT_STATEMENT_WITH_WILDCARD");
  IElementType INHERIT_LIST = new HaxeElementType("INHERIT_LIST");
  IElementType INTERFACE_BODY = new HaxeElementType("INTERFACE_BODY");
  IElementType INTERFACE_DECLARATION = new HaxeElementType("INTERFACE_DECLARATION");
  IElementType ITERABLE = new HaxeElementType("ITERABLE");
  IElementType ITERATOR_EXPRESSION = new HaxeElementType("ITERATOR_EXPRESSION");
  IElementType JS_REQUIRE_META = new HaxeElementType("JS_REQUIRE_META");
  IElementType KEEP_META = new HaxeElementType("KEEP_META");
  IElementType LITERAL_EXPRESSION = new HaxeElementType("LITERAL_EXPRESSION");
  IElementType LOCAL_FUNCTION_DECLARATION = new HaxeElementType("LOCAL_FUNCTION_DECLARATION");
  IElementType LOCAL_VAR_DECLARATION = new HaxeElementType("LOCAL_VAR_DECLARATION");
  IElementType LOCAL_VAR_DECLARATION_PART = new HaxeElementType("LOCAL_VAR_DECLARATION_PART");
  IElementType LOGIC_AND_EXPRESSION = new HaxeElementType("LOGIC_AND_EXPRESSION");
  IElementType LOGIC_OR_EXPRESSION = new HaxeElementType("LOGIC_OR_EXPRESSION");
  IElementType LONG_TEMPLATE_ENTRY = new HaxeElementType("LONG_TEMPLATE_ENTRY");
  IElementType MACRO_CLASS = new HaxeElementType("MACRO_CLASS");
  IElementType MACRO_CLASS_LIST = new HaxeElementType("MACRO_CLASS_LIST");
  IElementType MACRO_META = new HaxeElementType("MACRO_META");
  IElementType META_KEY_VALUE = new HaxeElementType("META_KEY_VALUE");
  IElementType META_META = new HaxeElementType("META_META");
  IElementType MULTIPLICATIVE_EXPRESSION = new HaxeElementType("MULTIPLICATIVE_EXPRESSION");
  IElementType NATIVE_META = new HaxeElementType("NATIVE_META");
  IElementType NEW_EXPRESSION = new HaxeElementType("NEW_EXPRESSION");
  IElementType NO_DEBUG_META = new HaxeElementType("NO_DEBUG_META");
  IElementType NS_META = new HaxeElementType("NS_META");
  IElementType OBJECT_LITERAL = new HaxeElementType("OBJECT_LITERAL");
  IElementType OBJECT_LITERAL_ELEMENT = new HaxeElementType("OBJECT_LITERAL_ELEMENT");
  IElementType OVERLOAD_META = new HaxeElementType("OVERLOAD_META");
  IElementType PACKAGE_STATEMENT = new HaxeElementType("PACKAGE_STATEMENT");
  IElementType PARAMETER = new HaxeElementType("PARAMETER");
  IElementType PARAMETER_LIST = new HaxeElementType("PARAMETER_LIST");
  IElementType PARENTHESIZED_EXPRESSION = new HaxeElementType("PARENTHESIZED_EXPRESSION");
  IElementType PREFIX_EXPRESSION = new HaxeElementType("PREFIX_EXPRESSION");
  IElementType PRIVATE_KEY_WORD = new HaxeElementType("PRIVATE_KEY_WORD");
  IElementType PROPERTY_ACCESSOR = new HaxeElementType("PROPERTY_ACCESSOR");
  IElementType PROPERTY_DECLARATION = new HaxeElementType("PROPERTY_DECLARATION");
  IElementType PROTECTED_META = new HaxeElementType("PROTECTED_META");
  IElementType REFERENCE_EXPRESSION = new HaxeElementType("REFERENCE_EXPRESSION");
  IElementType REGULAR_EXPRESSION_LITERAL = new HaxeElementType("REGULAR_EXPRESSION_LITERAL");
  IElementType REQUIRE_META = new HaxeElementType("REQUIRE_META");
  IElementType RETURN_STATEMENT = new HaxeElementType("RETURN_STATEMENT");
  IElementType RETURN_STATEMENT_WITHOUT_SEMICOLON = new HaxeElementType("RETURN_STATEMENT_WITHOUT_SEMICOLON");
  IElementType SETTER_META = new HaxeElementType("SETTER_META");
  IElementType SHIFT_EXPRESSION = new HaxeElementType("SHIFT_EXPRESSION");
  IElementType SHIFT_OPERATOR = new HaxeElementType("SHIFT_OPERATOR");
  IElementType SHIFT_RIGHT_OPERATOR = new HaxeElementType("SHIFT_RIGHT_OPERATOR");
  IElementType SHORT_TEMPLATE_ENTRY = new HaxeElementType("SHORT_TEMPLATE_ENTRY");
  IElementType SIMPLE_META = new HaxeElementType("SIMPLE_META");
  IElementType STRING_LITERAL_EXPRESSION = new HaxeElementType("STRING_LITERAL_EXPRESSION");
  IElementType SUFFIX_EXPRESSION = new HaxeElementType("SUFFIX_EXPRESSION");
  IElementType SUPER_EXPRESSION = new HaxeElementType("SUPER_EXPRESSION");
  IElementType SWITCH_BLOCK = new HaxeElementType("SWITCH_BLOCK");
  IElementType SWITCH_CASE = new HaxeElementType("SWITCH_CASE");
  IElementType SWITCH_CASE_BLOCK = new HaxeElementType("SWITCH_CASE_BLOCK");
  IElementType SWITCH_CASE_EXPRESSION = new HaxeElementType("SWITCH_CASE_EXPRESSION");
  IElementType SWITCH_STATEMENT = new HaxeElementType("SWITCH_STATEMENT");
  IElementType TERNARY_EXPRESSION = new HaxeElementType("TERNARY_EXPRESSION");
  IElementType THIS_EXPRESSION = new HaxeElementType("THIS_EXPRESSION");
  IElementType THROW_STATEMENT = new HaxeElementType("THROW_STATEMENT");
  IElementType TRY_STATEMENT = new HaxeElementType("TRY_STATEMENT");
  IElementType TYPE = new HaxeElementType("TYPE");
  IElementType TYPEDEF_DECLARATION = new HaxeElementType("TYPEDEF_DECLARATION");
  IElementType TYPE_EXTENDS = new HaxeElementType("TYPE_EXTENDS");
  IElementType TYPE_LIST = new HaxeElementType("TYPE_LIST");
  IElementType TYPE_LIST_PART = new HaxeElementType("TYPE_LIST_PART");
  IElementType TYPE_OR_ANONYMOUS = new HaxeElementType("TYPE_OR_ANONYMOUS");
  IElementType TYPE_PARAM = new HaxeElementType("TYPE_PARAM");
  IElementType TYPE_TAG = new HaxeElementType("TYPE_TAG");
  IElementType UNREFLECTIVE_META = new HaxeElementType("UNREFLECTIVE_META");
  IElementType UNSIGNED_SHIFT_RIGHT_OPERATOR = new HaxeElementType("UNSIGNED_SHIFT_RIGHT_OPERATOR");
  IElementType USING_STATEMENT = new HaxeElementType("USING_STATEMENT");
  IElementType VAR_DECLARATION = new HaxeElementType("VAR_DECLARATION");
  IElementType VAR_DECLARATION_PART = new HaxeElementType("VAR_DECLARATION_PART");
  IElementType VAR_INIT = new HaxeElementType("VAR_INIT");
  IElementType WHILE_STATEMENT = new HaxeElementType("WHILE_STATEMENT");
  IElementType WILDCARD = new HaxeElementType("WILDCARD");

  IElementType CLOSING_QUOTE = new HaxeElementType("CLOSING_QUOTE");
  IElementType CONDITIONAL_STATEMENT_ID = new HaxeElementType("CONDITIONAL_STATEMENT_ID");
  IElementType ID = new HaxeElementType("ID");
  IElementType KABSTRACT = new HaxeElementType("abstract");
  IElementType KAUTOBUILD = new HaxeElementType("@:autoBuild");
  IElementType KBIND = new HaxeElementType("@:bind");
  IElementType KBITMAP = new HaxeElementType("@:bitmap");
  IElementType KBREAK = new HaxeElementType("break");
  IElementType KBUILD = new HaxeElementType("@:build");
  IElementType KCASE = new HaxeElementType("case");
  IElementType KCAST = new HaxeElementType("cast");
  IElementType KCATCH = new HaxeElementType("catch");
  IElementType KCLASS = new HaxeElementType("class");
  IElementType KCONTINUE = new HaxeElementType("continue");
  IElementType KCOREAPI = new HaxeElementType("@:core_api");
  IElementType KDEBUG = new HaxeElementType("@:debug");
  IElementType KDEFAULT = new HaxeElementType("default");
  IElementType KDO = new HaxeElementType("do");
  IElementType KDYNAMIC = new HaxeElementType("dynamic");
  IElementType KELSE = new HaxeElementType("else");
  IElementType KENUM = new HaxeElementType("enum");
  IElementType KEXTENDS = new HaxeElementType("extends");
  IElementType KEXTERN = new HaxeElementType("extern");
  IElementType KFAKEENUM = new HaxeElementType("@:fakeEnum");
  IElementType KFALSE = new HaxeElementType("false");
  IElementType KFINAL = new HaxeElementType("@:final");
  IElementType KFOR = new HaxeElementType("for");
  IElementType KFUNCTION = new HaxeElementType("function");
  IElementType KGETTER = new HaxeElementType("@:getter");
  IElementType KHACK = new HaxeElementType("@:hack");
  IElementType KIF = new HaxeElementType("if");
  IElementType KIMPLEMENTS = new HaxeElementType("implements");
  IElementType KIMPORT = new HaxeElementType("import");
  IElementType KINLINE = new HaxeElementType("inline");
  IElementType KINTERFACE = new HaxeElementType("interface");
  IElementType KJSREQUIRE = new HaxeElementType("@:jsRequire");
  IElementType KKEEP = new HaxeElementType("@:keep");
  IElementType KMACRO = new HaxeElementType("@:macro");
  IElementType KMACRO2 = new HaxeElementType("macro");
  IElementType KMETA = new HaxeElementType("@:meta");
  IElementType KNATIVE = new HaxeElementType("@:native");
  IElementType KNEVER = new HaxeElementType("never");
  IElementType KNODEBUG = new HaxeElementType("@:nodebug");
  IElementType KNS = new HaxeElementType("@:ns");
  IElementType KNULL = new HaxeElementType("null");
  IElementType KOVERLOAD = new HaxeElementType("@:overload");
  IElementType KOVERRIDE = new HaxeElementType("override");
  IElementType KPACKAGE = new HaxeElementType("package");
  IElementType KPRIVATE = new HaxeElementType("private");
  IElementType KPROTECTED = new HaxeElementType("@:protected");
  IElementType KPUBLIC = new HaxeElementType("public");
  IElementType KREQUIRE = new HaxeElementType("@:require");
  IElementType KRETURN = new HaxeElementType("return");
  IElementType KSETTER = new HaxeElementType("@:setter");
  IElementType KSTATIC = new HaxeElementType("static");
  IElementType KSUPER = new HaxeElementType("super");
  IElementType KSWITCH = new HaxeElementType("switch");
  IElementType KTHIS = new HaxeElementType("this");
  IElementType KTHROW = new HaxeElementType("throw");
  IElementType KTRUE = new HaxeElementType("true");
  IElementType KTRY = new HaxeElementType("try");
  IElementType KTYPEDEF = new HaxeElementType("typedef");
  IElementType KUNTYPED = new HaxeElementType("untyped");
  IElementType KUSING = new HaxeElementType("using");
  IElementType KVAR = new HaxeElementType("var");
  IElementType KWHILE = new HaxeElementType("while");
  IElementType LITFLOAT = new HaxeElementType("LITFLOAT");
  IElementType LITHEX = new HaxeElementType("LITHEX");
  IElementType LITINT = new HaxeElementType("LITINT");
  IElementType LITOCT = new HaxeElementType("LITOCT");
  IElementType LONG_TEMPLATE_ENTRY_END = new HaxeElementType("LONG_TEMPLATE_ENTRY_END");
  IElementType LONG_TEMPLATE_ENTRY_START = new HaxeElementType("LONG_TEMPLATE_ENTRY_START");
  IElementType MACRO_ID = new HaxeElementType("MACRO_ID");
  IElementType OARROW = new HaxeElementType("->");
  IElementType OASSIGN = new HaxeElementType("=");
  IElementType OBIT_AND = new HaxeElementType("&");
  IElementType OBIT_AND_ASSIGN = new HaxeElementType("&=");
  IElementType OBIT_OR = new HaxeElementType("|");
  IElementType OBIT_OR_ASSIGN = new HaxeElementType("|=");
  IElementType OBIT_XOR = new HaxeElementType("^");
  IElementType OBIT_XOR_ASSIGN = new HaxeElementType("^=");
  IElementType OCOLON = new HaxeElementType(":");
  IElementType OCOMMA = new HaxeElementType(",");
  IElementType OCOMPLEMENT = new HaxeElementType("~");
  IElementType OCOND_AND = new HaxeElementType("&&");
  IElementType OCOND_OR = new HaxeElementType("||");
  IElementType ODOT = new HaxeElementType(".");
  IElementType OEQ = new HaxeElementType("==");
  IElementType OFAT_ARROW = new HaxeElementType("=>");
  IElementType OGREATER = new HaxeElementType(">");
  IElementType OIN = new HaxeElementType("in");
  IElementType OLESS = new HaxeElementType("<");
  IElementType OLESS_OR_EQUAL = new HaxeElementType("<=");
  IElementType OMINUS = new HaxeElementType("-");
  IElementType OMINUS_ASSIGN = new HaxeElementType("-=");
  IElementType OMINUS_MINUS = new HaxeElementType("--");
  IElementType OMUL = new HaxeElementType("*");
  IElementType OMUL_ASSIGN = new HaxeElementType("*=");
  IElementType ONEW = new HaxeElementType("new");
  IElementType ONOT = new HaxeElementType("!");
  IElementType ONOT_EQ = new HaxeElementType("!=");
  IElementType OPEN_QUOTE = new HaxeElementType("OPEN_QUOTE");
  IElementType OPLUS = new HaxeElementType("+");
  IElementType OPLUS_ASSIGN = new HaxeElementType("+=");
  IElementType OPLUS_PLUS = new HaxeElementType("++");
  IElementType OQUEST = new HaxeElementType("?");
  IElementType OQUOTIENT = new HaxeElementType("/");
  IElementType OQUOTIENT_ASSIGN = new HaxeElementType("/=");
  IElementType OREMAINDER = new HaxeElementType("%");
  IElementType OREMAINDER_ASSIGN = new HaxeElementType("%=");
  IElementType OSEMI = new HaxeElementType(";");
  IElementType OSHIFT_LEFT = new HaxeElementType("<<");
  IElementType OSHIFT_LEFT_ASSIGN = new HaxeElementType("<<=");
  IElementType OTRIPLE_DOT = new HaxeElementType("...");
  IElementType PLBRACK = new HaxeElementType("[");
  IElementType PLCURLY = new HaxeElementType("{");
  IElementType PLPAREN = new HaxeElementType("(");
  IElementType PPELSE = new HaxeElementType("#else");
  IElementType PPELSEIF = new HaxeElementType("#elseif");
  IElementType PPEND = new HaxeElementType("#end");
  IElementType PPERROR = new HaxeElementType("#error");
  IElementType PPIF = new HaxeElementType("#if");
  IElementType PRBRACK = new HaxeElementType("]");
  IElementType PRCURLY = new HaxeElementType("}");
  IElementType PRPAREN = new HaxeElementType(")");
  IElementType REGULAR_STRING_PART = new HaxeElementType("REGULAR_STRING_PART");
  IElementType REG_EXP = new HaxeElementType("REG_EXP");
  IElementType SHORT_TEMPLATE_ENTRY_START = new HaxeElementType("SHORT_TEMPLATE_ENTRY_START");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == ABSTRACT_CLASS_DECLARATION) {
        return new HaxeAbstractClassDeclarationImpl(node);
      }
      else if (type == ACCESS) {
        return new HaxeAccessImpl(node);
      }
      else if (type == ADDITIVE_EXPRESSION) {
        return new HaxeAdditiveExpressionImpl(node);
      }
      else if (type == ANONYMOUS_FUNCTION_DECLARATION) {
        return new HaxeAnonymousFunctionDeclarationImpl(node);
      }
      else if (type == ANONYMOUS_TYPE) {
        return new HaxeAnonymousTypeImpl(node);
      }
      else if (type == ANONYMOUS_TYPE_BODY) {
        return new HaxeAnonymousTypeBodyImpl(node);
      }
      else if (type == ANONYMOUS_TYPE_FIELD) {
        return new HaxeAnonymousTypeFieldImpl(node);
      }
      else if (type == ANONYMOUS_TYPE_FIELD_LIST) {
        return new HaxeAnonymousTypeFieldListImpl(node);
      }
      else if (type == ARRAY_ACCESS_EXPRESSION) {
        return new HaxeArrayAccessExpressionImpl(node);
      }
      else if (type == ARRAY_LITERAL) {
        return new HaxeArrayLiteralImpl(node);
      }
      else if (type == ASSIGN_EXPRESSION) {
        return new HaxeAssignExpressionImpl(node);
      }
      else if (type == ASSIGN_OPERATION) {
        return new HaxeAssignOperationImpl(node);
      }
      else if (type == AUTO_BUILD_MACRO) {
        return new HaxeAutoBuildMacroImpl(node);
      }
      else if (type == BIND_META) {
        return new HaxeBindMetaImpl(node);
      }
      else if (type == BITMAP_META) {
        return new HaxeBitmapMetaImpl(node);
      }
      else if (type == BITWISE_EXPRESSION) {
        return new HaxeBitwiseExpressionImpl(node);
      }
      else if (type == BIT_OPERATION) {
        return new HaxeBitOperationImpl(node);
      }
      else if (type == BLOCK_STATEMENT) {
        return new HaxeBlockStatementImpl(node);
      }
      else if (type == BREAK_STATEMENT) {
        return new HaxeBreakStatementImpl(node);
      }
      else if (type == BUILD_MACRO) {
        return new HaxeBuildMacroImpl(node);
      }
      else if (type == CALL_EXPRESSION) {
        return new HaxeCallExpressionImpl(node);
      }
      else if (type == CAST_EXPRESSION) {
        return new HaxeCastExpressionImpl(node);
      }
      else if (type == CATCH_STATEMENT) {
        return new HaxeCatchStatementImpl(node);
      }
      else if (type == CLASS_BODY) {
        return new HaxeClassBodyImpl(node);
      }
      else if (type == CLASS_DECLARATION) {
        return new HaxeClassDeclarationImpl(node);
      }
      else if (type == COMPARE_EXPRESSION) {
        return new HaxeCompareExpressionImpl(node);
      }
      else if (type == COMPARE_OPERATION) {
        return new HaxeCompareOperationImpl(node);
      }
      else if (type == COMPONENT_NAME) {
        return new HaxeComponentNameImpl(node);
      }
      else if (type == CONDITIONAL) {
        return new HaxeConditionalImpl(node);
      }
      else if (type == CONSTRUCTOR_NAME) {
        return new HaxeConstructorNameImpl(node);
      }
      else if (type == CONTINUE_STATEMENT) {
        return new HaxeContinueStatementImpl(node);
      }
      else if (type == CORE_API_META) {
        return new HaxeCoreApiMetaImpl(node);
      }
      else if (type == CUSTOM_META) {
        return new HaxeCustomMetaImpl(node);
      }
      else if (type == DEBUG_META) {
        return new HaxeDebugMetaImpl(node);
      }
      else if (type == DECLARATION_ATTRIBUTE) {
        return new HaxeDeclarationAttributeImpl(node);
      }
      else if (type == DEFAULT_CASE) {
        return new HaxeDefaultCaseImpl(node);
      }
      else if (type == DO_WHILE_STATEMENT) {
        return new HaxeDoWhileStatementImpl(node);
      }
      else if (type == ENUM_BODY) {
        return new HaxeEnumBodyImpl(node);
      }
      else if (type == ENUM_CONSTRUCTOR_PARAMETERS) {
        return new HaxeEnumConstructorParametersImpl(node);
      }
      else if (type == ENUM_DECLARATION) {
        return new HaxeEnumDeclarationImpl(node);
      }
      else if (type == ENUM_VALUE_DECLARATION) {
        return new HaxeEnumValueDeclarationImpl(node);
      }
      else if (type == EXPRESSION) {
        return new HaxeExpressionImpl(node);
      }
      else if (type == EXPRESSION_LIST) {
        return new HaxeExpressionListImpl(node);
      }
      else if (type == EXTENDS_DECLARATION) {
        return new HaxeExtendsDeclarationImpl(node);
      }
      else if (type == EXTERN_CLASS_DECLARATION) {
        return new HaxeExternClassDeclarationImpl(node);
      }
      else if (type == EXTERN_CLASS_DECLARATION_BODY) {
        return new HaxeExternClassDeclarationBodyImpl(node);
      }
      else if (type == EXTERN_FUNCTION_DECLARATION) {
        return new HaxeExternFunctionDeclarationImpl(node);
      }
      else if (type == EXTERN_OR_PRIVATE) {
        return new HaxeExternOrPrivateImpl(node);
      }
      else if (type == FAKE_ENUM_META) {
        return new HaxeFakeEnumMetaImpl(node);
      }
      else if (type == FAT_ARROW_EXPRESSION) {
        return new HaxeFatArrowExpressionImpl(node);
      }
      else if (type == FINAL_META) {
        return new HaxeFinalMetaImpl(node);
      }
      else if (type == FOR_STATEMENT) {
        return new HaxeForStatementImpl(node);
      }
      else if (type == FUNCTION_DECLARATION_WITH_ATTRIBUTES) {
        return new HaxeFunctionDeclarationWithAttributesImpl(node);
      }
      else if (type == FUNCTION_LITERAL) {
        return new HaxeFunctionLiteralImpl(node);
      }
      else if (type == FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES) {
        return new HaxeFunctionPrototypeDeclarationWithAttributesImpl(node);
      }
      else if (type == FUNCTION_TYPE) {
        return new HaxeFunctionTypeImpl(node);
      }
      else if (type == GENERIC_LIST_PART) {
        return new HaxeGenericListPartImpl(node);
      }
      else if (type == GENERIC_PARAM) {
        return new HaxeGenericParamImpl(node);
      }
      else if (type == GETTER_META) {
        return new HaxeGetterMetaImpl(node);
      }
      else if (type == HACK_META) {
        return new HaxeHackMetaImpl(node);
      }
      else if (type == IDENTIFIER) {
        return new HaxeIdentifierImpl(node);
      }
      else if (type == IF_STATEMENT) {
        return new HaxeIfStatementImpl(node);
      }
      else if (type == IMPLEMENTS_DECLARATION) {
        return new HaxeImplementsDeclarationImpl(node);
      }
      else if (type == IMPORT_STATEMENT_REGULAR) {
        return new HaxeImportStatementRegularImpl(node);
      }
      else if (type == IMPORT_STATEMENT_WITH_IN_SUPPORT) {
        return new HaxeImportStatementWithInSupportImpl(node);
      }
      else if (type == IMPORT_STATEMENT_WITH_WILDCARD) {
        return new HaxeImportStatementWithWildcardImpl(node);
      }
      else if (type == INHERIT_LIST) {
        return new HaxeInheritListImpl(node);
      }
      else if (type == INTERFACE_BODY) {
        return new HaxeInterfaceBodyImpl(node);
      }
      else if (type == INTERFACE_DECLARATION) {
        return new HaxeInterfaceDeclarationImpl(node);
      }
      else if (type == ITERABLE) {
        return new HaxeIterableImpl(node);
      }
      else if (type == ITERATOR_EXPRESSION) {
        return new HaxeIteratorExpressionImpl(node);
      }
      else if (type == JS_REQUIRE_META) {
        return new HaxeJsRequireMetaImpl(node);
      }
      else if (type == KEEP_META) {
        return new HaxeKeepMetaImpl(node);
      }
      else if (type == LITERAL_EXPRESSION) {
        return new HaxeLiteralExpressionImpl(node);
      }
      else if (type == LOCAL_FUNCTION_DECLARATION) {
        return new HaxeLocalFunctionDeclarationImpl(node);
      }
      else if (type == LOCAL_VAR_DECLARATION) {
        return new HaxeLocalVarDeclarationImpl(node);
      }
      else if (type == LOCAL_VAR_DECLARATION_PART) {
        return new HaxeLocalVarDeclarationPartImpl(node);
      }
      else if (type == LOGIC_AND_EXPRESSION) {
        return new HaxeLogicAndExpressionImpl(node);
      }
      else if (type == LOGIC_OR_EXPRESSION) {
        return new HaxeLogicOrExpressionImpl(node);
      }
      else if (type == LONG_TEMPLATE_ENTRY) {
        return new HaxeLongTemplateEntryImpl(node);
      }
      else if (type == MACRO_CLASS) {
        return new HaxeMacroClassImpl(node);
      }
      else if (type == MACRO_CLASS_LIST) {
        return new HaxeMacroClassListImpl(node);
      }
      else if (type == MACRO_META) {
        return new HaxeMacroMetaImpl(node);
      }
      else if (type == META_KEY_VALUE) {
        return new HaxeMetaKeyValueImpl(node);
      }
      else if (type == META_META) {
        return new HaxeMetaMetaImpl(node);
      }
      else if (type == MULTIPLICATIVE_EXPRESSION) {
        return new HaxeMultiplicativeExpressionImpl(node);
      }
      else if (type == NATIVE_META) {
        return new HaxeNativeMetaImpl(node);
      }
      else if (type == NEW_EXPRESSION) {
        return new HaxeNewExpressionImpl(node);
      }
      else if (type == NO_DEBUG_META) {
        return new HaxeNoDebugMetaImpl(node);
      }
      else if (type == NS_META) {
        return new HaxeNsMetaImpl(node);
      }
      else if (type == OBJECT_LITERAL) {
        return new HaxeObjectLiteralImpl(node);
      }
      else if (type == OBJECT_LITERAL_ELEMENT) {
        return new HaxeObjectLiteralElementImpl(node);
      }
      else if (type == OVERLOAD_META) {
        return new HaxeOverloadMetaImpl(node);
      }
      else if (type == PACKAGE_STATEMENT) {
        return new HaxePackageStatementImpl(node);
      }
      else if (type == PARAMETER) {
        return new HaxeParameterImpl(node);
      }
      else if (type == PARAMETER_LIST) {
        return new HaxeParameterListImpl(node);
      }
      else if (type == PARENTHESIZED_EXPRESSION) {
        return new HaxeParenthesizedExpressionImpl(node);
      }
      else if (type == PREFIX_EXPRESSION) {
        return new HaxePrefixExpressionImpl(node);
      }
      else if (type == PRIVATE_KEY_WORD) {
        return new HaxePrivateKeyWordImpl(node);
      }
      else if (type == PROPERTY_ACCESSOR) {
        return new HaxePropertyAccessorImpl(node);
      }
      else if (type == PROPERTY_DECLARATION) {
        return new HaxePropertyDeclarationImpl(node);
      }
      else if (type == PROTECTED_META) {
        return new HaxeProtectedMetaImpl(node);
      }
      else if (type == REFERENCE_EXPRESSION) {
        return new HaxeReferenceExpressionImpl(node);
      }
      else if (type == REGULAR_EXPRESSION_LITERAL) {
        return new HaxeRegularExpressionLiteralImpl(node);
      }
      else if (type == REQUIRE_META) {
        return new HaxeRequireMetaImpl(node);
      }
      else if (type == RETURN_STATEMENT) {
        return new HaxeReturnStatementImpl(node);
      }
      else if (type == RETURN_STATEMENT_WITHOUT_SEMICOLON) {
        return new HaxeReturnStatementWithoutSemicolonImpl(node);
      }
      else if (type == SETTER_META) {
        return new HaxeSetterMetaImpl(node);
      }
      else if (type == SHIFT_EXPRESSION) {
        return new HaxeShiftExpressionImpl(node);
      }
      else if (type == SHIFT_OPERATOR) {
        return new HaxeShiftOperatorImpl(node);
      }
      else if (type == SHIFT_RIGHT_OPERATOR) {
        return new HaxeShiftRightOperatorImpl(node);
      }
      else if (type == SHORT_TEMPLATE_ENTRY) {
        return new HaxeShortTemplateEntryImpl(node);
      }
      else if (type == SIMPLE_META) {
        return new HaxeSimpleMetaImpl(node);
      }
      else if (type == STRING_LITERAL_EXPRESSION) {
        return new HaxeStringLiteralExpressionImpl(node);
      }
      else if (type == SUFFIX_EXPRESSION) {
        return new HaxeSuffixExpressionImpl(node);
      }
      else if (type == SUPER_EXPRESSION) {
        return new HaxeSuperExpressionImpl(node);
      }
      else if (type == SWITCH_BLOCK) {
        return new HaxeSwitchBlockImpl(node);
      }
      else if (type == SWITCH_CASE) {
        return new HaxeSwitchCaseImpl(node);
      }
      else if (type == SWITCH_CASE_BLOCK) {
        return new HaxeSwitchCaseBlockImpl(node);
      }
      else if (type == SWITCH_CASE_EXPRESSION) {
        return new HaxeSwitchCaseExpressionImpl(node);
      }
      else if (type == SWITCH_STATEMENT) {
        return new HaxeSwitchStatementImpl(node);
      }
      else if (type == TERNARY_EXPRESSION) {
        return new HaxeTernaryExpressionImpl(node);
      }
      else if (type == THIS_EXPRESSION) {
        return new HaxeThisExpressionImpl(node);
      }
      else if (type == THROW_STATEMENT) {
        return new HaxeThrowStatementImpl(node);
      }
      else if (type == TRY_STATEMENT) {
        return new HaxeTryStatementImpl(node);
      }
      else if (type == TYPE) {
        return new HaxeTypeImpl(node);
      }
      else if (type == TYPEDEF_DECLARATION) {
        return new HaxeTypedefDeclarationImpl(node);
      }
      else if (type == TYPE_EXTENDS) {
        return new HaxeTypeExtendsImpl(node);
      }
      else if (type == TYPE_LIST) {
        return new HaxeTypeListImpl(node);
      }
      else if (type == TYPE_LIST_PART) {
        return new HaxeTypeListPartImpl(node);
      }
      else if (type == TYPE_OR_ANONYMOUS) {
        return new HaxeTypeOrAnonymousImpl(node);
      }
      else if (type == TYPE_PARAM) {
        return new HaxeTypeParamImpl(node);
      }
      else if (type == TYPE_TAG) {
        return new HaxeTypeTagImpl(node);
      }
      else if (type == UNREFLECTIVE_META) {
        return new HaxeUnreflectiveMetaImpl(node);
      }
      else if (type == UNSIGNED_SHIFT_RIGHT_OPERATOR) {
        return new HaxeUnsignedShiftRightOperatorImpl(node);
      }
      else if (type == USING_STATEMENT) {
        return new HaxeUsingStatementImpl(node);
      }
      else if (type == VAR_DECLARATION) {
        return new HaxeVarDeclarationImpl(node);
      }
      else if (type == VAR_DECLARATION_PART) {
        return new HaxeVarDeclarationPartImpl(node);
      }
      else if (type == VAR_INIT) {
        return new HaxeVarInitImpl(node);
      }
      else if (type == WHILE_STATEMENT) {
        return new HaxeWhileStatementImpl(node);
      }
      else if (type == WILDCARD) {
        return new HaxeWildcardImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
