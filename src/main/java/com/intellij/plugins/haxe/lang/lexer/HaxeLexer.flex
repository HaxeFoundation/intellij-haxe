/*
    Syntax and docs here:
    https://jflex.de/manual.html#ExampleUserCode
*/
package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

%%

/*
Options and declarations
*/
%public
%class HaxeLexer
%extends ConditionalCompilationLexer
%implements FlexLexer, HaxeTokenTypes, HaxeTokenTypeSets
%function advance
%type IElementType
%unicode


%{
   public HaxeLexer(Project context) {
    this((java.io.Reader)null);
    createConditionalCompilationSupport(context);
  }
%}


%xstate QUOTATION_STRING
%xstate APOSTROPHE_STRING
%xstate SHORT_STRING_INTERPOLATION_ENTRY
%xstate LONG_STRING_INTERPOLATION_ENTRY
%xstate METADATA
%xstate METADATA_ARGUMENTS

//TODO
//%xstate CONDITIONAL_COMPILATION
%xstate CONDITIONAL_COMPILATION_EXPRESSION
%xstate CONDITIONAL_COMPILATION_BLOCK
%xstate CONDITIONAL_COMPILATION_PASSIVE

%xstate CC_QUOTATION_STRING
%xstate CC_APOSTROPHE_STRING


WHITE_SPACE=\s+
LITERAL_SPACE=[ \t\n\x0B\f\r]+
/*
    helpers
*/
_LETTER = [:letter:] | "_"
_DIGIT = [:digit:]
_FLOAT_EXPONENT = [eE] [+-]? {_DIGIT}+


/*
    Numbers
*/
LITERAL_HEXADECIMAL = ("0x" | "0X") [0-9A-Fa-f]+
LITERAL_INTEGER = "0" | ([1-9] [0-9]*)
LITERAL_OCTAL = "0" [0-7]+
LITERAL_FLOAT = ( (({_DIGIT}* "." {_DIGIT}+) | ({_DIGIT}+ "." {_DIGIT}*)) {_FLOAT_EXPONENT}?) | ({_DIGIT}+ {_FLOAT_EXPONENT})

/*
    Comments & docs
*/
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
BLOCK_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"
LINE_COMMENT="/""/"[^\r\n]*
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?


/*
    Strings & String Interpolation
*/
STRING_ESCAPE_SEQUENCE=\\[^\r\n]
QUOTATION_STRING_PART=[^\\\"]+
APOSTROPHE_STRING_PART=[^\\\'\$]+


REG_EXP = "~/" ([^"/"] | {STRING_ESCAPE_SEQUENCE})* "/" [igmsu]*

LONELY_DOLLAR=\$
SHORT_INTERPOLATION_ENTRY=\${IDENTIFIER_NO_DOLLAR}
LONG_INTERPOLATION_ENTRY_START=\$\{
//LONG_INTERPOLATION_ENTRY_END

/*
    metadata
*/
COMPILE_META_PREFIX="@:"
RUNTIME_META_PREFIX="@"

META_PART = {mLETTER} ({mDIGIT} | {mLETTER})*
META_TYPE={META_PART} ("." {META_PART})*


/**
    identifiers
*/

//TODO better naming
IDENTIFIER_START_NO_DOLLAR={_LETTER}|"_"
IDENTIFIER_START={IDENTIFIER_START_NO_DOLLAR}|"$"
IDENTIFIER_PART_NO_DOLLAR={IDENTIFIER_START_NO_DOLLAR}|{_DIGIT}
IDENTIFIER_PART={IDENTIFIER_START}|{_DIGIT}
IDENTIFIER={IDENTIFIER_START}{IDENTIFIER_PART}*
IDENTIFIER_NO_DOLLAR={IDENTIFIER_START_NO_DOLLAR}{IDENTIFIER_PART_NO_DOLLAR}*

// todo make more readable (see old code meta_part)
IDENTIFIER_METADATA_NO_ARGS={_LETTER}("."?({_LETTER}|{_DIGIT})*)*

CONDITIONAL_IDENTIFIER={IDENTIFIER_NO_DOLLAR}
//
//COMPILE_META={COMPILE_META_PREFIX}{IDENTIFIER_METADATA_NO_ARGS}
//RUNTIME_META={RUNTIME_META_PREFIX}{IDENTIFIER_METADATA_NO_ARGS}


mLETTER = [:letter:] | "_"
mDIGIT = [:digit:]
ESCAPE_SEQUENCE=\\[^\r\n]

ID= {mLETTER} ({mDIGIT} | {mLETTER})*
MACRO_ID="$"{mLETTER} ({mDIGIT} | {mLETTER})*


/*
Lexical rules
*/
%%

<YYINITIAL, METADATA_ARGUMENTS, CONDITIONAL_COMPILATION_BLOCK, CONDITIONAL_COMPILATION_PASSIVE, LONG_STRING_INTERPOLATION_ENTRY>
{
    {WHITE_SPACE}                   { return emitToken(WHITE_SPACE); }

    "package"                       { return emitToken(KEYWORD_PACKAGE); }
    "import"                        { return emitToken(KEYWORD_IMPORT); }
    "using"                         { return emitToken(KEYWORD_USING); }
    "class"                         { return emitToken(KEYWORD_CLASS); }
    "enum"                          { return emitToken(KEYWORD_ENUM); }
    "abstract"                      { return emitToken(KEYWORD_ABSTRACT); }
    "interface"                     { return emitToken(KEYWORD_INTERFACE); }
    "implements"                    { return emitToken(KEYWORD_IMPLEMENTS); }
    "extends"                       { return emitToken(KEYWORD_EXTENDS); }
    "untyped"                       { return emitToken(KEYWORD_UNTYPED); }
    "typedef"                       { return emitToken(KEYWORD_TYPEDEF); }
    "extern"                        { return emitToken(KEYWORD_EXTERN); }
    "if"                            { return emitToken(KEYWORD_IF); }
    "else"                          { return emitToken(KEYWORD_ELSE); }
    "switch"                        { return emitToken(KEYWORD_SWITCH); }
    "case"                          { return emitToken(KEYWORD_CASE); }
    "default"                       { return emitToken(KEYWORD_DEFAULT); }
    "for"                           { return emitToken(KEYWORD_FOR); }
    "do"                            { return emitToken(KEYWORD_DO); }
    "while"                         { return emitToken(KEYWORD_WHILE); }
    "break"                         { return emitToken(KEYWORD_BREAK); }
    "continue"                      { return emitToken(KEYWORD_CONTINUE); }
    "return"                        { return emitToken(KEYWORD_RETURN); }
    "throw"                         { return emitToken(KEYWORD_THROW); }
    "try"                           { return emitToken(KEYWORD_TRY); }
    "catch"                         { return emitToken(KEYWORD_CATCH); }
    "var"                           { return emitToken(KEYWORD_VAR); }
    "function"                      { return emitToken(KEYWORD_FUNCTION); }
    "new"                           { return emitToken(KEYWORD_NEW); }
    "public"                        { return emitToken(KEYWORD_PUBLIC); }
    "private"                       { return emitToken(KEYWORD_PRIVATE); }
    "final"                         { return emitToken(KEYWORD_FINAL); }
    "static"                        { return emitToken(KEYWORD_STATIC); }
    "override"                      { return emitToken(KEYWORD_OVERRIDE); }
    "this"                          { return emitToken(KEYWORD_THIS); }
    "super"                         { return emitToken(KEYWORD_SUPER); }
      // for it to be keyword it must  be followed by a whitespace  (and not part of say a refrence like mypackage.macro.MyMacro)
//    "macro"                         { return KEYWORD_MACRO; }
    "macro" [\ ]+                   { return emitToken(KEYWORD_MACRO); }
    "dynamic"                       { return emitToken(KEYWORD_DYNAMIC); }
      //NOT reserved keywords, variables and methods can be call "is"
//    "is"                            { return KEYWORD_IS; }
    "cast"                          { return emitToken(KEYWORD_CAST); }
    "inline"                        { return emitToken(KEYWORD_INLINE); }
      //NOT reserved keywords, only used  getter setters?
//    "never"                         { return KEYWORD_NEVER; }
      //NOT reserved keywords, only used for abstracts ?
//    "from"                          { return KEYWORD_FROM; }
//    "to"                            { return KEYWORD_TO; }
    "null"                          { return emitToken(KEYWORD_NULL); }
    "true"                          { return emitToken(KEYWORD_TRUE); }
    "false"                         { return emitToken(KEYWORD_FALSE); }
    ":"                             { return emitToken(OPERATOR_COLON); }
    ";"                             { return emitToken(OPERATOR_SEMICOLON); }
    ","                             { return emitToken(OPERATOR_COMMA); }
    "."                             { return emitToken(OPERATOR_DOT); }
    "=="                            { return emitToken(OPERATOR_EQ); }
    "="                             { return emitToken(OPERATOR_ASSIGN); }
    "!="                            { return emitToken(OPERATOR_NOT_EQ); }
    "!"                             { return emitToken(OPERATOR_NOT); }
    "~"                             { return emitToken(OPERATOR_COMPLEMENT); }
    "++"                            { return emitToken(OPERATOR_PLUS_PLUS); }
    "+="                            { return emitToken(OPERATOR_PLUS_ASSIGN); }
    "+"                             { return emitToken(OPERATOR_PLUS); }
    "--"                            { return emitToken(OPERATOR_MINUS_MINUS); }
    "-="                            { return emitToken(OPERATOR_MINUS_ASSIGN); }
    "-"                             { return emitToken(OPERATOR_MINUS); }
    "?"                             { return emitToken(OPERATOR_TERNARY); }
    "||"                            { return emitToken(OPERATOR_COND_OR); }
    "|"                             { return emitToken(OPERATOR_BIT_OR); }
    "|="                            { return emitToken(OPERATOR_BIT_OR_ASSIGN); }
    "&&"                            { return emitToken(OPERATOR_COND_AND); }
    "&="                            { return emitToken(OPERATOR_BIT_AND_ASSIGN); }
    "&"                             { return emitToken(OPERATOR_BIT_AND); }
    "<<="                           { return emitToken(OPERATOR_SHIFT_LEFT_ASSIGN); }
    "<<"                            { return emitToken(OPERATOR_SHIFT_LEFT); }
    "<="                            { return emitToken(OPERATOR_LESS_OR_EQUAL); }

// Ignore to prevent wrong parsing of functiontyps with parameters (ex. ParamterizedType<T>->Void;)
    "<"                             { return emitToken(OPERATOR_LESS); }

    "^="                            { return emitToken(OPERATOR_BIT_XOR_ASSIGN); }
    "^"                             { return emitToken(OPERATOR_BIT_XOR); }
    "*="                            { return emitToken(OPERATOR_MUL_ASSIGN); }
    "*"                             { return emitToken(OPERATOR_MUL); }
    "/="                            { return emitToken(OPERATOR_QUOTIENT_ASSIGN); }
    "/"                             { return emitToken(OPERATOR_QUOTIENT); }
    "%="                            { return emitToken(OPERATOR_REMAINDER_ASSIGN); }
    "%"                             { return emitToken(OPERATOR_REMAINDER); }

// Ignore all variants of >> as they cause trouble for generic parsing ex. Array<Array<Int>>
//    ">>>="                          { return OPERATOR_UNSIGNED_SHIFT_RIGHT_ASSIGN; }
//    ">>>"                           { return OPERATOR_UNSIGNED_SHIFT_RIGHT; }
//    ">>="                           { return OPERATOR_SHIFT_RIGHT_ASSIGN; }
//    ">>"                            { return OPERATOR_SHIFT_RIGHT; }

    ">="                            { return emitToken(OPERATOR_GREATER_OR_EQUAL); }
    ">"                             { return emitToken(OPERATOR_GREATER); }
    "..."                           { return emitToken(OPERATOR_TRIPLE_DOT); }
      // Detect the '...' in 'for (a in 0...10)' so that '0.' is not picked up as a float.
      // This rule must appear before the '{mNUM_FLOAT}' rule.
      // This is used instead of the lookahead '{mNUM_FLOAT} / [^"."]}' regexp because the
      // lookahead precludes proper detection at the end of the file/stream.
    {LITERAL_INTEGER} ".."          {  yypushback(2); return emitToken(LITERAL_INTEGER); }

    "in"                            { return emitToken(OPERATOR_IN); }
    "->"                            { return emitToken(OPERATOR_ARROW); }
    "=>"                            { return emitToken(OPERATOR_FAT_ARROW); }



    {REG_EXP}                       { return emitToken(REG_EXP); }

//    {COMPILE_META}                  { return emitToken(COMPILE_META); }
//    {RUNTIME_META}                  { return emitToken(RUNTIME_META); }
    {COMPILE_META_PREFIX}          { pushState(METADATA); return emitToken(COMPILE_META_PREFIX); }
    {RUNTIME_META_PREFIX}          { pushState(METADATA); return emitToken(RUNTIME_META_PREFIX); }

    {LINE_COMMENT}                  { return emitToken(LINE_COMMENT); }
    {BLOCK_COMMENT}                 { return emitToken(BLOCK_COMMENT); }
    {DOC_COMMENT}                   { return emitToken(DOC_COMMENT); }

    {LITERAL_INTEGER}               { return emitToken(LITERAL_INTEGER); }
    {LITERAL_HEXADECIMAL}           { return emitToken(LITERAL_HEXADECIMAL); }
    {LITERAL_OCTAL}                 { return emitToken(LITERAL_OCTAL); }
    {LITERAL_FLOAT}                 { return emitToken(LITERAL_FLOAT); }
    {MACRO_ID}                      { return emitToken(MACRO_ID); }
    \$                              { return emitToken(DOLLAR); } // used for MACRO shorthand Expression
//    {MACRO_EXP}                     { return MACRO_EXP; }
    {ID}                            { return emitToken(ID); }




//  "#if"                                     { return CONDITIONAL_COMPILATION_IF; }
//  "#elseif"                                 { return CONDITIONAL_COMPILATION_ELSE; }
//  "#else"                                   { return CONDITIONAL_COMPILATION_ELSEIF; }
//

//
//  "#if"                                     { pushState(CONDITIONAL_COMPILATION_EXPRESSION); return processConditional(CONDITIONAL_COMPILATION_IF); }
//  "#elseif"                                 { pushState(CONDITIONAL_COMPILATION_EXPRESSION); return processConditional(CONDITIONAL_COMPILATION_ELSEIF); }
//  "#else"                                   { return evaluate(processConditional(CONDITIONAL_COMPILATION_ELSE)); }
//  "#end"                                    { return evaluate(processConditional(CONDITIONAL_COMPILATION_END)); }
//  "#error"                                  { return evaluate(processConditional(CONDITIONAL_COMPILATION_ERROR)); }
//  "#line"                                   { return evaluate(processConditional(CONDITIONAL_COMPILATION_LINE)); }

  "#if"                                     { return processConditional(CONDITIONAL_COMPILATION_IF); }
  "#elseif"                                 { return processConditional(CONDITIONAL_COMPILATION_ELSEIF); }
  "#else"                                   { return processConditional(CONDITIONAL_COMPILATION_ELSE); }
  "#end"                                    { return processConditional(CONDITIONAL_COMPILATION_END); }
  "#error"                                  { return processConditional(CONDITIONAL_COMPILATION_ERROR); }
  "#line"                                   { return processConditional(CONDITIONAL_COMPILATION_LINE); }

}

<YYINITIAL, METADATA_ARGUMENTS, CONDITIONAL_COMPILATION_BLOCK,  LONG_STRING_INTERPOLATION_ENTRY>
{
    \"                              { pushState(QUOTATION_STRING); return emitToken(OPEN_QUOTE);}
    \'                              { pushState(APOSTROPHE_STRING); return emitToken(OPEN_APOSTROPHE);}
}

<CONDITIONAL_COMPILATION_PASSIVE>
{
// TODO replace when passive tokens has been made
    \"                              { pushState(CC_QUOTATION_STRING); return emitToken(CC_OPEN_QUOTE);}
    \'                              { pushState(CC_APOSTROPHE_STRING); return emitToken(CC_OPEN_APOSTROPHE);}
}

<YYINITIAL, CONDITIONAL_COMPILATION_BLOCK, CONDITIONAL_COMPILATION_PASSIVE,  LONG_STRING_INTERPOLATION_ENTRY> {
    "["                             { return emitToken(ENCLOSURE_BRACKET_LEFT); }
    "]"                             { return emitToken(ENCLOSURE_BRACKET_RIGHT); }
    "("                             { return emitToken(ENCLOSURE_PARENTHESIS_LEFT); }
    ")"                             { return emitToken(ENCLOSURE_PARENTHESIS_RIGHT); }
}


<METADATA_ARGUMENTS> {
    "["                             { return emitToken(ENCLOSURE_BRACKET_LEFT); }
    "]"                             { return emitToken(ENCLOSURE_BRACKET_RIGHT); }

    "("                             { leftParenCount++; return ENCLOSURE_PARENTHESIS_LEFT; }
    ")"                             {
                                          if (--leftParenCount == 0) popState();
                                          return emitToken(ENCLOSURE_PARENTHESIS_RIGHT);
                                    }
 . {  popState();  }// if nothing is matched  pop state so lexer wont break
}

<YYINITIAL, METADATA_ARGUMENTS, CONDITIONAL_COMPILATION_BLOCK, CONDITIONAL_COMPILATION_PASSIVE> {
    "{"                             { return emitToken(ENCLOSURE_CURLY_BRACKET_LEFT); }
    "}"                             { return emitToken(ENCLOSURE_CURLY_BRACKET_RIGHT); }
}
<CONDITIONAL_COMPILATION_PASSIVE> {
    .                             { popState(); return emitToken(CC_PASSIVE); }
}

<QUOTATION_STRING> {
    {STRING_ESCAPE_SEQUENCE}              { return emitToken(STRING_ESCAPE); }
    {QUOTATION_STRING_PART}               { return emitToken(REGULAR_STRING_PART); }
    \"                                    { popState(); return emitToken(CLOSING_QUOTE);}
}
<APOSTROPHE_STRING> {
    {APOSTROPHE_STRING_PART}               { return emitToken(REGULAR_STRING_PART); }
    {SHORT_INTERPOLATION_ENTRY}            { pushState(SHORT_STRING_INTERPOLATION_ENTRY); yypushback(yylength() -1); return emitToken(SHORT_INTERPOLATION_ENTRY); }
    {LONG_INTERPOLATION_ENTRY_START}       { pushState(LONG_STRING_INTERPOLATION_ENTRY);  return emitToken(LONG_INTERPOLATION_ENTRY_START); }
    {STRING_ESCAPE_SEQUENCE}               { return emitToken(STRING_ESCAPE); }
    \$                                     { return emitToken(REGULAR_STRING_PART); }
    \'                                     { popState(); return emitToken(CLOSING_APOSTROPHE); }
}
<CC_QUOTATION_STRING> {
    {STRING_ESCAPE_SEQUENCE}              { return emitToken(CC_STRING_ESCAPE); }
    {QUOTATION_STRING_PART}               { return emitToken(CC_REGULAR_STRING_PART); }
    \"                                    { popState(); return emitToken(CC_CLOSING_QUOTE);}
}
<CC_APOSTROPHE_STRING> {
    {STRING_ESCAPE_SEQUENCE}              { return emitToken(CC_STRING_ESCAPE); }
    {APOSTROPHE_STRING_PART}               { return emitToken(CC_REGULAR_STRING_PART); }
    \'                                    { popState(); return emitToken(CC_CLOSING_APOSTROPHE);}
}

<SHORT_STRING_INTERPOLATION_ENTRY> {
    "this"                                  { popState(); return emitToken(KEYWORD_THIS); }
    {IDENTIFIER_NO_DOLLAR}                  { popState(); return emitToken(ID); }
}
<LONG_STRING_INTERPOLATION_ENTRY> {
    "{"                 { leftBraceCount++; return ENCLOSURE_CURLY_BRACKET_LEFT; }
    "}"                 {
                              if (leftBraceCount == 0) {
                                popState();
                                return emitToken(LONG_INTERPOLATION_ENTRY_END);
                              }else {
                                leftBraceCount--;
                                return emitToken(ENCLOSURE_CURLY_BRACKET_RIGHT);
                              }
                         }
 . {  popState();  }// if nothing is matched  pop state so lexer wont break
}

<METADATA> {
    {META_TYPE}                  {return emitToken(META_TYPE);}
      "("                        {
                                  leftParenCount++;
                                  pushState(METADATA_ARGUMENTS);
                                  return emitToken(METADATA_PARENTHESIS_LEFT);
                                 }
      ")"                        {
                                  leftParenCount--;
                                  if(leftParenCount == 0) popState();
                                  return emitToken(METADATA_PARENTHESIS_RIGHT);
                                 }
    .                            { popState(); }
   [^]                           { popState(); }
}




//<CONDITIONAL_COMPILATION> {
//    "("                             { pushState(CONDITIONAL_COMPILATION_EXPRESSION); return  processConditional(CONDITIONAL_COMPILATION_EXPRESSION_START); }
//    "!"                             { return processConditional(CC_OPERATOR_NOT); }
//    {WHITE_SPACE}                   { return processConditional(WHITE_SPACE); }
//
//    {LITERAL_INTEGER}               { return evaluateAndPop(processConditional(CC_LITERAL_INTEGER)); }
//    {LITERAL_HEXADECIMAL}           { return evaluateAndPop(processConditional(CC_LITERAL_HEXADECIMAL)); }
//    {LITERAL_OCTAL}                 { return evaluateAndPop(processConditional(CC_LITERAL_OCTAL)); }
//    {LITERAL_FLOAT}                 { return evaluateAndPop(processConditional(CC_LITERAL_FLOAT)); }
//    "true"                          { return evaluateAndPop(processConditional(CC_KEYWORD_TRUE)); }
//    "false"                         { return evaluateAndPop(processConditional(CC_KEYWORD_FALSE)); }
//    {CONDITIONAL_IDENTIFIER}        { return evaluateAndPop(processConditional(CONDITIONAL_ID)); }
//}
<CONDITIONAL_COMPILATION_EXPRESSION> {
      {WHITE_SPACE}                   { return conditionExpressionAppend(WHITE_SPACE); }
      {LITERAL_INTEGER}               { return conditionExpressionAppend(CC_LITERAL_INTEGER); }
      {LITERAL_HEXADECIMAL}           { return conditionExpressionAppend(CC_LITERAL_HEXADECIMAL); }
      {LITERAL_OCTAL}                 { return conditionExpressionAppend(CC_LITERAL_OCTAL); }
      {LITERAL_FLOAT}                 { return conditionExpressionAppend(CC_LITERAL_FLOAT); }
      "true"                          { return conditionExpressionAppend(CC_KEYWORD_TRUE); }
      "false"                         { return conditionExpressionAppend(CC_KEYWORD_FALSE); }

// TODO string for  CC expression


      {CONDITIONAL_IDENTIFIER}        { return conditionExpressionAppend(CONDITIONAL_ID); }


"=="                                      { return conditionExpressionAppend(CC_OPERATOR_EQ); }
"!="                                      { return conditionExpressionAppend(CC_OPERATOR_NOT_EQ); }
"!"                                       { return conditionExpressionAppend(CC_OPERATOR_NOT); }

">="                                      { return conditionExpressionAppend(CC_OPERATOR_GREATER_OR_EQUAL); }
">"                                       { return conditionExpressionAppend(CC_OPERATOR_GREATER); }

"<="                                      { return conditionExpressionAppend(CC_OPERATOR_LESS_OR_EQUAL); }
"<"                                       { return conditionExpressionAppend(CC_OPERATOR_LESS); }

"&&"                                      { return conditionExpressionAppend(CC_OPERATOR_COND_AND); }
"||"                                      { return conditionExpressionAppend(CC_OPERATOR_COND_OR); }

"."                                      { return conditionExpressionAppend(CC_OPERATOR_DOT); }
","                                      { return conditionExpressionAppend(CC_OPERATOR_COMMA); }


"("                                      { leftParenCount++; return conditionExpressionAppend(CC_ENCLOSURE_PARENTHESIS_LEFT); }
")"                                      {
                                              if (leftParenCount == 0) {
                                                return processConditional(CONDITIONAL_COMPILATION_EXPRESSION_END);
                                              }else {
                                                leftParenCount--;
                                                return conditionExpressionAppend(CC_ENCLOSURE_PARENTHESIS_RIGHT);
                                              }
                                         }

//\'                                        { pushState(CC_APOS_STRING); return conditionExpressionAppend( OPEN_QUOTE ); }
//\"                                        { pushState(CC_STRING); return conditionExpressionAppend( OPEN_QUOTE ); }

\"                                         { pushState(CC_QUOTATION_STRING); return processConditional(CC_OPEN_QUOTE);}
\'                                         { pushState(CC_APOSTROPHE_STRING); return processConditional(CC_OPEN_APOSTROPHE);}
//.                                          {return CC_BAD_CHARACTER;}
.                                          { popState();}

}


  [^] { return BAD_CHARACTER; }