package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public interface HaxeTokenTypeSets {
  IFileElementType HAXE_FILE = new IFileElementType("HAXEFILE", HaxeFileType.HAXE_LANGUAGE);
  IElementType MSL_COMMENT = new HaxeElementType("MSL_COMMENT");
  IElementType WSNLS = new HaxeElementType("WSNLS");

  IElementType MML_COMMENT = new HaxeElementType("MML_COMMENT");

  static final TokenSet WHITESPACES = TokenSet.create(
    WSNLS,
    TokenType.WHITE_SPACE,
    TokenType.NEW_LINE_INDENT
  );

  static final TokenSet COMMENTS = TokenSet.create(
    MML_COMMENT,
    MSL_COMMENT
  );

  static final TokenSet BLOCK_COMMENTS = TokenSet.create(
    MML_COMMENT
  );

  static final TokenSet NUMBERS = TokenSet.create(
    LITINT,
    LITHEX,
    LITOCT,
    LITFLOAT
  );

  static final TokenSet LINE_COMMENTS = TokenSet.create(
    MSL_COMMENT
  );

  static final TokenSet BAD_TOKENS = TokenSet.create(
    TokenType.BAD_CHARACTER
  );

  static final TokenSet STRINGS = TokenSet.create(
    LITSTRING
  );

  static final TokenSet IDENTIFIERS = TokenSet.create(
    ID
  );

  static final TokenSet BRACES = TokenSet.create(
    PLBRACK,
    PRBRACK,
    PLPAREN,
    PRPAREN,
    PLCURLY,
    PRCURLY
  );

  static final TokenSet LITERALS = TokenSet.create(
    LITCHAR,
    LITFLOAT,
    LITSTRING,
    LITINT,
    LITHEX,
    LITOCT
  );

  public static final TokenSet KEYWORDS = TokenSet.create(
    KPACKAGE,
    KIMPORT,
    KBREAK,
    KCASE,
    KCONTINUE,
    KDEFAULT,
    KELSE,
    KFOR,
    KDO,
    KWHILE,
    KFUNCTION,
    KIF,
    KIMPORT,
    KINTERFACE,
    KPACKAGE,
    KRETURN,
    KCLASS,
    KSWITCH,
    KVAR,
    KENUM,
    KINTERFACE,
    KTHROW,
    KIMPLEMENTS,
    KEXTENDS
  );

  public static final TokenSet UNARY_OPERATORS = TokenSet.create(
    OPLUS_PLUS, OMINUS_MINUS, ONOT
  );

  public static final TokenSet ASSIGN_OPERATORS = TokenSet.create(
    OASSIGN,
    OPLUS_ASSIGN, OMINUS_ASSIGN, OBIT_OR_ASSIGN, OBIT_XOR_ASSIGN,
    OMUL_ASSIGN, OQUOTIENT_ASSIGN, OREMAINDER_ASSIGN,
    OSHIFT_LEFT_ASSIGN, OSHIFT_RIGHT_ASSIGN,
    OBIT_AND_ASSIGN
  );

  public static final TokenSet BINARY_OPERATORS = TokenSet.create(
    OCOND_OR, OCOND_AND,
    OEQ, ONOT_EQ, OLESS, OLESS_OR_EQUAL, OGREATER, OGREATER_OR_EQUAL,
    OPLUS, OMINUS, OBIT_OR, OBIT_XOR,
    OMUL, OQUOTIENT, OREMAINDER, OSHIFT_LEFT, OSHIFT_RIGHT, OBIT_AND
  );

  public static final TokenSet INC_DEC_OPERATORS = TokenSet.create(
    OPLUS_PLUS, OMINUS_MINUS
  );

  public static final TokenSet OPERATORS = TokenSet.create(
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

