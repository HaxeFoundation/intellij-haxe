/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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

  IElementType PPEXPRESSION = new HaxeElementType("PPEXPRESSION");
  IElementType PPBODY = new HaxeElementType("PPBODY");

  IElementType WSNLS = new HaxeElementType("WSNLS");

  TokenSet WHITESPACES = TokenSet.create(
    WSNLS,
    TokenType.WHITE_SPACE,
    TokenType.NEW_LINE_INDENT
  );

  TokenSet ONLY_COMMENTS = TokenSet.create(
    MML_COMMENT,
    MSL_COMMENT,
    DOC_COMMENT
  );

  TokenSet COMMENTS = TokenSet.create(
    MML_COMMENT,
    MSL_COMMENT,
    DOC_COMMENT,
    PPIF,
    PPEND,
    PPELSE,
    PPERROR,
    PPBODY,
    PPEXPRESSION
  );

  TokenSet CONDITIONALLY_NOT_COMPILED = TokenSet.create(
    PPIF,
    PPEND,
    PPELSE,
    PPERROR,
    PPBODY,
    PPEXPRESSION
  );

  TokenSet BAD_TOKENS = TokenSet.create(
    TokenType.BAD_CHARACTER
  );

  TokenSet STRINGS = TokenSet.create(
    OPEN_QUOTE,
    CLOSING_QUOTE,
    REGULAR_STRING_PART
  );

  TokenSet KEYWORDS = TokenSet.create(
    KBREAK,
    KCASE,
    KCAST,
    KCLASS,
    KABSTRACT,
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
    KEXTERN,
    KFINAL,
    KHACK,
    KNATIVE,
    KMACRO,
    KBUILD,
    KAUTOBUILD,
    KKEEP,
    KREQUIRE,
    KFAKEENUM,
    KCOREAPI,
    KBIND,
    KBITMAP,
    KNS,
    KPROTECTED,
    KGETTER,
    KSETTER,
    KDEBUG,
    KNODEBUG,
    KMETA,
    KUSING,
    KSUPER,
    MACRO_ID,
    KFROM,
    KTO,
    KNEVER,
    ONEW
  );

  TokenSet FUNCTION_DEFINITION = TokenSet.create(
    FUNCTION_DECLARATION_WITH_ATTRIBUTES,
    LOCAL_FUNCTION_DECLARATION,
    FUNCTION_LITERAL
  );

  TokenSet BINARY_EXPRESSIONS = TokenSet.create(
    LOGIC_OR_EXPRESSION,
    LOGIC_AND_EXPRESSION,
    COMPARE_EXPRESSION,
    SHIFT_EXPRESSION,
    ADDITIVE_EXPRESSION,
    MULTIPLICATIVE_EXPRESSION
  );

  TokenSet BINARY_OPERATORS = TokenSet.create(
    BIT_OPERATION,
    OCOND_OR, OCOND_AND,
    COMPARE_OPERATION,
    SHIFT_OPERATOR,
    OPLUS, OMINUS,
    OMUL, OQUOTIENT, OREMAINDER
  );

  TokenSet ASSIGN_OPERATORS = TokenSet.create(
    OASSIGN,
    OPLUS_ASSIGN, OMINUS_ASSIGN, OBIT_OR_ASSIGN, OBIT_XOR_ASSIGN,
    OMUL_ASSIGN, OQUOTIENT_ASSIGN, OREMAINDER_ASSIGN,
    OSHIFT_LEFT_ASSIGN, OSHIFT_RIGHT_ASSIGN,
    OBIT_AND_ASSIGN,
    ASSIGN_OPERATION
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
    BIT_OPERATION
  );

  TokenSet SHIFT_OPERATORS = TokenSet.create(
    OSHIFT_LEFT,
    SHIFT_RIGHT_OPERATOR,
    UNSIGNED_SHIFT_RIGHT_OPERATOR,
    SHIFT_OPERATOR
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

