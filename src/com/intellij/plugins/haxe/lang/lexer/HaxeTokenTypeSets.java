package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public interface HaxeTokenTypeSets {
  IFileElementType HAXE_FILE = new IFileElementType("HAXEFILE", HaxeLanguage.INSTANCE);
  IElementType MSL_COMMENT = new HaxeElementType("MSL_COMMENT");
  IElementType MML_COMMENT = new HaxeElementType("MML_COMMENT");
  IElementType DOC_COMMENT = new HaxeElementType("DOC_COMMENT");

  IElementType WSNLS = new HaxeElementType("WSNLS");

  TokenSet WHITESPACES = TokenSet.create(
    WSNLS,
    TokenType.WHITE_SPACE,
    TokenType.NEW_LINE_INDENT
  );

  TokenSet COMMENTS = TokenSet.create(
    MML_COMMENT,
    MSL_COMMENT,
    DOC_COMMENT
  );

  TokenSet BAD_TOKENS = TokenSet.create(
    TokenType.BAD_CHARACTER
  );

  TokenSet STRINGS = TokenSet.create(
    LITSTRING
  );

  TokenSet KEYWORDS = TokenSet.create(
    KBREAK,
    KCASE,
    KCAST,
    KCLASS,
    KCONTINUE,
    KDEFAULT,
    KDO,
    KDYNAMIC,
    KELSE,
    KENUM,
    KEXTENDS,
    KFOR,
    KFUNCTION,
    KIF,
    KIMPLEMENTS,
    KIMPORT,
    KINLINE,
    KINTERFACE,
    KNULL,
    KOVERRIDE,
    KPACKAGE,
    KPRIVATE,
    KPUBLIC,
    KRETURN,
    KSTATIC,
    KSWITCH,
    KTHIS,
    KTHROW,
    KUNTYPED,
    KVAR,
    KWHILE,
    KTRY,
    KCATCH,
    KTYPEDEF,
    PPELSE,
    PPELSEIF,
    PPEND,
    PPERROR,
    PPIF,
    KEXTERN
  );

  TokenSet FUNCTION_DEFINITION = TokenSet.create(
    HAXE_FUNCTIONDECLARATIONWITHATTRIBUTES,
    HAXE_LOCALFUNCTIONDECLARATION,
    HAXE_FUNCTIONLITERAL
  );

  TokenSet BINARY_EXPRESSIONS = TokenSet.create(
    HAXE_LOGICOREXPRESSION,
    HAXE_LOGICANDEXPRESSION,
    HAXE_COMPAREEXPRESSION,
    HAXE_SHIFTEXPRESSION,
    HAXE_ADDITIVEEXPRESSION,
    HAXE_MULTIPLICATIVEEXPRESSION
  );

  TokenSet BINARY_OPERATORS = TokenSet.create(
    HAXE_BITOPERATION,
    OCOND_OR, OCOND_AND,
    HAXE_COMPAREOPERATION,
    HAXE_SHIFTOPERATOR,
    OPLUS, OMINUS,
    OMUL, OQUOTIENT, OREMAINDER
  );

  TokenSet ASSIGN_OPERATORS = TokenSet.create(
    OASSIGN,
    OPLUS_ASSIGN, OMINUS_ASSIGN, OBIT_OR_ASSIGN, OBIT_XOR_ASSIGN,
    OMUL_ASSIGN, OQUOTIENT_ASSIGN, OREMAINDER_ASSIGN,
    OSHIFT_LEFT_ASSIGN, OSHIFT_RIGHT_ASSIGN,
    OBIT_AND_ASSIGN,
    HAXE_ASSIGNOPERATION
  );

  TokenSet LOGIC_OPERATORS = TokenSet.create(
    OCOND_OR, OCOND_AND
  );

  TokenSet EQUALITY_OPERATORS = TokenSet.create(
    OEQ, ONOT_EQ
  );

  TokenSet RELATIONAL_OPERATORS = TokenSet.create(
    OLESS, OLESS_OR_EQUAL, OGREATER, OGREATER_OR_EQUAL
  );

  TokenSet ADDITIVE_OPERATORS = TokenSet.create(
    OPLUS, OMINUS
  );

  TokenSet MULTIPLICATIVE_OPERATORS = TokenSet.create(
    OMUL, OQUOTIENT, OREMAINDER
  );

  TokenSet UNARY_OPERATORS = TokenSet.create(
    OPLUS_PLUS, OMINUS_MINUS, ONOT, OMINUS
  );

  TokenSet BITWISE_OPERATORS = TokenSet.create(
    OBIT_AND, OBIT_OR, OBIT_XOR,
    HAXE_BITOPERATION
  );

  TokenSet SHIFT_OPERATORS = TokenSet.create(
    OSHIFT_LEFT,
    HAXE_SHIFTRIGHTOPERATOR, HAXE_UNSIGNEDSHIFTRIGHTOPERATOR, HAXE_SHIFTOPERATOR
  );

  TokenSet OPERATORS = TokenSet.create(
    OTRIPLE_DOT,
    OEQ,
    OASSIGN,
    ONOT_EQ,
    ONOT,
    OPLUS_PLUS,
    OPLUS_ASSIGN,
    OPLUS,
    OMINUS_MINUS,
    OMINUS_ASSIGN,
    OMINUS,
    OCOND_OR,
    OBIT_OR_ASSIGN,
    OBIT_OR,
    OCOND_AND,
    OBIT_AND_ASSIGN,
    OBIT_AND,
    OSHIFT_LEFT_ASSIGN,
    OSHIFT_LEFT,
    OLESS_OR_EQUAL,
    OLESS,
    OBIT_XOR_ASSIGN,
    OBIT_XOR,
    OMUL_ASSIGN,
    OMUL,
    OQUOTIENT_ASSIGN,
    OQUOTIENT,
    OREMAINDER_ASSIGN,
    OREMAINDER,
    OSHIFT_RIGHT_ASSIGN,
    OGREATER_OR_EQUAL,
    OGREATER
  );
}

