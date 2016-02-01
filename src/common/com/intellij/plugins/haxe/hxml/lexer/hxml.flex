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

CRLF= \n|\r|\r\n
WHITE_SPACE=[\ \t\f]
FIRST_VALUE_CHARACTER=[^ \n\r\f\\]
FIRST_KEY_CHARACTER="-""-"?
FIRST_CLASS_CHARACTER=[A-Z]
H_QUALIFIED_NAME = {QUALIFIED_NAME_WORD}*({FIRST_CLASS_CHARACTER}{CLASS_NAME_WORD}*)+
VALUE_CHARACTER=[^\n\r\f\\]
LINE_COMMENT=("#")[^\r\n]*
SEPARATOR=[\ ]
KEY_CHARACTER=[^\ \n\r\t\f\\]
HXML_FILE=[A-Za-z0-9_\-\\/]+".hxml"
CLASS_NAME_WORD=[a-z0-9]
QUALIFIED_NAME_WORD=[a-zA-Z0-9]+"."

%state WAITING_VALUE

%%

<YYINITIAL> {LINE_COMMENT}                                  { yybegin(YYINITIAL); return HXMLTypes.COMMENT; }
<YYINITIAL> {FIRST_KEY_CHARACTER}{KEY_CHARACTER}+           { yybegin(YYINITIAL); return HXMLTypes.KEY; }
<YYINITIAL> {HXML_FILE}                                     { yybegin(YYINITIAL); return HXMLTypes.HXML_FILE; }
<YYINITIAL> {SEPARATOR}                                     { yybegin(WAITING_VALUE); return HXMLTypes.SEPARATOR; }
<YYINITIAL> {H_QUALIFIED_NAME}                              { yybegin(YYINITIAL); return HXMLTypes.QUALIFIEDCLASSNAME; }
<WAITING_VALUE> {CRLF}                                      { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
<WAITING_VALUE> {WHITE_SPACE}+                              { yybegin(WAITING_VALUE); return TokenType.WHITE_SPACE; }
<WAITING_VALUE> {H_QUALIFIED_NAME}                          { yybegin(YYINITIAL); return HXMLTypes.QUALIFIEDCLASSNAME; }
<WAITING_VALUE> {FIRST_VALUE_CHARACTER}{VALUE_CHARACTER}*   { yybegin(YYINITIAL); return HXMLTypes.VALUE; }
{CRLF}                                                      { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
{WHITE_SPACE}+                                              { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
.                                                           { return TokenType.BAD_CHARACTER; }
