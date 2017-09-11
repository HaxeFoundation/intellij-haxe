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
%implements FlexLexer
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

QUALIFIED_NAME_WORD=[_a-zA-Z0-9]+"."
FIRST_CLASS_CHARACTER=[_A-Z]
CLASS_NAME_WORD=[_a-zA-Z0-9]
H_QUALIFIED_NAME = {QUALIFIED_NAME_WORD}*({FIRST_CLASS_CHARACTER}{CLASS_NAME_WORD}*)+

FIRST_VALUE_CHARACTER=[^\ \t\n\r\f]
VALUE_CHARACTER=[^\n\r\f]
VALUE_PART={FIRST_VALUE_CHARACTER}{VALUE_CHARACTER}*{FIRST_VALUE_CHARACTER}+

LINE_COMMENT=("#")[^\r\n\f]*
SEPARATOR=[\ \t]
HXML_FILE=[A-Za-z0-9_\-\\/]+".hxml"

%state WAITING_VALUE

%%

<YYINITIAL> {LINE_COMMENT}                                  { yybegin(YYINITIAL); return HXMLTypes.COMMENT; }
<YYINITIAL> {H_QUALIFIED_NAME}                              { yybegin(YYINITIAL); return HXMLTypes.QUALIFIEDCLASSNAME; }
<YYINITIAL> {HXML_FILE}                                     { yybegin(YYINITIAL); return HXMLTypes.HXML_FILE; }
<YYINITIAL> {FIRST_KEY_CHARACTER}{KEY_CHARACTER}+           { yybegin(YYINITIAL); return HXMLTypes.KEY_TOKEN; }
<YYINITIAL> {SEPARATOR}+                                    { yybegin(WAITING_VALUE); return TokenType.WHITE_SPACE; }
<WAITING_VALUE> {WHITE_SPACE}+                              { yybegin(WAITING_VALUE); return TokenType.WHITE_SPACE; }
<WAITING_VALUE> {CRLF}                                      { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
<WAITING_VALUE> {H_QUALIFIED_NAME}                          { yybegin(YYINITIAL); return HXMLTypes.QUALIFIEDCLASSNAME; }
<WAITING_VALUE> {VALUE_PART}                                { yybegin(YYINITIAL); return HXMLTypes.VALUE_TOKEN; }
{CRLF}+                                                     { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
{WHITE_SPACE}+                                              { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
.                                                           { return TokenType.BAD_CHARACTER; }
