package com.intellij.plugins.haxe.hxml.lexer;

import com.intellij.plugins.haxe.hxml.psi.HXMLTypes;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%
%{
 public HXMLLexer() {
   this((java.io.Reader)null);
 }
%}

%class HXMLLexer
%implements FlexLexer, HXMLTypes
%unicode
%public
%function advance
%type IElementType
%eof{
  return;
%eof}

CRLF=\n|\r|\r\n
WHITE_SPACE=[\ \t\f]

FIRST_KEY_CHARACTER="-""-"?
KEY_CHARACTER=[^\ \n\r\t\f\\]

FIRST_CLASS_CHARACTER=[_A-Z]
CLASS_NAME_WORD=[_a-zA-Z0-9]

CLASS_NAME={FIRST_CLASS_CHARACTER}{CLASS_NAME_WORD}*

FIRST_VALUE_CHARACTER=[^\ \t\n\r\f]
VALUE_CHARACTER=[^\n\r\f]
VALUE_PART={FIRST_VALUE_CHARACTER}{VALUE_CHARACTER}*{FIRST_VALUE_CHARACTER}+

LINE_COMMENT=("#")[^\r\n\f]*
SEPARATOR=[\ \t]

HXML_EXTENSION="hxml"

HXML_FILE_PATTERN=({FILE_PATH_FRAGMENT}*{SLASH})* ({IDENTIFIER}{DOT})+ {HXML_EXTENSION}
FILE_PATTERN=({FILE_PATH_FRAGMENT}*{SLASH})+ {FILE_PATH_FRAGMENT}+ {SLASH}?
QNAME_PATTERN=({IDENTIFIER}{DOT})+{IDENTIFIER}

FILE_PATH_FRAGMENT=({DOTDOT}|{DOT}|{IDENTIFIER})

IDENTIFIER = [a-zA-Z0-9_-]+
DOT = "."
DOTDOT = ".."
SLASH = "/"


%state WAITING_VALUE PATH_VALUE QNAME_VALUE

%%
<YYINITIAL> {
{LINE_COMMENT}                              { return HXMLTypes.COMMENT; }
{FIRST_KEY_CHARACTER}{KEY_CHARACTER}+       { return HXMLTypes.KEY_TOKEN; }

{SEPARATOR}+                                { yybegin(WAITING_VALUE); return TokenType.WHITE_SPACE; }

{HXML_FILE_PATTERN}                         { yybegin(PATH_VALUE);  yypushback(yylength()); }
{FILE_PATTERN}                              { yybegin(PATH_VALUE);  yypushback(yylength()); }
{QNAME_PATTERN}                             { yybegin(QNAME_VALUE);  yypushback(yylength()); }
{CRLF}                                      {return HXMLTypes.CRLF; }

.                                           {return HXMLTypes.UNKNOWN;}
}

<WAITING_VALUE> {
{WHITE_SPACE}+                              { return TokenType.WHITE_SPACE; }

{FILE_PATTERN}                              { yybegin(PATH_VALUE);  yypushback(yylength()); }
{QNAME_PATTERN}                             { yybegin(QNAME_VALUE);  yypushback(yylength()); }
{CLASS_NAME} / [^/\.]                       { yybegin(YYINITIAL); return HXMLTypes.CLASS_NAME; }
{VALUE_PART}                                { yybegin(YYINITIAL); return HXMLTypes.VALUE_TOKEN; }
{DOTDOT} /{SEPARATOR}|{CRLF}                { yybegin(YYINITIAL); return HXMLTypes.DOTDOT; }
{DOT}   /{SEPARATOR}|{CRLF}                 { yybegin(YYINITIAL); return HXMLTypes.DOT; }
{CRLF}                                      { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
.                                           { yybegin(YYINITIAL);  yypushback(yylength()); }
}

<PATH_VALUE> {
{HXML_EXTENSION} /[^/\.]                     { return HXMLTypes.HXML_EXTENSION; }
{DOTDOT}                                     { return HXMLTypes.DOTDOT; }
{DOT}                                        { return HXMLTypes.DOT; }
{SLASH}                                      { return HXMLTypes.SLASH; }
{IDENTIFIER}                                 { return HXMLTypes.IDENTIFER; }

{SEPARATOR}                                  { yybegin(YYINITIAL);  yypushback(yylength()); }
.                                            { return TokenType.BAD_CHARACTER; }
}

<QNAME_VALUE> {
{DOT}                                        { return HXMLTypes.DOT; }
{IDENTIFIER}                                 { return HXMLTypes.IDENTIFER; }

{SEPARATOR}                                  { yybegin(YYINITIAL);  yypushback(yylength()); }
.                                            { return TokenType.BAD_CHARACTER; }
}

{CRLF}+                                      { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
{WHITE_SPACE}+                               { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
.                                            { return TokenType.BAD_CHARACTER; }

