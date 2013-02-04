package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.*;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;

%%
%{
    private static final class State {
        final int lBraceCount;
        final int state;

        public State(int state, int lBraceCount) {
            this.state = state;
            this.lBraceCount = lBraceCount;
        }

        @Override
        public String toString() {
            return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
        }
    }

    private final Stack<State> states = new Stack<State>();
    private int lBraceCount;

    private int commentStart;
    private int commentDepth;

    private void pushState(int state) {
        states.push(new State(yystate(), lBraceCount));
        lBraceCount = 0;
        yybegin(state);
    }

    private void popState() {
        State state = states.pop();
        lBraceCount = state.lBraceCount;
        yybegin(state.state);
    }

    public _HaxeLexer() {
      this((java.io.Reader)null);
    }
%}

%unicode
%class _HaxeLexer
%implements FlexLexer, HaxeTokenTypes, HaxeTokenTypeSets
%unicode
%public

%function advance
%type IElementType

%eof{
%eof}

%xstate QUO_STRING APOS_STRING SHORT_TEMPLATE_ENTRY LONG_TEMPLATE_ENTRY

WHITE_SPACE_CHAR=[\ \n\r\t\f]

mLETTER = [:letter:] | "_"
mDIGIT = [:digit:]
ESCAPE_SEQUENCE=\\[^\r\n]

IDENTIFIER="$"? {mLETTER} ({mDIGIT} | {mLETTER})*
MACRO_IDENTIFIER="@" ":"? {IDENTIFIER}

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

mREG_EXP = "~/" ([^"/"] | {ESCAPE_SEQUENCE})* "/" [igmsu]*

mFLOAT_EXPONENT = [eE] [+-]? {mDIGIT}+
mNUM_FLOAT = ( (({mDIGIT}* "." {mDIGIT}+) | ({mDIGIT}+ "." {mDIGIT}*)) {mFLOAT_EXPONENT}?) | ({mDIGIT}+ {mFLOAT_EXPONENT})

/*
    Strings with templates
*/

REGULAR_QUO_STRING_PART=[^\\\"\$]+
REGULAR_APOS_STRING_PART=[^\\\'\$]+
SHORT_TEMPLATE_ENTRY=\${IDENTIFIER_NO_DOLLAR}
LONELY_DOLLAR=\$
LONG_TEMPLATE_ENTRY_START=\$\{

IDENTIFIER_START_NO_DOLLAR={mLETTER}|"_"
IDENTIFIER_START={IDENTIFIER_START_NO_DOLLAR}|"$"
IDENTIFIER_PART_NO_DOLLAR={IDENTIFIER_START_NO_DOLLAR}|{mDIGIT}
IDENTIFIER_PART={IDENTIFIER_START}|{mDIGIT}
IDENTIFIER={IDENTIFIER_START}{IDENTIFIER_PART}*
IDENTIFIER_NO_DOLLAR={IDENTIFIER_START_NO_DOLLAR}{IDENTIFIER_PART_NO_DOLLAR}*

%%

<YYINITIAL> "{"                           { return PLCURLY; }
<YYINITIAL> "}"                           { return PRCURLY; }
<LONG_TEMPLATE_ENTRY> "{"                 { lBraceCount++; return PLCURLY; }
<LONG_TEMPLATE_ENTRY> "}"                 {
                                              if (lBraceCount == 0) {
                                                popState();
                                                return LONG_TEMPLATE_ENTRY_END;
                                              }
                                              lBraceCount--;
                                              return PRCURLY;
                                          }

<YYINITIAL, LONG_TEMPLATE_ENTRY> {

{WHITE_SPACE_CHAR}+                       { return com.intellij.psi.TokenType.WHITE_SPACE;}

{END_OF_LINE_COMMENT}                     { return MSL_COMMENT; }
{C_STYLE_COMMENT}                         { return MML_COMMENT; }
{DOC_COMMENT}                             { return DOC_COMMENT; }

"..."                                     { return OTRIPLE_DOT; }

{mNUM_FLOAT} / [^"."]                     {  return LITFLOAT; }
{mNUM_OCT}                                {  return LITOCT; }
{mNUM_HEX}                                {  return LITHEX; }
{mNUM_INT}                                {  return LITINT; }
{mREG_EXP}                                {  return REG_EXP; }

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
"true"                                    {  return KTRUE ;  }
"false"                                   {  return KFALSE ;  }
"this"                                    {  return KTHIS ;  }
"super"                                   {  return KSUPER ;  }

"for"                                     {  return KFOR ;  }
"do"                                      {  return KDO ;  }
"while"                                   {  return KWHILE ;  }
"return"                                  {  return KRETURN ;  }
"import"                                  {  return KIMPORT ;  }
"using"                                   {  return KUSING ;  }
"continue"                                {  return KCONTINUE ;  }
"else"                                    {  return KELSE ;  }
"switch"                                  {  return KSWITCH ;  }
"throw"                                   {  return KTHROW ;  }

"var"                                     {  return KVAR;  }
"public"                                  {  return KPUBLIC;  }
"private"                                 {  return KPRIVATE;  }
"static"                                  {  return KSTATIC;  }
"dynamic"                                 {  return KDYNAMIC;  }
"never"                                   {  return KNEVER;  }
"override"                                {  return KOVERRIDE;  }
"inline"                                  {  return KINLINE;  }

"untyped"                                 {  return KUNTYPED;  }
"typedef"                                 {  return KTYPEDEF;  }

"extern"                                  {  return KEXTERN;  }

"@:final"                                 {  return KFINAL;  }
"@:hack"                                  {  return KHACK;  }
"@:native"                                {  return KNATIVE;  }
"@:macro"                                 {  return KMACRO;  }
"@:build"                                 {  return KBUILD;  }
"@:autoBuild"                             {  return KAUTOBUILD;  }
"@:keep"                                  {  return KKEEP;  }
"@:require"                               {  return KREQUIRE;  }
"@:fakeEnum"                              {  return KFAKEENUM;  }
"@:core_api"                              {  return KCOREAPI;  }

"@:bind"                                  {  return KBIND;  }
"@:bitmap"                                {  return KBITMAP;  }
"@:ns"                                    {  return KNS;  }
"@:protected"                             {  return KPROTECTED;  }
"@:getter"                                {  return KGETTER;  }
"@:setter"                                {  return KSETTER;  }
"@:debug"                                 {  return KDEBUG;  }
"@:nodebug"                               {  return KNODEBUG;  }
"@:meta"                                  {  return KMETA;  }
"@:overload"                              {  return KOVERLOAD;  }

"try"                                     {  return KTRY;  }
"catch"                                   {  return KCATCH;  }


{MACRO_IDENTIFIER}                        {  return MACRO_ID; }
{IDENTIFIER}                              {  return ID; }

"."                                       { return ODOT; }

"["                                       { return PLBRACK; }
"]"                                       { return PRBRACK; }

"("                                       { return PLPAREN; }
")"                                       { return PRPAREN; }

":"                                       { return OCOLON; }
";"                                       { return OSEMI; }
","                                       { return OCOMMA; }

"->"                                      { return OARROW; }

"=="                                      { return OEQ; }
"="                                       { return OASSIGN; }

"!="                                      { return ONOT_EQ; }
"!"                                       { return ONOT; }
"~" / [^"/"]                              { return OCOMPLEMENT; }

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

">>="                                     { return OSHIFT_RIGHT_ASSIGN; }
">="                                      { return OGREATER_OR_EQUAL; }
">"                                       { return OGREATER; }

"#if"                                     { return PPIF; }
"#end"                                    { return PPEND; }
"#error"                                  { return PPERROR; }
"#elseif"                                 { return PPELSEIF; }
"#else"                                   { return PPELSE; }
}

// "

<YYINITIAL, LONG_TEMPLATE_ENTRY> \"       { pushState(QUO_STRING); return OPEN_QUOTE; }
<QUO_STRING> \"                 { popState(); return CLOSING_QUOTE; }
<QUO_STRING> {ESCAPE_SEQUENCE}  { return REGULAR_STRING_PART; }

<QUO_STRING> {REGULAR_QUO_STRING_PART}     { return REGULAR_STRING_PART; }
<QUO_STRING> {SHORT_TEMPLATE_ENTRY}        {
                                                                  pushState(SHORT_TEMPLATE_ENTRY);
                                                                  yypushback(yylength() - 1);
                                                                  return SHORT_TEMPLATE_ENTRY_START;
                                                             }

<QUO_STRING> {LONELY_DOLLAR}               { return REGULAR_STRING_PART; }
<QUO_STRING> {LONG_TEMPLATE_ENTRY_START}   { pushState(LONG_TEMPLATE_ENTRY); return LONG_TEMPLATE_ENTRY_START; }

// '

<YYINITIAL, LONG_TEMPLATE_ENTRY> \'     { pushState(APOS_STRING); return OPEN_QUOTE; }
<APOS_STRING> \'                 { popState(); return CLOSING_QUOTE; }
<APOS_STRING> {ESCAPE_SEQUENCE}  { return REGULAR_STRING_PART; }

<APOS_STRING> {REGULAR_APOS_STRING_PART}    { return REGULAR_STRING_PART; }
<APOS_STRING> {SHORT_TEMPLATE_ENTRY}        {
                                                                  pushState(SHORT_TEMPLATE_ENTRY);
                                                                  yypushback(yylength() - 1);
                                                                  return SHORT_TEMPLATE_ENTRY_START;
                                                             }

<APOS_STRING> {LONELY_DOLLAR}               { return REGULAR_STRING_PART; }
<APOS_STRING> {LONG_TEMPLATE_ENTRY_START}   { pushState(LONG_TEMPLATE_ENTRY); return LONG_TEMPLATE_ENTRY_START; }


// Only *this* keyword is itself an expression valid in this position
// *null*, *true* and *false* are also keywords and expression, but it does not make sense to put them
// in a string template for it'd be easier to just type them in without a dollar
<SHORT_TEMPLATE_ENTRY> "this"          { popState(); return KTHIS; }
<SHORT_TEMPLATE_ENTRY> {IDENTIFIER_NO_DOLLAR}    { popState(); return ID; }

.                                         {  yybegin(YYINITIAL); return com.intellij.psi.TokenType.BAD_CHARACTER; }