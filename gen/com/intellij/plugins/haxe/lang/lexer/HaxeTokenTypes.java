// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.*;

public interface HaxeTokenTypes {

  IElementType HAXE_ACCESS = new HaxeElementType("HAXE_ACCESS");
  IElementType HAXE_ADDITIVE_EXPRESSION = new HaxeElementType("HAXE_ADDITIVE_EXPRESSION");
  IElementType HAXE_ANONYMOUS_FUNCTION_DECLARATION = new HaxeElementType("HAXE_ANONYMOUS_FUNCTION_DECLARATION");
  IElementType HAXE_ANONYMOUS_TYPE = new HaxeElementType("HAXE_ANONYMOUS_TYPE");
  IElementType HAXE_ANONYMOUS_TYPE_BODY = new HaxeElementType("HAXE_ANONYMOUS_TYPE_BODY");
  IElementType HAXE_ANONYMOUS_TYPE_FIELD = new HaxeElementType("HAXE_ANONYMOUS_TYPE_FIELD");
  IElementType HAXE_ANONYMOUS_TYPE_FIELD_LIST = new HaxeElementType("HAXE_ANONYMOUS_TYPE_FIELD_LIST");
  IElementType HAXE_ARRAY_ACCESS_EXPRESSION = new HaxeElementType("HAXE_ARRAY_ACCESS_EXPRESSION");
  IElementType HAXE_ARRAY_LITERAL = new HaxeElementType("HAXE_ARRAY_LITERAL");
  IElementType HAXE_ASSIGN_EXPRESSION = new HaxeElementType("HAXE_ASSIGN_EXPRESSION");
  IElementType HAXE_ASSIGN_OPERATION = new HaxeElementType("HAXE_ASSIGN_OPERATION");
  IElementType HAXE_AUTO_BUILD_MACRO = new HaxeElementType("HAXE_AUTO_BUILD_MACRO");
  IElementType HAXE_BITMAP_META = new HaxeElementType("HAXE_BITMAP_META");
  IElementType HAXE_BITWISE_EXPRESSION = new HaxeElementType("HAXE_BITWISE_EXPRESSION");
  IElementType HAXE_BIT_OPERATION = new HaxeElementType("HAXE_BIT_OPERATION");
  IElementType HAXE_BLOCK_STATEMENT = new HaxeElementType("HAXE_BLOCK_STATEMENT");
  IElementType HAXE_BREAK_STATEMENT = new HaxeElementType("HAXE_BREAK_STATEMENT");
  IElementType HAXE_BUILD_MACRO = new HaxeElementType("HAXE_BUILD_MACRO");
  IElementType HAXE_CALL_EXPRESSION = new HaxeElementType("HAXE_CALL_EXPRESSION");
  IElementType HAXE_CAST_EXPRESSION = new HaxeElementType("HAXE_CAST_EXPRESSION");
  IElementType HAXE_CATCH_STATEMENT = new HaxeElementType("HAXE_CATCH_STATEMENT");
  IElementType HAXE_CLASS_BODY = new HaxeElementType("HAXE_CLASS_BODY");
  IElementType HAXE_CLASS_DECLARATION = new HaxeElementType("HAXE_CLASS_DECLARATION");
  IElementType HAXE_COMPARE_EXPRESSION = new HaxeElementType("HAXE_COMPARE_EXPRESSION");
  IElementType HAXE_COMPARE_OPERATION = new HaxeElementType("HAXE_COMPARE_OPERATION");
  IElementType HAXE_COMPONENT_NAME = new HaxeElementType("HAXE_COMPONENT_NAME");
  IElementType HAXE_CONTINUE_STATEMENT = new HaxeElementType("HAXE_CONTINUE_STATEMENT");
  IElementType HAXE_CUSTOM_META = new HaxeElementType("HAXE_CUSTOM_META");
  IElementType HAXE_DECLARATION_ATTRIBUTE = new HaxeElementType("HAXE_DECLARATION_ATTRIBUTE");
  IElementType HAXE_DECLARATION_ATTRIBUTE_LIST = new HaxeElementType("HAXE_DECLARATION_ATTRIBUTE_LIST");
  IElementType HAXE_DEFAULT_CASE = new HaxeElementType("HAXE_DEFAULT_CASE");
  IElementType HAXE_DO_WHILE_STATEMENT = new HaxeElementType("HAXE_DO_WHILE_STATEMENT");
  IElementType HAXE_ENUM_BODY = new HaxeElementType("HAXE_ENUM_BODY");
  IElementType HAXE_ENUM_CONSTRUCTOR_PARAMETERS = new HaxeElementType("HAXE_ENUM_CONSTRUCTOR_PARAMETERS");
  IElementType HAXE_ENUM_DECLARATION = new HaxeElementType("HAXE_ENUM_DECLARATION");
  IElementType HAXE_ENUM_VALUE_DECLARATION = new HaxeElementType("HAXE_ENUM_VALUE_DECLARATION");
  IElementType HAXE_EXPRESSION = new HaxeElementType("HAXE_EXPRESSION");
  IElementType HAXE_EXPRESSION_LIST = new HaxeElementType("HAXE_EXPRESSION_LIST");
  IElementType HAXE_EXTERN_CLASS_DECLARATION = new HaxeElementType("HAXE_EXTERN_CLASS_DECLARATION");
  IElementType HAXE_EXTERN_CLASS_DECLARATION_BODY = new HaxeElementType("HAXE_EXTERN_CLASS_DECLARATION_BODY");
  IElementType HAXE_EXTERN_FUNCTION_DECLARATION = new HaxeElementType("HAXE_EXTERN_FUNCTION_DECLARATION");
  IElementType HAXE_EXTERN_OR_PRIVATE = new HaxeElementType("HAXE_EXTERN_OR_PRIVATE");
  IElementType HAXE_FAKE_ENUM_META = new HaxeElementType("HAXE_FAKE_ENUM_META");
  IElementType HAXE_FOR_STATEMENT = new HaxeElementType("HAXE_FOR_STATEMENT");
  IElementType HAXE_FUNCTION_DECLARATION_WITH_ATTRIBUTES = new HaxeElementType("HAXE_FUNCTION_DECLARATION_WITH_ATTRIBUTES");
  IElementType HAXE_FUNCTION_LITERAL = new HaxeElementType("HAXE_FUNCTION_LITERAL");
  IElementType HAXE_FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES = new HaxeElementType("HAXE_FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES");
  IElementType HAXE_FUNCTION_TYPE = new HaxeElementType("HAXE_FUNCTION_TYPE");
  IElementType HAXE_GENERIC_LIST_PART = new HaxeElementType("HAXE_GENERIC_LIST_PART");
  IElementType HAXE_GENERIC_PARAM = new HaxeElementType("HAXE_GENERIC_PARAM");
  IElementType HAXE_GETTER_META = new HaxeElementType("HAXE_GETTER_META");
  IElementType HAXE_IDENTIFIER = new HaxeElementType("HAXE_IDENTIFIER");
  IElementType HAXE_IF_STATEMENT = new HaxeElementType("HAXE_IF_STATEMENT");
  IElementType HAXE_IMPORT_STATEMENT = new HaxeElementType("HAXE_IMPORT_STATEMENT");
  IElementType HAXE_INHERIT = new HaxeElementType("HAXE_INHERIT");
  IElementType HAXE_INHERIT_LIST = new HaxeElementType("HAXE_INHERIT_LIST");
  IElementType HAXE_INTERFACE_BODY = new HaxeElementType("HAXE_INTERFACE_BODY");
  IElementType HAXE_INTERFACE_DECLARATION = new HaxeElementType("HAXE_INTERFACE_DECLARATION");
  IElementType HAXE_ITERABLE = new HaxeElementType("HAXE_ITERABLE");
  IElementType HAXE_ITERATOR_EXPRESSION = new HaxeElementType("HAXE_ITERATOR_EXPRESSION");
  IElementType HAXE_LITERAL_EXPRESSION = new HaxeElementType("HAXE_LITERAL_EXPRESSION");
  IElementType HAXE_LOCAL_FUNCTION_DECLARATION = new HaxeElementType("HAXE_LOCAL_FUNCTION_DECLARATION");
  IElementType HAXE_LOCAL_VAR_DECLARATION = new HaxeElementType("HAXE_LOCAL_VAR_DECLARATION");
  IElementType HAXE_LOCAL_VAR_DECLARATION_PART = new HaxeElementType("HAXE_LOCAL_VAR_DECLARATION_PART");
  IElementType HAXE_LOGIC_AND_EXPRESSION = new HaxeElementType("HAXE_LOGIC_AND_EXPRESSION");
  IElementType HAXE_LOGIC_OR_EXPRESSION = new HaxeElementType("HAXE_LOGIC_OR_EXPRESSION");
  IElementType HAXE_LONG_TEMPLATE_ENTRY = new HaxeElementType("HAXE_LONG_TEMPLATE_ENTRY");
  IElementType HAXE_META_KEY_VALUE = new HaxeElementType("HAXE_META_KEY_VALUE");
  IElementType HAXE_META_META = new HaxeElementType("HAXE_META_META");
  IElementType HAXE_MULTIPLICATIVE_EXPRESSION = new HaxeElementType("HAXE_MULTIPLICATIVE_EXPRESSION");
  IElementType HAXE_NATIVE_META = new HaxeElementType("HAXE_NATIVE_META");
  IElementType HAXE_NEW_EXPRESSION = new HaxeElementType("HAXE_NEW_EXPRESSION");
  IElementType HAXE_NS_META = new HaxeElementType("HAXE_NS_META");
  IElementType HAXE_OBJECT_LITERAL = new HaxeElementType("HAXE_OBJECT_LITERAL");
  IElementType HAXE_OBJECT_LITERAL_ELEMENT = new HaxeElementType("HAXE_OBJECT_LITERAL_ELEMENT");
  IElementType HAXE_OVERLOAD_META = new HaxeElementType("HAXE_OVERLOAD_META");
  IElementType HAXE_PACKAGE_STATEMENT = new HaxeElementType("HAXE_PACKAGE_STATEMENT");
  IElementType HAXE_PARAMETER = new HaxeElementType("HAXE_PARAMETER");
  IElementType HAXE_PARAMETER_LIST = new HaxeElementType("HAXE_PARAMETER_LIST");
  IElementType HAXE_PARENTHESIZED_EXPRESSION = new HaxeElementType("HAXE_PARENTHESIZED_EXPRESSION");
  IElementType HAXE_PREFIX_EXPRESSION = new HaxeElementType("HAXE_PREFIX_EXPRESSION");
  IElementType HAXE_PROPERTY_ACCESSOR = new HaxeElementType("HAXE_PROPERTY_ACCESSOR");
  IElementType HAXE_PROPERTY_DECLARATION = new HaxeElementType("HAXE_PROPERTY_DECLARATION");
  IElementType HAXE_REFERENCE_EXPRESSION = new HaxeElementType("HAXE_REFERENCE_EXPRESSION");
  IElementType HAXE_REGULAR_EXPRESSION_LITERAL = new HaxeElementType("HAXE_REGULAR_EXPRESSION_LITERAL");
  IElementType HAXE_REQUIRE_META = new HaxeElementType("HAXE_REQUIRE_META");
  IElementType HAXE_RETURN_STATEMENT = new HaxeElementType("HAXE_RETURN_STATEMENT");
  IElementType HAXE_RETURN_STATEMENT_WITHOUT_SEMICOLON = new HaxeElementType("HAXE_RETURN_STATEMENT_WITHOUT_SEMICOLON");
  IElementType HAXE_SETTER_META = new HaxeElementType("HAXE_SETTER_META");
  IElementType HAXE_SHIFT_EXPRESSION = new HaxeElementType("HAXE_SHIFT_EXPRESSION");
  IElementType HAXE_SHIFT_OPERATOR = new HaxeElementType("HAXE_SHIFT_OPERATOR");
  IElementType HAXE_SHIFT_RIGHT_OPERATOR = new HaxeElementType("HAXE_SHIFT_RIGHT_OPERATOR");
  IElementType HAXE_SHORT_TEMPLATE_ENTRY = new HaxeElementType("HAXE_SHORT_TEMPLATE_ENTRY");
  IElementType HAXE_STRING_LITERAL_EXPRESSION = new HaxeElementType("HAXE_STRING_LITERAL_EXPRESSION");
  IElementType HAXE_SUFFIX_EXPRESSION = new HaxeElementType("HAXE_SUFFIX_EXPRESSION");
  IElementType HAXE_SUPER_EXPRESSION = new HaxeElementType("HAXE_SUPER_EXPRESSION");
  IElementType HAXE_SWITCH_BLOCK = new HaxeElementType("HAXE_SWITCH_BLOCK");
  IElementType HAXE_SWITCH_CASE = new HaxeElementType("HAXE_SWITCH_CASE");
  IElementType HAXE_SWITCH_CASE_BLOCK = new HaxeElementType("HAXE_SWITCH_CASE_BLOCK");
  IElementType HAXE_SWITCH_CASE_EXPRESSION = new HaxeElementType("HAXE_SWITCH_CASE_EXPRESSION");
  IElementType HAXE_SWITCH_STATEMENT = new HaxeElementType("HAXE_SWITCH_STATEMENT");
  IElementType HAXE_TERNARY_EXPRESSION = new HaxeElementType("HAXE_TERNARY_EXPRESSION");
  IElementType HAXE_THIS_EXPRESSION = new HaxeElementType("HAXE_THIS_EXPRESSION");
  IElementType HAXE_THROW_STATEMENT = new HaxeElementType("HAXE_THROW_STATEMENT");
  IElementType HAXE_TRY_STATEMENT = new HaxeElementType("HAXE_TRY_STATEMENT");
  IElementType HAXE_TYPE = new HaxeElementType("HAXE_TYPE");
  IElementType HAXE_TYPEDEF_DECLARATION = new HaxeElementType("HAXE_TYPEDEF_DECLARATION");
  IElementType HAXE_TYPE_EXTENDS = new HaxeElementType("HAXE_TYPE_EXTENDS");
  IElementType HAXE_TYPE_LIST = new HaxeElementType("HAXE_TYPE_LIST");
  IElementType HAXE_TYPE_LIST_PART = new HaxeElementType("HAXE_TYPE_LIST_PART");
  IElementType HAXE_TYPE_OR_ANONYMOUS = new HaxeElementType("HAXE_TYPE_OR_ANONYMOUS");
  IElementType HAXE_TYPE_PARAM = new HaxeElementType("HAXE_TYPE_PARAM");
  IElementType HAXE_TYPE_TAG = new HaxeElementType("HAXE_TYPE_TAG");
  IElementType HAXE_UNSIGNED_SHIFT_RIGHT_OPERATOR = new HaxeElementType("HAXE_UNSIGNED_SHIFT_RIGHT_OPERATOR");
  IElementType HAXE_USING_STATEMENT = new HaxeElementType("HAXE_USING_STATEMENT");
  IElementType HAXE_VAR_DECLARATION = new HaxeElementType("HAXE_VAR_DECLARATION");
  IElementType HAXE_VAR_DECLARATION_PART = new HaxeElementType("HAXE_VAR_DECLARATION_PART");
  IElementType HAXE_VAR_INIT = new HaxeElementType("HAXE_VAR_INIT");
  IElementType HAXE_WHILE_STATEMENT = new HaxeElementType("HAXE_WHILE_STATEMENT");

  IElementType CLOSING_QUOTE = new HaxeElementType("CLOSING_QUOTE");
  IElementType ID = new HaxeElementType("ID");
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
  IElementType KKEEP = new HaxeElementType("@:keep");
  IElementType KMACRO = new HaxeElementType("@:macro");
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
  IElementType OGREATER = new HaxeElementType(">");
  IElementType OGREATER_OR_EQUAL = new HaxeElementType(">=");
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
  IElementType OSHIFT_RIGHT_ASSIGN = new HaxeElementType(">>=");
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
       if (type == HAXE_ACCESS) {
        return new HaxeAccessImpl(node);
      }
      else if (type == HAXE_ADDITIVE_EXPRESSION) {
        return new HaxeAdditiveExpressionImpl(node);
      }
      else if (type == HAXE_ANONYMOUS_FUNCTION_DECLARATION) {
        return new HaxeAnonymousFunctionDeclarationImpl(node);
      }
      else if (type == HAXE_ANONYMOUS_TYPE) {
        return new HaxeAnonymousTypeImpl(node);
      }
      else if (type == HAXE_ANONYMOUS_TYPE_BODY) {
        return new HaxeAnonymousTypeBodyImpl(node);
      }
      else if (type == HAXE_ANONYMOUS_TYPE_FIELD) {
        return new HaxeAnonymousTypeFieldImpl(node);
      }
      else if (type == HAXE_ANONYMOUS_TYPE_FIELD_LIST) {
        return new HaxeAnonymousTypeFieldListImpl(node);
      }
      else if (type == HAXE_ARRAY_ACCESS_EXPRESSION) {
        return new HaxeArrayAccessExpressionImpl(node);
      }
      else if (type == HAXE_ARRAY_LITERAL) {
        return new HaxeArrayLiteralImpl(node);
      }
      else if (type == HAXE_ASSIGN_EXPRESSION) {
        return new HaxeAssignExpressionImpl(node);
      }
      else if (type == HAXE_ASSIGN_OPERATION) {
        return new HaxeAssignOperationImpl(node);
      }
      else if (type == HAXE_AUTO_BUILD_MACRO) {
        return new HaxeAutoBuildMacroImpl(node);
      }
      else if (type == HAXE_BITMAP_META) {
        return new HaxeBitmapMetaImpl(node);
      }
      else if (type == HAXE_BITWISE_EXPRESSION) {
        return new HaxeBitwiseExpressionImpl(node);
      }
      else if (type == HAXE_BIT_OPERATION) {
        return new HaxeBitOperationImpl(node);
      }
      else if (type == HAXE_BLOCK_STATEMENT) {
        return new HaxeBlockStatementImpl(node);
      }
      else if (type == HAXE_BREAK_STATEMENT) {
        return new HaxeBreakStatementImpl(node);
      }
      else if (type == HAXE_BUILD_MACRO) {
        return new HaxeBuildMacroImpl(node);
      }
      else if (type == HAXE_CALL_EXPRESSION) {
        return new HaxeCallExpressionImpl(node);
      }
      else if (type == HAXE_CAST_EXPRESSION) {
        return new HaxeCastExpressionImpl(node);
      }
      else if (type == HAXE_CATCH_STATEMENT) {
        return new HaxeCatchStatementImpl(node);
      }
      else if (type == HAXE_CLASS_BODY) {
        return new HaxeClassBodyImpl(node);
      }
      else if (type == HAXE_CLASS_DECLARATION) {
        return new HaxeClassDeclarationImpl(node);
      }
      else if (type == HAXE_COMPARE_EXPRESSION) {
        return new HaxeCompareExpressionImpl(node);
      }
      else if (type == HAXE_COMPARE_OPERATION) {
        return new HaxeCompareOperationImpl(node);
      }
      else if (type == HAXE_COMPONENT_NAME) {
        return new HaxeComponentNameImpl(node);
      }
      else if (type == HAXE_CONTINUE_STATEMENT) {
        return new HaxeContinueStatementImpl(node);
      }
      else if (type == HAXE_CUSTOM_META) {
        return new HaxeCustomMetaImpl(node);
      }
      else if (type == HAXE_DECLARATION_ATTRIBUTE) {
        return new HaxeDeclarationAttributeImpl(node);
      }
      else if (type == HAXE_DECLARATION_ATTRIBUTE_LIST) {
        return new HaxeDeclarationAttributeListImpl(node);
      }
      else if (type == HAXE_DEFAULT_CASE) {
        return new HaxeDefaultCaseImpl(node);
      }
      else if (type == HAXE_DO_WHILE_STATEMENT) {
        return new HaxeDoWhileStatementImpl(node);
      }
      else if (type == HAXE_ENUM_BODY) {
        return new HaxeEnumBodyImpl(node);
      }
      else if (type == HAXE_ENUM_CONSTRUCTOR_PARAMETERS) {
        return new HaxeEnumConstructorParametersImpl(node);
      }
      else if (type == HAXE_ENUM_DECLARATION) {
        return new HaxeEnumDeclarationImpl(node);
      }
      else if (type == HAXE_ENUM_VALUE_DECLARATION) {
        return new HaxeEnumValueDeclarationImpl(node);
      }
      else if (type == HAXE_EXPRESSION) {
        return new HaxeExpressionImpl(node);
      }
      else if (type == HAXE_EXPRESSION_LIST) {
        return new HaxeExpressionListImpl(node);
      }
      else if (type == HAXE_EXTERN_CLASS_DECLARATION) {
        return new HaxeExternClassDeclarationImpl(node);
      }
      else if (type == HAXE_EXTERN_CLASS_DECLARATION_BODY) {
        return new HaxeExternClassDeclarationBodyImpl(node);
      }
      else if (type == HAXE_EXTERN_FUNCTION_DECLARATION) {
        return new HaxeExternFunctionDeclarationImpl(node);
      }
      else if (type == HAXE_EXTERN_OR_PRIVATE) {
        return new HaxeExternOrPrivateImpl(node);
      }
      else if (type == HAXE_FAKE_ENUM_META) {
        return new HaxeFakeEnumMetaImpl(node);
      }
      else if (type == HAXE_FOR_STATEMENT) {
        return new HaxeForStatementImpl(node);
      }
      else if (type == HAXE_FUNCTION_DECLARATION_WITH_ATTRIBUTES) {
        return new HaxeFunctionDeclarationWithAttributesImpl(node);
      }
      else if (type == HAXE_FUNCTION_LITERAL) {
        return new HaxeFunctionLiteralImpl(node);
      }
      else if (type == HAXE_FUNCTION_PROTOTYPE_DECLARATION_WITH_ATTRIBUTES) {
        return new HaxeFunctionPrototypeDeclarationWithAttributesImpl(node);
      }
      else if (type == HAXE_FUNCTION_TYPE) {
        return new HaxeFunctionTypeImpl(node);
      }
      else if (type == HAXE_GENERIC_LIST_PART) {
        return new HaxeGenericListPartImpl(node);
      }
      else if (type == HAXE_GENERIC_PARAM) {
        return new HaxeGenericParamImpl(node);
      }
      else if (type == HAXE_GETTER_META) {
        return new HaxeGetterMetaImpl(node);
      }
      else if (type == HAXE_IDENTIFIER) {
        return new HaxeIdentifierImpl(node);
      }
      else if (type == HAXE_IF_STATEMENT) {
        return new HaxeIfStatementImpl(node);
      }
      else if (type == HAXE_IMPORT_STATEMENT) {
        return new HaxeImportStatementImpl(node);
      }
      else if (type == HAXE_INHERIT) {
        return new HaxeInheritImpl(node);
      }
      else if (type == HAXE_INHERIT_LIST) {
        return new HaxeInheritListImpl(node);
      }
      else if (type == HAXE_INTERFACE_BODY) {
        return new HaxeInterfaceBodyImpl(node);
      }
      else if (type == HAXE_INTERFACE_DECLARATION) {
        return new HaxeInterfaceDeclarationImpl(node);
      }
      else if (type == HAXE_ITERABLE) {
        return new HaxeIterableImpl(node);
      }
      else if (type == HAXE_ITERATOR_EXPRESSION) {
        return new HaxeIteratorExpressionImpl(node);
      }
      else if (type == HAXE_LITERAL_EXPRESSION) {
        return new HaxeLiteralExpressionImpl(node);
      }
      else if (type == HAXE_LOCAL_FUNCTION_DECLARATION) {
        return new HaxeLocalFunctionDeclarationImpl(node);
      }
      else if (type == HAXE_LOCAL_VAR_DECLARATION) {
        return new HaxeLocalVarDeclarationImpl(node);
      }
      else if (type == HAXE_LOCAL_VAR_DECLARATION_PART) {
        return new HaxeLocalVarDeclarationPartImpl(node);
      }
      else if (type == HAXE_LOGIC_AND_EXPRESSION) {
        return new HaxeLogicAndExpressionImpl(node);
      }
      else if (type == HAXE_LOGIC_OR_EXPRESSION) {
        return new HaxeLogicOrExpressionImpl(node);
      }
      else if (type == HAXE_LONG_TEMPLATE_ENTRY) {
        return new HaxeLongTemplateEntryImpl(node);
      }
      else if (type == HAXE_META_KEY_VALUE) {
        return new HaxeMetaKeyValueImpl(node);
      }
      else if (type == HAXE_META_META) {
        return new HaxeMetaMetaImpl(node);
      }
      else if (type == HAXE_MULTIPLICATIVE_EXPRESSION) {
        return new HaxeMultiplicativeExpressionImpl(node);
      }
      else if (type == HAXE_NATIVE_META) {
        return new HaxeNativeMetaImpl(node);
      }
      else if (type == HAXE_NEW_EXPRESSION) {
        return new HaxeNewExpressionImpl(node);
      }
      else if (type == HAXE_NS_META) {
        return new HaxeNsMetaImpl(node);
      }
      else if (type == HAXE_OBJECT_LITERAL) {
        return new HaxeObjectLiteralImpl(node);
      }
      else if (type == HAXE_OBJECT_LITERAL_ELEMENT) {
        return new HaxeObjectLiteralElementImpl(node);
      }
      else if (type == HAXE_OVERLOAD_META) {
        return new HaxeOverloadMetaImpl(node);
      }
      else if (type == HAXE_PACKAGE_STATEMENT) {
        return new HaxePackageStatementImpl(node);
      }
      else if (type == HAXE_PARAMETER) {
        return new HaxeParameterImpl(node);
      }
      else if (type == HAXE_PARAMETER_LIST) {
        return new HaxeParameterListImpl(node);
      }
      else if (type == HAXE_PARENTHESIZED_EXPRESSION) {
        return new HaxeParenthesizedExpressionImpl(node);
      }
      else if (type == HAXE_PREFIX_EXPRESSION) {
        return new HaxePrefixExpressionImpl(node);
      }
      else if (type == HAXE_PROPERTY_ACCESSOR) {
        return new HaxePropertyAccessorImpl(node);
      }
      else if (type == HAXE_PROPERTY_DECLARATION) {
        return new HaxePropertyDeclarationImpl(node);
      }
      else if (type == HAXE_REFERENCE_EXPRESSION) {
        return new HaxeReferenceExpressionImpl(node);
      }
      else if (type == HAXE_REGULAR_EXPRESSION_LITERAL) {
        return new HaxeRegularExpressionLiteralImpl(node);
      }
      else if (type == HAXE_REQUIRE_META) {
        return new HaxeRequireMetaImpl(node);
      }
      else if (type == HAXE_RETURN_STATEMENT) {
        return new HaxeReturnStatementImpl(node);
      }
      else if (type == HAXE_RETURN_STATEMENT_WITHOUT_SEMICOLON) {
        return new HaxeReturnStatementWithoutSemicolonImpl(node);
      }
      else if (type == HAXE_SETTER_META) {
        return new HaxeSetterMetaImpl(node);
      }
      else if (type == HAXE_SHIFT_EXPRESSION) {
        return new HaxeShiftExpressionImpl(node);
      }
      else if (type == HAXE_SHIFT_OPERATOR) {
        return new HaxeShiftOperatorImpl(node);
      }
      else if (type == HAXE_SHIFT_RIGHT_OPERATOR) {
        return new HaxeShiftRightOperatorImpl(node);
      }
      else if (type == HAXE_SHORT_TEMPLATE_ENTRY) {
        return new HaxeShortTemplateEntryImpl(node);
      }
      else if (type == HAXE_STRING_LITERAL_EXPRESSION) {
        return new HaxeStringLiteralExpressionImpl(node);
      }
      else if (type == HAXE_SUFFIX_EXPRESSION) {
        return new HaxeSuffixExpressionImpl(node);
      }
      else if (type == HAXE_SUPER_EXPRESSION) {
        return new HaxeSuperExpressionImpl(node);
      }
      else if (type == HAXE_SWITCH_BLOCK) {
        return new HaxeSwitchBlockImpl(node);
      }
      else if (type == HAXE_SWITCH_CASE) {
        return new HaxeSwitchCaseImpl(node);
      }
      else if (type == HAXE_SWITCH_CASE_BLOCK) {
        return new HaxeSwitchCaseBlockImpl(node);
      }
      else if (type == HAXE_SWITCH_CASE_EXPRESSION) {
        return new HaxeSwitchCaseExpressionImpl(node);
      }
      else if (type == HAXE_SWITCH_STATEMENT) {
        return new HaxeSwitchStatementImpl(node);
      }
      else if (type == HAXE_TERNARY_EXPRESSION) {
        return new HaxeTernaryExpressionImpl(node);
      }
      else if (type == HAXE_THIS_EXPRESSION) {
        return new HaxeThisExpressionImpl(node);
      }
      else if (type == HAXE_THROW_STATEMENT) {
        return new HaxeThrowStatementImpl(node);
      }
      else if (type == HAXE_TRY_STATEMENT) {
        return new HaxeTryStatementImpl(node);
      }
      else if (type == HAXE_TYPE) {
        return new HaxeTypeImpl(node);
      }
      else if (type == HAXE_TYPEDEF_DECLARATION) {
        return new HaxeTypedefDeclarationImpl(node);
      }
      else if (type == HAXE_TYPE_EXTENDS) {
        return new HaxeTypeExtendsImpl(node);
      }
      else if (type == HAXE_TYPE_LIST) {
        return new HaxeTypeListImpl(node);
      }
      else if (type == HAXE_TYPE_LIST_PART) {
        return new HaxeTypeListPartImpl(node);
      }
      else if (type == HAXE_TYPE_OR_ANONYMOUS) {
        return new HaxeTypeOrAnonymousImpl(node);
      }
      else if (type == HAXE_TYPE_PARAM) {
        return new HaxeTypeParamImpl(node);
      }
      else if (type == HAXE_TYPE_TAG) {
        return new HaxeTypeTagImpl(node);
      }
      else if (type == HAXE_UNSIGNED_SHIFT_RIGHT_OPERATOR) {
        return new HaxeUnsignedShiftRightOperatorImpl(node);
      }
      else if (type == HAXE_USING_STATEMENT) {
        return new HaxeUsingStatementImpl(node);
      }
      else if (type == HAXE_VAR_DECLARATION) {
        return new HaxeVarDeclarationImpl(node);
      }
      else if (type == HAXE_VAR_DECLARATION_PART) {
        return new HaxeVarDeclarationPartImpl(node);
      }
      else if (type == HAXE_VAR_INIT) {
        return new HaxeVarInitImpl(node);
      }
      else if (type == HAXE_WHILE_STATEMENT) {
        return new HaxeWhileStatementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
