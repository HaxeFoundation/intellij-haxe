package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.*;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.lang.lexer.HaxeConditionalCompilationLexerSupport;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.openapi.project.Project;

%%
%{
    static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
    static {      // Take this out when finished debugging.
      LOG.setLevel(org.apache.log4j.Level.DEBUG);
    }

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

    Project context; // Required for conditional compilation support.
    public HaxeConditionalCompilationLexerSupport ccsupport;

    private void pushState(int state) {
        states.push(new State(yystate(), lBraceCount));
        lBraceCount = 0;
        yybegin(state);
    }

    private String getStateName(int state) {
        if(state == SHORT_TEMPLATE_ENTRY) {
          return "SHORT_TEMPLATE_ENTRY";
        }
        if(state == LONG_TEMPLATE_ENTRY) {
          return "LONG_TEMPLATE_ENTRY";
        }
        if(state == QUO_STRING) {
          return "QUO_STRING";
        }
        if(state == APOS_STRING) {
          return "APOS_STRING";
        }
        if(state == COMPILER_CONDITIONAL) {
          return "COMPILER_CONDITIONAL";
        }
        if(state == CC_STRING) {
          return "CC_STRING";
        }
        if(state == CC_APOS_STRING) {
          return "CC_APOS_STRING";
        }
        if(state == CC_BLOCK) {
          return "CC_BLOCK";
        }
        return null;
    }

    private void popState() {
        State state = states.pop();
        lBraceCount = state.lBraceCount;
        yybegin(state.state);
    }

    /** Map output within conditional blocks to comments if the condition is false. */
    private IElementType emitToken(IElementType tokenType) {
        if (ccsupport.currentContextIsActive()) {
           return tokenType;
        } else {
            return ccsupport.mapToken(tokenType);
        }
    }

    /** Deal with compiler conditional block constructs (e.g. #if...#end). */
    private IElementType processConditional(IElementType type) {
        ccsupport.processConditional(yytext(), type);

        if (PPIF.equals(type)) {
            ccStart();
        } else if (PPEND.equals(type)) {
            ccEnd();
        } else if (zzLexicalState != CC_BLOCK) {
            // Maybe the #if is missing, but if we're not at the end, we want to be sure that we're
            // in the conditional state.
            LOG.debug("Unexpected lexical state. Missing starting #if?");
            ccStart();
        }

        if (PPIF.equals(type) || PPELSEIF.equals(type)) {
            conditionStart();
        }
        return type;
    }

    // These deal with the state of lexing the *condition* for compiler conditionals
    private void conditionStart() { pushState(COMPILER_CONDITIONAL); ccsupport.conditionStart(); }
    private boolean conditionIsComplete() { return ccsupport.conditionIsComplete(); }
    private IElementType conditionAppend(IElementType type) {
        ccsupport.conditionAppend(yytext(),type);
        if (ccsupport.conditionIsComplete()) {
            conditionEnd();
        }
        return PPEXPRESSION;
    }
    private void conditionEnd() {
        ccsupport.conditionEnd();
        popState();
    }

    // We use the CC_BLOCK state to tell the highlighters, etc. that their context
    // has to go back to the start of the conditional (even though that may be a ways).  Basically,
    // we need to keep the state as something other than YYINITIAL.
    private void ccStart() { pushState(CC_BLOCK); } // Until we know better
    private void ccEnd() {
        // When there is no #if, but there is an end, popping the state produces an EmptyStackException
        // and messes up further processing.
        if (zzLexicalState == CC_BLOCK) {
            popState();
        }
    }

    // There are two other constructors generated for us.  This is the only one that is actually used.
    public _HaxeLexer(Project context) {
      this((java.io.Reader)null);
      this.context = context;
      ccsupport = new HaxeConditionalCompilationLexerSupport(context);
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

%xstate QUO_STRING APOS_STRING SHORT_TEMPLATE_ENTRY LONG_TEMPLATE_ENTRY COMPILER_CONDITIONAL CC_STRING CC_APOS_STRING CC_BLOCK

WHITE_SPACE_CHAR=[\ \n\r\t\f]
//WHITE_SPACE={WHITE_SPACE_CHAR}+

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

/*
    Compiler conditionals: e.g. "#if (js)...#else...#endif"
    "macro", "this", and "null" are all identifiers as far as CC is concerned.
 */
CONDITIONAL_IDENTIFIER={IDENTIFIER_NO_DOLLAR}

// Treat #line and #error as end of line comments
CONDITIONAL_LINE="#line"[^\r\n]*
CONDITIONAL_ERROR="#error"[^\r\n]*

%%

<YYINITIAL, CC_BLOCK> "{"                 { return emitToken( PLCURLY); }
<YYINITIAL, CC_BLOCK> "}"                 { return emitToken( PRCURLY); }
<LONG_TEMPLATE_ENTRY> "{"                 { lBraceCount++; return emitToken( PLCURLY); }
<LONG_TEMPLATE_ENTRY> "}"                 {
                                              if (lBraceCount == 0) {
                                                popState();
                                                return emitToken( LONG_TEMPLATE_ENTRY_END);
                                              }
                                              lBraceCount--;
                                              return emitToken( PRCURLY);
                                          }

<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY> {

{WHITE_SPACE_CHAR}+                       { return emitToken( com.intellij.psi.TokenType.WHITE_SPACE);}

{CONDITIONAL_LINE}                        { return emitToken( MSL_COMMENT); }
{CONDITIONAL_ERROR}                       { return emitToken( MSL_COMMENT); }
{END_OF_LINE_COMMENT}                     { return emitToken( MSL_COMMENT); }
{C_STYLE_COMMENT}                         { return emitToken( MML_COMMENT); }
{DOC_COMMENT}                             { return emitToken( DOC_COMMENT); }

"..."                                     { return emitToken( OTRIPLE_DOT); }

{mNUM_FLOAT} / [^"."]                     {  return emitToken( LITFLOAT); }
{mNUM_OCT}                                {  return emitToken( LITOCT); }
{mNUM_HEX}                                {  return emitToken( LITHEX); }
{mNUM_INT}                                {  return emitToken( LITINT); }
{mREG_EXP}                                {  return emitToken( REG_EXP); }

"new"                                     { return emitToken( ONEW); }
"in"                                      { return emitToken( OIN); }

"break"                                   { return emitToken( KBREAK);  }
"default"                                 { return emitToken( KDEFAULT);  }
"package"                                 { return emitToken( KPACKAGE);  }
"function"                                { return emitToken( KFUNCTION);  }

"case"                                    { return emitToken( KCASE);  }
"cast"                                    { return emitToken( KCAST);  }

"abstract"                                {  return emitToken( KABSTRACT);  }
//"from"                                    {  return emitToken( KFROM);  }
//"to"                                      {  return emitToken( KTO );  }

"class"                                   {  return emitToken( KCLASS);  }
"enum"                                    {  return emitToken( KENUM);  }
"interface"                               {  return emitToken( KINTERFACE);  }

"implements"                              {  return emitToken( KIMPLEMENTS);  }
"extends"                                 {  return emitToken( KEXTENDS);  }

"if"                                      {  return emitToken( KIF );  }
"null"                                    {  return emitToken( KNULL );  }
"true"                                    {  return emitToken( KTRUE );  }
"false"                                   {  return emitToken( KFALSE );  }
"this"                                    {  return emitToken( KTHIS );  }
"super"                                   {  return emitToken( KSUPER );  }

"for"                                     {  return emitToken( KFOR );  }
"do"                                      {  return emitToken( KDO );  }
"while"                                   {  return emitToken( KWHILE );  }
"return"                                  {  return emitToken( KRETURN );  }
"import"                                  {  return emitToken( KIMPORT );  }
"using"                                   {  return emitToken( KUSING );  }
"continue"                                {  return emitToken( KCONTINUE );  }
"else"                                    {  return emitToken( KELSE );  }
"switch"                                  {  return emitToken( KSWITCH );  }
"throw"                                   {  return emitToken( KTHROW );  }

"var"                                     {  return emitToken( KVAR);  }
"public"                                  {  return emitToken( KPUBLIC);  }
"private"                                 {  return emitToken( KPRIVATE);  }
"static"                                  {  return emitToken( KSTATIC);  }
"dynamic"                                 {  return emitToken( KDYNAMIC);  }
"never"                                   {  return emitToken( KNEVER);  }
"override"                                {  return emitToken( KOVERRIDE);  }
"inline"                                  {  return emitToken( KINLINE);  }
"macro" [\ ]+                                   {  return emitToken( KMACRO2); }

"untyped"                                 {  return emitToken( KUNTYPED);  }
"typedef"                                 {  return emitToken( KTYPEDEF);  }

"extern" [\ ]+                                  {  return emitToken( KEXTERN);  }

"@:final"                                 {  return emitToken( KFINAL);  }
"@:hack"                                  {  return emitToken( KHACK);  }
"@:native"                                {  return emitToken( KNATIVE);  }
"@:macro"                                 {  return emitToken( KMACRO);  }
"@:build"                                 {  return emitToken( KBUILD);  }
"@:autoBuild"                             {  return emitToken( KAUTOBUILD);  }
"@:keep"                                  {  return emitToken( KKEEP);  }
"@:require"                               {  return emitToken( KREQUIRE);  }
"@:fakeEnum"                              {  return emitToken( KFAKEENUM);  }
"@:core_api"                              {  return emitToken( KCOREAPI);  }

"@:bind"                                  {  return emitToken( KBIND);  }
"@:bitmap"                                {  return emitToken( KBITMAP);  }
"@:ns"                                    {  return emitToken( KNS);  }
"@:protected"                             {  return emitToken( KPROTECTED);  }
"@:getter"                                {  return emitToken( KGETTER);  }
"@:setter"                                {  return emitToken( KSETTER);  }
"@:debug"                                 {  return emitToken( KDEBUG);  }
"@:nodebug"                               {  return emitToken( KNODEBUG);  }
"@:meta"                                  {  return emitToken( KMETA);  }
"@:overload"                              {  return emitToken( KOVERLOAD);  }

"try"                                     {  return emitToken( KTRY);  }
"catch"                                   {  return emitToken( KCATCH);  }


{MACRO_IDENTIFIER}                        {  return emitToken( MACRO_ID); }
{IDENTIFIER}                              {  return emitToken( ID); }

"."                                       { return emitToken( ODOT); }

"["                                       { return emitToken( PLBRACK); }
"]"                                       { return emitToken( PRBRACK); }

"("                                       { return emitToken( PLPAREN); }
")"                                       { return emitToken( PRPAREN); }

":"                                       { return emitToken( OCOLON); }
";"                                       { return emitToken( OSEMI); }
","                                       { return emitToken( OCOMMA); }

"->"                                      { return emitToken( OARROW); }

"=="                                      { return emitToken( OEQ); }
"="                                       { return emitToken( OASSIGN); }

"!="                                      { return emitToken( ONOT_EQ); }
"!"                                       { return emitToken( ONOT); }
"~" / [^"/"]                              { return emitToken( OCOMPLEMENT); }

"?"                                       { return emitToken( OQUEST);}

"++"                                      { return emitToken( OPLUS_PLUS); }
"+="                                      { return emitToken( OPLUS_ASSIGN); }
"+"                                       { return emitToken( OPLUS); }

"--"                                      { return emitToken( OMINUS_MINUS); }
"-="                                      { return emitToken( OMINUS_ASSIGN); }
"-"                                       { return emitToken( OMINUS); }

"||"                                      { return emitToken( OCOND_OR); }
"|="                                      { return emitToken( OBIT_OR_ASSIGN); }
"|"                                       { return emitToken( OBIT_OR); }

"&&"                                      { return emitToken( OCOND_AND); }
"&="                                      { return emitToken( OBIT_AND_ASSIGN); }
"&"                                       { return emitToken( OBIT_AND); }

"<<="                                     { return emitToken( OSHIFT_LEFT_ASSIGN); }
"<<"                                      { return emitToken( OSHIFT_LEFT); }
"<="                                      { return emitToken( OLESS_OR_EQUAL); }
"<"                                       { return emitToken( OLESS); }

"^="                                      { return emitToken( OBIT_XOR_ASSIGN); }
"^"                                       { return emitToken( OBIT_XOR); }

"*="                                      { return emitToken( OMUL_ASSIGN); }
"*"                                       { return emitToken( OMUL); }

"/="                                      { return emitToken( OQUOTIENT_ASSIGN); }
"/"                                       { return emitToken( OQUOTIENT); }

"%="                                      { return emitToken( OREMAINDER_ASSIGN); }
"%"                                       { return emitToken( OREMAINDER); }

//">>>="                                    { return emitToken( OUNSIGNED_SHIFT_RIGHT_ASSIGN); }
//">>="                                     { return emitToken( OSHIFT_RIGHT_ASSIGN); }
//">="                                      { return emitToken( OGREATER_OR_EQUAL); }
"=>"                                      { return emitToken( OFAT_ARROW); }
">"                                       { return emitToken( OGREATER); }

".*"                                      { return emitToken( TWILDCARD); }

//{CONDITIONAL_IF} | {CONDITIONAL_ELSEIF}                          { return emitToken( CONDITIONAL_STATEMENT_ID); }
"#end"                                    { return processConditional(PPEND); }
"#elseif"                                 { return processConditional(PPELSEIF); }
"#else"                                   { return processConditional(PPELSE); }
"#if"                                     { return processConditional(PPIF); }
} // <YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY>

// "

<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY> \"   { pushState(QUO_STRING); return emitToken( OPEN_QUOTE); }
<QUO_STRING> \"                 { popState(); return emitToken( CLOSING_QUOTE); }
<QUO_STRING> {ESCAPE_SEQUENCE}  { return emitToken( REGULAR_STRING_PART); }

<QUO_STRING> {REGULAR_QUO_STRING_PART}     { return emitToken( REGULAR_STRING_PART); }
<QUO_STRING> {SHORT_TEMPLATE_ENTRY}        {
                                                                  pushState(SHORT_TEMPLATE_ENTRY);
                                                                  yypushback(yylength() - 1);
                                                                  return emitToken( SHORT_TEMPLATE_ENTRY_START);
                                                             }

<QUO_STRING> {LONELY_DOLLAR}               { return emitToken( REGULAR_STRING_PART); }
<QUO_STRING> {LONG_TEMPLATE_ENTRY_START}   { pushState(LONG_TEMPLATE_ENTRY); return emitToken( LONG_TEMPLATE_ENTRY_START); }

// Support single quote strings: "'"

<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY> \'     { pushState(APOS_STRING); return emitToken( OPEN_QUOTE); }
<APOS_STRING> \'                 { popState(); return emitToken( CLOSING_QUOTE); }
<APOS_STRING> {ESCAPE_SEQUENCE}  { return emitToken( REGULAR_STRING_PART); }

<APOS_STRING> {REGULAR_APOS_STRING_PART}    { return emitToken( REGULAR_STRING_PART); }
<APOS_STRING> {SHORT_TEMPLATE_ENTRY}        {
                                                                  pushState(SHORT_TEMPLATE_ENTRY);
                                                                  yypushback(yylength() - 1);
                                                                  return emitToken( SHORT_TEMPLATE_ENTRY_START);
                                                             }

<APOS_STRING> {LONELY_DOLLAR}               { return emitToken( REGULAR_STRING_PART); }
<APOS_STRING> {LONG_TEMPLATE_ENTRY_START}   { pushState(LONG_TEMPLATE_ENTRY); return emitToken( LONG_TEMPLATE_ENTRY_START); }


// Only *this* keyword is itself an expression valid in this position
// *null*, *true* and *false* are also keywords and expression, but it does not make sense to put them
// in a string template for it'd be easier to just type them in without a dollar
<SHORT_TEMPLATE_ENTRY> "this"          { popState(); return emitToken( KTHIS); }
<SHORT_TEMPLATE_ENTRY> {IDENTIFIER_NO_DOLLAR}    { popState(); return emitToken( ID); }

// Parses the *condition* in a compiler conditional construct (e.g. #if <condition> ...)
<COMPILER_CONDITIONAL> {

{WHITE_SPACE_CHAR}+                       { return conditionAppend(com.intellij.psi.TokenType.WHITE_SPACE);}

"true"                                    { return conditionAppend( KTRUE ); }
"false"                                   { return conditionAppend( KFALSE ); }

{CONDITIONAL_IDENTIFIER}                  { return conditionAppend( ID ); }

{mNUM_FLOAT} / [^"."]                     { return conditionAppend( LITFLOAT ); }
{mNUM_OCT}                                { return conditionAppend( LITOCT ); }
{mNUM_HEX}                                { return conditionAppend( LITHEX ); }
{mNUM_INT}                                { return conditionAppend( LITINT ); }

"("                                       { return conditionAppend( PLPAREN ); }
")"                                       { return conditionAppend( PRPAREN ); }

"=="                                      { return conditionAppend( OEQ ); }
"!="                                      { return conditionAppend( ONOT_EQ ); }
"!"                                       { return conditionAppend( ONOT ); }

">="                                      { return conditionAppend( OGREATER_OR_EQUAL ); }
">"                                       { return conditionAppend( OGREATER ); }

"<="                                      { return conditionAppend( OLESS_OR_EQUAL ); }
"<"                                       { return conditionAppend( OLESS ); }

"&&"                                      { return conditionAppend( OCOND_AND ); }
"||"                                      { return conditionAppend( OCOND_OR ); }

\'                                        { pushState(CC_APOS_STRING); return conditionAppend( OPEN_QUOTE ); }
\"                                        { pushState(CC_STRING); return conditionAppend( OPEN_QUOTE ); }


// Any other token is an error which needs to kill this state and be processed normally.
.                                         {
                                            LOG.debug("Bad termination of PP condition: \"" + yytext() + "\"");
                                            yypushback(1);
                                            conditionEnd();
                                            return PPBODY;
                                          }
}

// Strings inside of compiler conditionals.  They can't use string interpolation/templates (e.g. $var).
<CC_STRING> {
\"                                        { popState(); return conditionAppend( CLOSING_QUOTE ); }
{ESCAPE_SEQUENCE}                         { return conditionAppend( REGULAR_STRING_PART ); }
{REGULAR_QUO_STRING_PART}                 { return conditionAppend( REGULAR_STRING_PART ); }
}
<CC_APOS_STRING> {
\'                                        { popState(); return conditionAppend( CLOSING_QUOTE ); }
{ESCAPE_SEQUENCE}                         { return conditionAppend( REGULAR_STRING_PART ); }
{REGULAR_APOS_STRING_PART}                { return conditionAppend( REGULAR_STRING_PART ); }
}

<QUO_STRING, APOS_STRING, SHORT_TEMPLATE_ENTRY, LONG_TEMPLATE_ENTRY, CC_BLOCK> .  { return emitToken( com.intellij.psi.TokenType.BAD_CHARACTER ); }
.                                                                       {
                                                                          yybegin(YYINITIAL);
                                                                          return emitToken( com.intellij.psi.TokenType.BAD_CHARACTER );
                                                                        }
