package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.*;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;

%%

%unicode
%class _HaxeLexer
%implements FlexLexer, HaxeTokenTypes, HaxeTokenTypeSets
%unicode
%public

%function advance
%type IElementType

%eof{
%eof}

WHITE_SPACE_CHAR=[\ \n\r\t\f]

mLETTER = [:letter:] | "_"
mDIGIT = [:digit:]

IDENTIFIER={mLETTER} ({mDIGIT} | {mLETTER})*

C_STYLE_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
END_OF_LINE_COMMENT="/""/"[^\r\n]*

mHEX_DIGIT = [0-9A-Fa-f]
mINT_DIGIT = [0-9]
mOCT_DIGIT = [0-7]

mNUM_INT = "0" | ([1-9] {mINT_DIGIT}*)
mNUM_HEX = ("0x" | "0X") {mHEX_DIGIT}+
mNUM_OCT = "0" {mOCT_DIGIT}+

mFLOAT_EXPONENT = [eE] [+-]? {mDIGIT}+
mNUM_FLOAT = ( ({mDIGIT}* "." {mDIGIT}+) {mFLOAT_EXPONENT}?) | ({mDIGIT}+ {mFLOAT_EXPONENT})

CHARACTER_LITERAL="'"([^\\\'\r\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
STRING_LITERAL=\"([^\\\"\r\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ESCAPE_SEQUENCE=\\[^\r\n]

%%

<YYINITIAL> {

{WHITE_SPACE_CHAR}+                       { return com.intellij.psi.TokenType.WHITE_SPACE;}

{END_OF_LINE_COMMENT}                     { return MSL_COMMENT; }
{C_STYLE_COMMENT}                         { return MML_COMMENT; }
{DOC_COMMENT}                             { return DOC_COMMENT; }

{CHARACTER_LITERAL}                       { return LITCHAR; }
{STRING_LITERAL}                          { return LITSTRING; }

"..."                                     { return OTRIPLE_DOT; }

{mNUM_FLOAT}                              {  return LITFLOAT; }
{mNUM_OCT}                                {  return LITOCT; }
{mNUM_HEX}                                {  return LITHEX; }
{mNUM_INT}                                {  return LITINT; }

"new"                                     { return ONEW; }
"in"                                      { return OIN; }

"break"                                   { return( KBREAK );  }
"default"                                 { return( KDEFAULT );  }
"package"                                 { return( KPACKAGE );  }
"function"                                { return( KFUNCTION );  }

"case"                                    { return( KCASE );  }
"cast"                                    { return( KCAST );  }

"class"                                   {  return( KCLASS );  }
"enum"                                    {  return( KENUM );  }
"interface"                               {  return( KINTERFACE );  }

"implements"                              {  return( KIMPLEMENTS );  }
"extends"                                 {  return( KEXTENDS );  }

"if"                                      {  return KIF ;  }
"null"                                    {  return KNULL ;  }
"this"                                    {  return KTHIS ;  }

"for"                                     {  return KFOR ;  }
"do"                                      {  return KDO ;  }
"while"                                   {  return KWHILE ;  }
"return"                                  {  return KRETURN ;  }
"import"                                  {  return KIMPORT ;  }
"continue"                                {  return KCONTINUE ;  }
"else"                                    {  return KELSE ;  }
"switch"                                  {  return KSWITCH ;  }
"throw"                                   {  return KTHROW ;  }

"var"                                     {  return KVAR;  }
"public"                                  {  return KPUBLIC;  }
"private"                                 {  return KPRIVATE;  }
"static"                                  {  return KSTATIC;  }
"dynamic"                                 {  return KDYNAMIC;  }
"override"                                {  return KOVERRIDE;  }
"inline"                                  {  return KINLINE;  }

"untype"                                  {  return KUNTYPE;  }
"typedef"                                 {  return KTYPEDEF;  }

"try"                                     {  return KTRY;  }
"catch"                                   {  return KCATCH;  }


{IDENTIFIER}                              {  return ID; }

"."                                       { return ODOT; }

"{"                                       { return PLCURLY; }
"}"                                       { return PRCURLY; }

"["                                       { return PLBRACK; }
"]"                                       { return PRBRACK; }

"("                                       { return PLPAREN; }
")"                                       { return PRPAREN; }

":"                                       { return OCOLON; }
";"                                       { return OSEMI; }
","                                       { return OCOMMA; }

"=="                                      { return OEQ; }
"="                                       { return OASSIGN; }

"!="                                      { return ONOT_EQ; }
"!"                                       { return ONOT; }
"~"                                       { return OCOMPLEMENT; }

"?"                                       { return OQUEST;}

"++"                                      { return OPLUS_PLUS; }
"+="                                      { return OPLUS_ASSIGN; }
"+"                                       { return OPLUS; }

"--"                                      { return OMINUS_MINUS; }
"-="                                      { return OMINUS_ASSIGN; }
"-"                                       { return OMINUS; }

"||"                                      { return OCOND_OR; }
"|="                                      { return OBIT_OR_ASSIGN; }
"|"                                       { return OBIT_OR; }

"&&"                                      { return OCOND_AND; }
"&="                                      { return OBIT_AND_ASSIGN; }
"&"                                       { return OBIT_AND; }

"<<="                                     { return OSHIFT_LEFT_ASSIGN; }
"<<"                                      { return OSHIFT_LEFT; }
"<="                                      { return OLESS_OR_EQUAL; }
"<"                                       { return OLESS; }

"^="                                      { return OBIT_XOR_ASSIGN; }
"^"                                       { return OBIT_XOR; }

"*="                                      { return OMUL_ASSIGN; }
"*"                                       { return OMUL; }

"/="                                      { return OQUOTIENT_ASSIGN; }
"/"                                       { return OQUOTIENT; }

"%="                                      { return OREMAINDER_ASSIGN; }
"%"                                       { return OREMAINDER; }

">>>"                                     { return OUNSIGNED_SHIFT_RIGHT; }
">>="                                     { return OSHIFT_RIGHT_ASSIGN; }
">>"                                      { return OSHIFT_RIGHT; }
">="                                      { return OGREATER_OR_EQUAL; }
">"                                       { return OGREATER; }

"#if"                                     { return PPIF; }
"#end"                                    { return PPEND; }
"#error"                                  { return PPERROR; }
"#elseif"                                 { return PPELSEIF; }
"#else"                                   { return PPELSE; }
}

.                                         {  yybegin(YYINITIAL); return com.intellij.psi.TokenType.BAD_CHARACTER; }