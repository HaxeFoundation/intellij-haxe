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
  IElementType WSNLS = new HaxeElementType("WSNLS");

  IElementType MML_COMMENT = new HaxeElementType("MML_COMMENT");

  TokenSet WHITESPACES = TokenSet.create(
    WSNLS,
    TokenType.WHITE_SPACE,
    TokenType.NEW_LINE_INDENT
  );

  TokenSet COMMENTS = TokenSet.create(
    MML_COMMENT,
    MSL_COMMENT
  );

  TokenSet BLOCK_COMMENTS = TokenSet.create(
    MML_COMMENT
  );

  TokenSet NUMBERS = TokenSet.create(
    LITINT,
    LITHEX,
    LITOCT,
    LITFLOAT
  );

  TokenSet LINE_COMMENTS = TokenSet.create(
    MSL_COMMENT
  );

  TokenSet BAD_TOKENS = TokenSet.create(
    TokenType.BAD_CHARACTER
  );

  TokenSet STRINGS = TokenSet.create(
    LITSTRING
  );

  TokenSet IDENTIFIERS = TokenSet.create(
    ID
  );

  TokenSet BRACES = TokenSet.create(
    PLBRACK,
    PRBRACK,
    PLPAREN,
    PRPAREN,
    PLCURLY,
    PRCURLY
  );

  TokenSet LITERALS = TokenSet.create(
    LITCHAR,
    LITFLOAT,
    LITSTRING,
    LITINT,
    LITHEX,
    LITOCT
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
    KUNTYPE,
    KVAR,
    KWHILE,
    KTRY,
    KCATCH,
    KTYPEDEF,
    PPELSE,
    PPELSEIF,
    PPEND,
    PPERROR,
    PPIF
  );

  TokenSet UNARY_OPERATORS = TokenSet.create(
    OPLUS_PLUS, OMINUS_MINUS, ONOT
  );

  TokenSet ASSIGN_OPERATORS = TokenSet.create(
    OASSIGN,
    OPLUS_ASSIGN, OMINUS_ASSIGN, OBIT_OR_ASSIGN, OBIT_XOR_ASSIGN,
    OMUL_ASSIGN, OQUOTIENT_ASSIGN, OREMAINDER_ASSIGN,
    OSHIFT_LEFT_ASSIGN, OSHIFT_RIGHT_ASSIGN,
    OBIT_AND_ASSIGN
  );

  TokenSet BINARY_OPERATORS = TokenSet.create(
    OCOND_OR, OCOND_AND,
    OEQ, ONOT_EQ, OLESS, OLESS_OR_EQUAL, OGREATER, OGREATER_OR_EQUAL,
    OPLUS, OMINUS, OBIT_OR, OBIT_XOR,
    OMUL, OQUOTIENT, OREMAINDER, OSHIFT_LEFT, OSHIFT_RIGHT, OBIT_AND
  );

  TokenSet INC_DEC_OPERATORS = TokenSet.create(
    OPLUS_PLUS, OMINUS_MINUS
  );

  TokenSet OPERATORS = TokenSet.create(
    OSEMI,
    OTRIPLE_DOT,
    ODOT,
    OCOLON,
    OCOMMA,
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
    OSHIFT_RIGHT,
    OGREATER_OR_EQUAL,
    OGREATER,
    ONEW
  );
}

