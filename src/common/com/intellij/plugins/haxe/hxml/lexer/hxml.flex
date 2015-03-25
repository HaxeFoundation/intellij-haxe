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
VALUE_CHARACTER=[^\n\r\f\\]
END_OF_LINE_COMMENT=("#")[^\r\n]*
SEPARATOR=[\ ]
KEY_CHARACTER=[^\ \n\r\t\f\\]
CLASS_NAME_WORD=[a-z]
QUALIFIED_NAME_WORD=[a-z]+"."

%state WAITING_VALUE

%%

<YYINITIAL> {END_OF_LINE_COMMENT}                           { yybegin(YYINITIAL); return HXMLTypes.COMMENT; }
<YYINITIAL> {FIRST_KEY_CHARACTER}{KEY_CHARACTER}+           { yybegin(YYINITIAL); return HXMLTypes.KEY; }
<YYINITIAL> {FIRST_CLASS_CHARACTER}{CLASS_NAME_WORD}+       { yybegin(YYINITIAL); return HXMLTypes.QUALIFIEDCLASSNAME; }
<YYINITIAL> {QUALIFIED_NAME_WORD}+{FIRST_CLASS_CHARACTER}{CLASS_NAME_WORD}+
                                                            { yybegin(YYINITIAL); return HXMLTypes.QUALIFIEDCLASSNAME; }
<YYINITIAL> {SEPARATOR}                                     { yybegin(WAITING_VALUE); return HXMLTypes.SEPARATOR; }
<WAITING_VALUE> {CRLF}                                      { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
<WAITING_VALUE> {WHITE_SPACE}+                              { yybegin(WAITING_VALUE); return TokenType.WHITE_SPACE; }
<WAITING_VALUE> {FIRST_VALUE_CHARACTER}{VALUE_CHARACTER}*   { yybegin(YYINITIAL); return HXMLTypes.VALUE; }
{CRLF}                                                      { yybegin(YYINITIAL); return HXMLTypes.CRLF; }
{WHITE_SPACE}+                                              { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }
.                                                           { return TokenType.BAD_CHARACTER; }