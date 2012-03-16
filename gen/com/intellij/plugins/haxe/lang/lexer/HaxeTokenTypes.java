// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.*;

public interface HaxeTokenTypes {

  IElementType HAXE_ACCESS = new HaxeElementType("HAXE_ACCESS");
  IElementType HAXE_ADDITIVEEXPRESSION = new HaxeElementType("HAXE_ADDITIVEEXPRESSION");
  IElementType HAXE_ANONYMOUSTYPE = new HaxeElementType("HAXE_ANONYMOUSTYPE");
  IElementType HAXE_ANONYMOUSTYPEBODY = new HaxeElementType("HAXE_ANONYMOUSTYPEBODY");
  IElementType HAXE_ANONYMOUSTYPEFIELD = new HaxeElementType("HAXE_ANONYMOUSTYPEFIELD");
  IElementType HAXE_ANONYMOUSTYPEFIELDLIST = new HaxeElementType("HAXE_ANONYMOUSTYPEFIELDLIST");
  IElementType HAXE_ARRAYACCESSEXPRESSION = new HaxeElementType("HAXE_ARRAYACCESSEXPRESSION");
  IElementType HAXE_ARRAYLITERAL = new HaxeElementType("HAXE_ARRAYLITERAL");
  IElementType HAXE_ASSIGNEXPRESSION = new HaxeElementType("HAXE_ASSIGNEXPRESSION");
  IElementType HAXE_ASSIGNOPERATION = new HaxeElementType("HAXE_ASSIGNOPERATION");
  IElementType HAXE_BITOPERATION = new HaxeElementType("HAXE_BITOPERATION");
  IElementType HAXE_BITWISEEXPRESSION = new HaxeElementType("HAXE_BITWISEEXPRESSION");
  IElementType HAXE_BLOCKSTATEMENT = new HaxeElementType("HAXE_BLOCKSTATEMENT");
  IElementType HAXE_BREAKSTATEMENT = new HaxeElementType("HAXE_BREAKSTATEMENT");
  IElementType HAXE_CALLEXPRESSION = new HaxeElementType("HAXE_CALLEXPRESSION");
  IElementType HAXE_CASESTATEMENT = new HaxeElementType("HAXE_CASESTATEMENT");
  IElementType HAXE_CASTEXPRESSION = new HaxeElementType("HAXE_CASTEXPRESSION");
  IElementType HAXE_CATCHSTATEMENT = new HaxeElementType("HAXE_CATCHSTATEMENT");
  IElementType HAXE_CLASSBODY = new HaxeElementType("HAXE_CLASSBODY");
  IElementType HAXE_CLASSDECLARATION = new HaxeElementType("HAXE_CLASSDECLARATION");
  IElementType HAXE_COMPAREEXPRESSION = new HaxeElementType("HAXE_COMPAREEXPRESSION");
  IElementType HAXE_COMPAREOPERATION = new HaxeElementType("HAXE_COMPAREOPERATION");
  IElementType HAXE_COMPONENTNAME = new HaxeElementType("HAXE_COMPONENTNAME");
  IElementType HAXE_CONTINUESTATEMENT = new HaxeElementType("HAXE_CONTINUESTATEMENT");
  IElementType HAXE_DECLARATIONATTRIBUTE = new HaxeElementType("HAXE_DECLARATIONATTRIBUTE");
  IElementType HAXE_DECLARATIONATTRIBUTELIST = new HaxeElementType("HAXE_DECLARATIONATTRIBUTELIST");
  IElementType HAXE_DEFAULTSTATEMENT = new HaxeElementType("HAXE_DEFAULTSTATEMENT");
  IElementType HAXE_DOWHILESTATEMENT = new HaxeElementType("HAXE_DOWHILESTATEMENT");
  IElementType HAXE_ENUMBODY = new HaxeElementType("HAXE_ENUMBODY");
  IElementType HAXE_ENUMCONSTRUCTORPARAMETERS = new HaxeElementType("HAXE_ENUMCONSTRUCTORPARAMETERS");
  IElementType HAXE_ENUMDECLARATION = new HaxeElementType("HAXE_ENUMDECLARATION");
  IElementType HAXE_ENUMVALUEDECLARATION = new HaxeElementType("HAXE_ENUMVALUEDECLARATION");
  IElementType HAXE_EXPRESSION = new HaxeElementType("HAXE_EXPRESSION");
  IElementType HAXE_EXPRESSIONLIST = new HaxeElementType("HAXE_EXPRESSIONLIST");
  IElementType HAXE_EXTERNCLASSDECLARATION = new HaxeElementType("HAXE_EXTERNCLASSDECLARATION");
  IElementType HAXE_EXTERNCLASSDECLARATIONBODY = new HaxeElementType("HAXE_EXTERNCLASSDECLARATIONBODY");
  IElementType HAXE_EXTERNFUNCTIONDECLARATION = new HaxeElementType("HAXE_EXTERNFUNCTIONDECLARATION");
  IElementType HAXE_EXTERNORPRIVATE = new HaxeElementType("HAXE_EXTERNORPRIVATE");
  IElementType HAXE_FAKEENUMMETA = new HaxeElementType("HAXE_FAKEENUMMETA");
  IElementType HAXE_FORSTATEMENT = new HaxeElementType("HAXE_FORSTATEMENT");
  IElementType HAXE_FUNCTIONDECLARATIONWITHATTRIBUTES = new HaxeElementType("HAXE_FUNCTIONDECLARATIONWITHATTRIBUTES");
  IElementType HAXE_FUNCTIONLITERAL = new HaxeElementType("HAXE_FUNCTIONLITERAL");
  IElementType HAXE_FUNCTIONPROTOTYPEDECLARATIONWITHATTRIBUTES = new HaxeElementType("HAXE_FUNCTIONPROTOTYPEDECLARATIONWITHATTRIBUTES");
  IElementType HAXE_FUNCTIONTYPE = new HaxeElementType("HAXE_FUNCTIONTYPE");
  IElementType HAXE_IDENTIFIER = new HaxeElementType("HAXE_IDENTIFIER");
  IElementType HAXE_IFEXPRESSION = new HaxeElementType("HAXE_IFEXPRESSION");
  IElementType HAXE_IFSTATEMENT = new HaxeElementType("HAXE_IFSTATEMENT");
  IElementType HAXE_IMPORTSTATEMENT = new HaxeElementType("HAXE_IMPORTSTATEMENT");
  IElementType HAXE_INHERIT = new HaxeElementType("HAXE_INHERIT");
  IElementType HAXE_INHERITLIST = new HaxeElementType("HAXE_INHERITLIST");
  IElementType HAXE_INTERFACEBODY = new HaxeElementType("HAXE_INTERFACEBODY");
  IElementType HAXE_INTERFACEDECLARATION = new HaxeElementType("HAXE_INTERFACEDECLARATION");
  IElementType HAXE_ITERATOREXPRESSION = new HaxeElementType("HAXE_ITERATOREXPRESSION");
  IElementType HAXE_LITERALEXPRESSION = new HaxeElementType("HAXE_LITERALEXPRESSION");
  IElementType HAXE_LOCALFUNCTIONDECLARATION = new HaxeElementType("HAXE_LOCALFUNCTIONDECLARATION");
  IElementType HAXE_LOCALVARDECLARATION = new HaxeElementType("HAXE_LOCALVARDECLARATION");
  IElementType HAXE_LOCALVARDECLARATIONPART = new HaxeElementType("HAXE_LOCALVARDECLARATIONPART");
  IElementType HAXE_LOGICANDEXPRESSION = new HaxeElementType("HAXE_LOGICANDEXPRESSION");
  IElementType HAXE_LOGICOREXPRESSION = new HaxeElementType("HAXE_LOGICOREXPRESSION");
  IElementType HAXE_MULTIPLICATIVEEXPRESSION = new HaxeElementType("HAXE_MULTIPLICATIVEEXPRESSION");
  IElementType HAXE_NEWEXPRESSION = new HaxeElementType("HAXE_NEWEXPRESSION");
  IElementType HAXE_OBJECTLITERAL = new HaxeElementType("HAXE_OBJECTLITERAL");
  IElementType HAXE_OBJECTLITERALELEMENT = new HaxeElementType("HAXE_OBJECTLITERALELEMENT");
  IElementType HAXE_PACKAGESTATEMENT = new HaxeElementType("HAXE_PACKAGESTATEMENT");
  IElementType HAXE_PARAMETER = new HaxeElementType("HAXE_PARAMETER");
  IElementType HAXE_PARAMETERLIST = new HaxeElementType("HAXE_PARAMETERLIST");
  IElementType HAXE_PARENTHESIZEDEXPRESSION = new HaxeElementType("HAXE_PARENTHESIZEDEXPRESSION");
  IElementType HAXE_PP = new HaxeElementType("HAXE_PP");
  IElementType HAXE_PPELSE = new HaxeElementType("HAXE_PPELSE");
  IElementType HAXE_PPELSEIF = new HaxeElementType("HAXE_PPELSEIF");
  IElementType HAXE_PPEND = new HaxeElementType("HAXE_PPEND");
  IElementType HAXE_PPERROR = new HaxeElementType("HAXE_PPERROR");
  IElementType HAXE_PPIF = new HaxeElementType("HAXE_PPIF");
  IElementType HAXE_PREFIXEXPRESSION = new HaxeElementType("HAXE_PREFIXEXPRESSION");
  IElementType HAXE_PROPERTYACCESSOR = new HaxeElementType("HAXE_PROPERTYACCESSOR");
  IElementType HAXE_PROPERTYDECLARATION = new HaxeElementType("HAXE_PROPERTYDECLARATION");
  IElementType HAXE_REFERENCEEXPRESSION = new HaxeElementType("HAXE_REFERENCEEXPRESSION");
  IElementType HAXE_REQUIREMETA = new HaxeElementType("HAXE_REQUIREMETA");
  IElementType HAXE_RETURNSTATEMENT = new HaxeElementType("HAXE_RETURNSTATEMENT");
  IElementType HAXE_RETURNSTATEMENTWITHOUTSEMICOLON = new HaxeElementType("HAXE_RETURNSTATEMENTWITHOUTSEMICOLON");
  IElementType HAXE_SHIFTEXPRESSION = new HaxeElementType("HAXE_SHIFTEXPRESSION");
  IElementType HAXE_SHIFTOPERATOR = new HaxeElementType("HAXE_SHIFTOPERATOR");
  IElementType HAXE_SHIFTRIGHTOPERATOR = new HaxeElementType("HAXE_SHIFTRIGHTOPERATOR");
  IElementType HAXE_SUFFIXEXPRESSION = new HaxeElementType("HAXE_SUFFIXEXPRESSION");
  IElementType HAXE_SWITCHSTATEMENT = new HaxeElementType("HAXE_SWITCHSTATEMENT");
  IElementType HAXE_TERNARYEXPRESSION = new HaxeElementType("HAXE_TERNARYEXPRESSION");
  IElementType HAXE_THISEXPRESSION = new HaxeElementType("HAXE_THISEXPRESSION");
  IElementType HAXE_THROWSTATEMENT = new HaxeElementType("HAXE_THROWSTATEMENT");
  IElementType HAXE_TRYSTATEMENT = new HaxeElementType("HAXE_TRYSTATEMENT");
  IElementType HAXE_TYPE = new HaxeElementType("HAXE_TYPE");
  IElementType HAXE_TYPECONSTRAINT = new HaxeElementType("HAXE_TYPECONSTRAINT");
  IElementType HAXE_TYPEEXTENDS = new HaxeElementType("HAXE_TYPEEXTENDS");
  IElementType HAXE_TYPELIST = new HaxeElementType("HAXE_TYPELIST");
  IElementType HAXE_TYPEPARAM = new HaxeElementType("HAXE_TYPEPARAM");
  IElementType HAXE_TYPETAG = new HaxeElementType("HAXE_TYPETAG");
  IElementType HAXE_TYPEDEFDECLARATION = new HaxeElementType("HAXE_TYPEDEFDECLARATION");
  IElementType HAXE_UNSIGNEDSHIFTRIGHTOPERATOR = new HaxeElementType("HAXE_UNSIGNEDSHIFTRIGHTOPERATOR");
  IElementType HAXE_VARDECLARATION = new HaxeElementType("HAXE_VARDECLARATION");
  IElementType HAXE_VARDECLARATIONPART = new HaxeElementType("HAXE_VARDECLARATIONPART");
  IElementType HAXE_VARINIT = new HaxeElementType("HAXE_VARINIT");
  IElementType HAXE_WHILESTATEMENT = new HaxeElementType("HAXE_WHILESTATEMENT");

  IElementType ID = new HaxeElementType("ID");
  IElementType KBREAK = new HaxeElementType("break");
  IElementType KCASE = new HaxeElementType("case");
  IElementType KCAST = new HaxeElementType("cast");
  IElementType KCATCH = new HaxeElementType("catch");
  IElementType KCLASS = new HaxeElementType("class");
  IElementType KCONTINUE = new HaxeElementType("continue");
  IElementType KDEFAULT = new HaxeElementType("default");
  IElementType KDO = new HaxeElementType("do");
  IElementType KDYNAMIC = new HaxeElementType("dynamic");
  IElementType KELSE = new HaxeElementType("else");
  IElementType KENUM = new HaxeElementType("enum");
  IElementType KEXTENDS = new HaxeElementType("extends");
  IElementType KEXTERN = new HaxeElementType("extern");
  IElementType KFAKEENUM = new HaxeElementType("@:fakeEnum");
  IElementType KFINAL = new HaxeElementType("@:final");
  IElementType KFOR = new HaxeElementType("for");
  IElementType KFUNCTION = new HaxeElementType("function");
  IElementType KIF = new HaxeElementType("if");
  IElementType KIMPLEMENTS = new HaxeElementType("implements");
  IElementType KIMPORT = new HaxeElementType("import");
  IElementType KINLINE = new HaxeElementType("inline");
  IElementType KINTERFACE = new HaxeElementType("interface");
  IElementType KMACRO = new HaxeElementType("@:macro");
  IElementType KNULL = new HaxeElementType("null");
  IElementType KOVERRIDE = new HaxeElementType("override");
  IElementType KPACKAGE = new HaxeElementType("package");
  IElementType KPRIVATE = new HaxeElementType("private");
  IElementType KPUBLIC = new HaxeElementType("public");
  IElementType KREQUIRE = new HaxeElementType("@:require");
  IElementType KRETURN = new HaxeElementType("return");
  IElementType KSTATIC = new HaxeElementType("static");
  IElementType KSWITCH = new HaxeElementType("switch");
  IElementType KTHIS = new HaxeElementType("this");
  IElementType KTHROW = new HaxeElementType("throw");
  IElementType KTRY = new HaxeElementType("try");
  IElementType KTYPEDEF = new HaxeElementType("typedef");
  IElementType KUNTYPE = new HaxeElementType("untyped");
  IElementType KVAR = new HaxeElementType("var");
  IElementType KWHILE = new HaxeElementType("while");
  IElementType LITCHAR = new HaxeElementType("LITCHAR");
  IElementType LITFLOAT = new HaxeElementType("LITFLOAT");
  IElementType LITHEX = new HaxeElementType("LITHEX");
  IElementType LITINT = new HaxeElementType("LITINT");
  IElementType LITOCT = new HaxeElementType("LITOCT");
  IElementType LITSTRING = new HaxeElementType("LITSTRING");
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

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == HAXE_ACCESS) {
        return new HaxeAccessImpl(node);
      }
      else if (type == HAXE_ADDITIVEEXPRESSION) {
        return new HaxeAdditiveExpressionImpl(node);
      }
      else if (type == HAXE_ANONYMOUSTYPE) {
        return new HaxeAnonymousTypeImpl(node);
      }
      else if (type == HAXE_ANONYMOUSTYPEBODY) {
        return new HaxeAnonymousTypeBodyImpl(node);
      }
      else if (type == HAXE_ANONYMOUSTYPEFIELD) {
        return new HaxeAnonymousTypeFieldImpl(node);
      }
      else if (type == HAXE_ANONYMOUSTYPEFIELDLIST) {
        return new HaxeAnonymousTypeFieldListImpl(node);
      }
      else if (type == HAXE_ARRAYACCESSEXPRESSION) {
        return new HaxeArrayAccessExpressionImpl(node);
      }
      else if (type == HAXE_ARRAYLITERAL) {
        return new HaxeArrayLiteralImpl(node);
      }
      else if (type == HAXE_ASSIGNEXPRESSION) {
        return new HaxeAssignExpressionImpl(node);
      }
      else if (type == HAXE_ASSIGNOPERATION) {
        return new HaxeAssignOperationImpl(node);
      }
      else if (type == HAXE_BITOPERATION) {
        return new HaxeBitOperationImpl(node);
      }
      else if (type == HAXE_BITWISEEXPRESSION) {
        return new HaxeBitwiseExpressionImpl(node);
      }
      else if (type == HAXE_BLOCKSTATEMENT) {
        return new HaxeBlockStatementImpl(node);
      }
      else if (type == HAXE_BREAKSTATEMENT) {
        return new HaxeBreakStatementImpl(node);
      }
      else if (type == HAXE_CALLEXPRESSION) {
        return new HaxeCallExpressionImpl(node);
      }
      else if (type == HAXE_CASESTATEMENT) {
        return new HaxeCaseStatementImpl(node);
      }
      else if (type == HAXE_CASTEXPRESSION) {
        return new HaxeCastExpressionImpl(node);
      }
      else if (type == HAXE_CATCHSTATEMENT) {
        return new HaxeCatchStatementImpl(node);
      }
      else if (type == HAXE_CLASSBODY) {
        return new HaxeClassBodyImpl(node);
      }
      else if (type == HAXE_CLASSDECLARATION) {
        return new HaxeClassDeclarationImpl(node);
      }
      else if (type == HAXE_COMPAREEXPRESSION) {
        return new HaxeCompareExpressionImpl(node);
      }
      else if (type == HAXE_COMPAREOPERATION) {
        return new HaxeCompareOperationImpl(node);
      }
      else if (type == HAXE_COMPONENTNAME) {
        return new HaxeComponentNameImpl(node);
      }
      else if (type == HAXE_CONTINUESTATEMENT) {
        return new HaxeContinueStatementImpl(node);
      }
      else if (type == HAXE_DECLARATIONATTRIBUTE) {
        return new HaxeDeclarationAttributeImpl(node);
      }
      else if (type == HAXE_DECLARATIONATTRIBUTELIST) {
        return new HaxeDeclarationAttributeListImpl(node);
      }
      else if (type == HAXE_DEFAULTSTATEMENT) {
        return new HaxeDefaultStatementImpl(node);
      }
      else if (type == HAXE_DOWHILESTATEMENT) {
        return new HaxeDoWhileStatementImpl(node);
      }
      else if (type == HAXE_ENUMBODY) {
        return new HaxeEnumBodyImpl(node);
      }
      else if (type == HAXE_ENUMCONSTRUCTORPARAMETERS) {
        return new HaxeEnumConstructorParametersImpl(node);
      }
      else if (type == HAXE_ENUMDECLARATION) {
        return new HaxeEnumDeclarationImpl(node);
      }
      else if (type == HAXE_ENUMVALUEDECLARATION) {
        return new HaxeEnumValueDeclarationImpl(node);
      }
      else if (type == HAXE_EXPRESSION) {
        return new HaxeExpressionImpl(node);
      }
      else if (type == HAXE_EXPRESSIONLIST) {
        return new HaxeExpressionListImpl(node);
      }
      else if (type == HAXE_EXTERNCLASSDECLARATION) {
        return new HaxeExternClassDeclarationImpl(node);
      }
      else if (type == HAXE_EXTERNCLASSDECLARATIONBODY) {
        return new HaxeExternClassDeclarationBodyImpl(node);
      }
      else if (type == HAXE_EXTERNFUNCTIONDECLARATION) {
        return new HaxeExternFunctionDeclarationImpl(node);
      }
      else if (type == HAXE_EXTERNORPRIVATE) {
        return new HaxeExternOrPrivateImpl(node);
      }
      else if (type == HAXE_FAKEENUMMETA) {
        return new HaxeFakeEnumMetaImpl(node);
      }
      else if (type == HAXE_FORSTATEMENT) {
        return new HaxeForStatementImpl(node);
      }
      else if (type == HAXE_FUNCTIONDECLARATIONWITHATTRIBUTES) {
        return new HaxeFunctionDeclarationWithAttributesImpl(node);
      }
      else if (type == HAXE_FUNCTIONLITERAL) {
        return new HaxeFunctionLiteralImpl(node);
      }
      else if (type == HAXE_FUNCTIONPROTOTYPEDECLARATIONWITHATTRIBUTES) {
        return new HaxeFunctionPrototypeDeclarationWithAttributesImpl(node);
      }
      else if (type == HAXE_FUNCTIONTYPE) {
        return new HaxeFunctionTypeImpl(node);
      }
      else if (type == HAXE_IDENTIFIER) {
        return new HaxeIdentifierImpl(node);
      }
      else if (type == HAXE_IFEXPRESSION) {
        return new HaxeIfExpressionImpl(node);
      }
      else if (type == HAXE_IFSTATEMENT) {
        return new HaxeIfStatementImpl(node);
      }
      else if (type == HAXE_IMPORTSTATEMENT) {
        return new HaxeImportStatementImpl(node);
      }
      else if (type == HAXE_INHERIT) {
        return new HaxeInheritImpl(node);
      }
      else if (type == HAXE_INHERITLIST) {
        return new HaxeInheritListImpl(node);
      }
      else if (type == HAXE_INTERFACEBODY) {
        return new HaxeInterfaceBodyImpl(node);
      }
      else if (type == HAXE_INTERFACEDECLARATION) {
        return new HaxeInterfaceDeclarationImpl(node);
      }
      else if (type == HAXE_ITERATOREXPRESSION) {
        return new HaxeIteratorExpressionImpl(node);
      }
      else if (type == HAXE_LITERALEXPRESSION) {
        return new HaxeLiteralExpressionImpl(node);
      }
      else if (type == HAXE_LOCALFUNCTIONDECLARATION) {
        return new HaxeLocalFunctionDeclarationImpl(node);
      }
      else if (type == HAXE_LOCALVARDECLARATION) {
        return new HaxeLocalVarDeclarationImpl(node);
      }
      else if (type == HAXE_LOCALVARDECLARATIONPART) {
        return new HaxeLocalVarDeclarationPartImpl(node);
      }
      else if (type == HAXE_LOGICANDEXPRESSION) {
        return new HaxeLogicAndExpressionImpl(node);
      }
      else if (type == HAXE_LOGICOREXPRESSION) {
        return new HaxeLogicOrExpressionImpl(node);
      }
      else if (type == HAXE_MULTIPLICATIVEEXPRESSION) {
        return new HaxeMultiplicativeExpressionImpl(node);
      }
      else if (type == HAXE_NEWEXPRESSION) {
        return new HaxeNewExpressionImpl(node);
      }
      else if (type == HAXE_OBJECTLITERAL) {
        return new HaxeObjectLiteralImpl(node);
      }
      else if (type == HAXE_OBJECTLITERALELEMENT) {
        return new HaxeObjectLiteralElementImpl(node);
      }
      else if (type == HAXE_PACKAGESTATEMENT) {
        return new HaxePackageStatementImpl(node);
      }
      else if (type == HAXE_PARAMETER) {
        return new HaxeParameterImpl(node);
      }
      else if (type == HAXE_PARAMETERLIST) {
        return new HaxeParameterListImpl(node);
      }
      else if (type == HAXE_PARENTHESIZEDEXPRESSION) {
        return new HaxeParenthesizedExpressionImpl(node);
      }
      else if (type == HAXE_PP) {
        return new HaxePpImpl(node);
      }
      else if (type == HAXE_PPELSE) {
        return new HaxePpElseImpl(node);
      }
      else if (type == HAXE_PPELSEIF) {
        return new HaxePpElseIfImpl(node);
      }
      else if (type == HAXE_PPEND) {
        return new HaxePpEndImpl(node);
      }
      else if (type == HAXE_PPERROR) {
        return new HaxePpErrorImpl(node);
      }
      else if (type == HAXE_PPIF) {
        return new HaxePpIfImpl(node);
      }
      else if (type == HAXE_PREFIXEXPRESSION) {
        return new HaxePrefixExpressionImpl(node);
      }
      else if (type == HAXE_PROPERTYACCESSOR) {
        return new HaxePropertyAccessorImpl(node);
      }
      else if (type == HAXE_PROPERTYDECLARATION) {
        return new HaxePropertyDeclarationImpl(node);
      }
      else if (type == HAXE_REFERENCEEXPRESSION) {
        return new HaxeReferenceExpressionImpl(node);
      }
      else if (type == HAXE_REQUIREMETA) {
        return new HaxeRequireMetaImpl(node);
      }
      else if (type == HAXE_RETURNSTATEMENT) {
        return new HaxeReturnStatementImpl(node);
      }
      else if (type == HAXE_RETURNSTATEMENTWITHOUTSEMICOLON) {
        return new HaxeReturnStatementWithoutSemicolonImpl(node);
      }
      else if (type == HAXE_SHIFTEXPRESSION) {
        return new HaxeShiftExpressionImpl(node);
      }
      else if (type == HAXE_SHIFTOPERATOR) {
        return new HaxeShiftOperatorImpl(node);
      }
      else if (type == HAXE_SHIFTRIGHTOPERATOR) {
        return new HaxeShiftRightOperatorImpl(node);
      }
      else if (type == HAXE_SUFFIXEXPRESSION) {
        return new HaxeSuffixExpressionImpl(node);
      }
      else if (type == HAXE_SWITCHSTATEMENT) {
        return new HaxeSwitchStatementImpl(node);
      }
      else if (type == HAXE_TERNARYEXPRESSION) {
        return new HaxeTernaryExpressionImpl(node);
      }
      else if (type == HAXE_THISEXPRESSION) {
        return new HaxeThisExpressionImpl(node);
      }
      else if (type == HAXE_THROWSTATEMENT) {
        return new HaxeThrowStatementImpl(node);
      }
      else if (type == HAXE_TRYSTATEMENT) {
        return new HaxeTryStatementImpl(node);
      }
      else if (type == HAXE_TYPE) {
        return new HaxeTypeImpl(node);
      }
      else if (type == HAXE_TYPECONSTRAINT) {
        return new HaxeTypeConstraintImpl(node);
      }
      else if (type == HAXE_TYPEEXTENDS) {
        return new HaxeTypeExtendsImpl(node);
      }
      else if (type == HAXE_TYPELIST) {
        return new HaxeTypeListImpl(node);
      }
      else if (type == HAXE_TYPEPARAM) {
        return new HaxeTypeParamImpl(node);
      }
      else if (type == HAXE_TYPETAG) {
        return new HaxeTypeTagImpl(node);
      }
      else if (type == HAXE_TYPEDEFDECLARATION) {
        return new HaxeTypedefDeclarationImpl(node);
      }
      else if (type == HAXE_UNSIGNEDSHIFTRIGHTOPERATOR) {
        return new HaxeUnsignedShiftRightOperatorImpl(node);
      }
      else if (type == HAXE_VARDECLARATION) {
        return new HaxeVarDeclarationImpl(node);
      }
      else if (type == HAXE_VARDECLARATIONPART) {
        return new HaxeVarDeclarationPartImpl(node);
      }
      else if (type == HAXE_VARINIT) {
        return new HaxeVarInitImpl(node);
      }
      else if (type == HAXE_WHILESTATEMENT) {
        return new HaxeWhileStatementImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
