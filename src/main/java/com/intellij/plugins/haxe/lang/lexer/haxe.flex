package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.*;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.lang.lexer.HaxeConditionalCompilationLexerSupport;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.LogLevel;

%%
%{
    static final Logger log = com.intellij.openapi.diagnostic.Logger.getInstance(_HaxeLexer.class);
         static {      // Take this out when finished debugging.
             log.setLevel(LogLevel.DEBUG);
         }

        private static final class State {
            final int lBraceCount;
            final int lParenCount;
            final int state;

            public State(int state, int lBraceCount, int lParenCount) {
                this.state = state;
                this.lBraceCount = lBraceCount;
                this.lParenCount = lParenCount;
            }

            @Override
            public String toString() {
                return "yystate = " + state + (lBraceCount == 0 ? "" : " lBraceCount = " + lBraceCount)
                                            + (lParenCount == 0 ? "" : " lParenCount = " + lParenCount);
            }
        }

        protected final Stack<State> states = new Stack<State>();

        private int lBraceCount;
        private int lParenCount;

        private int commentStart;
        private int commentDepth;

        /**
         * Tracks an in-progress  inline XML/markup literal (ex. `<xml>...</xml>` or `<xml/>`.)
         * This is an attempt at mirroring how the Haxe compiler handles xml literals and keep track of depth
         * and nested occurrences of the same open tag.
         *
         *
         * inOpenTag tracks whether we're still positioned right after an (outer or same-name) opening tag
         * such that a following "/>" should count as that tag self-closing.
         *
         * we want to keep track of depth and open state due to how permissive the haxe compiler is.
         * Stuff like `<xml a=" </xml>` (yes theres no ">" for the open tag) is a perfectly valid xml
         * literal when parsed by the compiler.
         *
         * i (m0rkeulv) have tried a few different solutions for parsing XML literals, amongst other handling this
         * in the grammar (BNF) with some special parser function, but due to the complexity and many rollback
         * code paths, i have concluded that its probably better and faster to handle this in the lexer with some
         * guessing/ makeing sure we only start an XML literal when expected.
         *
         * see `isExpressionExpected` below.
         *
         */
        private static final class XmlContext {
            final String openTag;
            final String closeTag;
            int depth;
            boolean inOpenTag;

            XmlContext(String openTag, String closeTag, boolean inOpenTag) {
                this.openTag = openTag;
                this.closeTag = closeTag;
                this.depth = 0;
                this.inOpenTag = inOpenTag;
            }
        }
        protected final Stack<XmlContext> xmlContexts = new Stack<XmlContext>();

        // Last non-whitespace/comment token emitted; used by isExpressionExpected() below.
        protected IElementType lastSignificantToken;

        /**
         * This is an best (guess) effort to only start lexing as XML literal tokens when we do not expect
         * normal tokens (less-than, shift or generic-parameter) or conditional compilation tokens.
         *
         * According to AI (i dont know Ocaml enough to check this my self)
         * The Haxe compiler parser triggers markup-literal lexing whenever it is about to parse
         * an expression and the next token is '<' -- there is no other expression production starting
         * with '<', so that is unambiguous once you know the parser's grammar position.
         *
         * Our lexer on the other hand runs ahead of and independently from our parser,
         * so we need to approximate/guess when an an expression is expected.
         * we do this by checking the last significant token (not whitespace or comment)
         *
         * if the lastSignificantToken comes after something that completes a valueExpression
         * (identifier, literal, closing bracket, this/super/null/true/false, etc.) we treat '<' as a normal operator.
         *
         * if lastSignificantToken is start of file, block, after an operator, a keyword, or seperators/operators
         * '(', ',', ';', '=', etc.) we expect it to be a xml expression.
         * This should be compatible with existing parsing for generics and comparisons
         * since those are always preceded by an identifier or value (e.g. "Array<Int>", "a < b").
         *
         * NOTE: Its is not unikely that  there are cases that i have missed and that needs to be fixed.
         * (see `com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.VALUE_COMPLETING_TOKENS`)
         */
        private boolean isExpressionExpected() {
            return lastSignificantToken == null || !VALUE_COMPLETING_TOKENS.contains(lastSignificantToken);
        }

        Project context; // Required for conditional compilation support.
        public HaxeConditionalCompilationLexerSupport ccsupport;

        private void pushState(int state) {
            states.push(new State(yystate(), lBraceCount, lParenCount));
            lBraceCount = 0;
            lParenCount = 0;
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
            if(state == METADATA) {
              return "METADATA";
            }
            if(state == XML_CONTENT) {
              return "XML_CONTENT";
            }
            return null;
        }

        private void popState() {
            State state = states.pop();
            lBraceCount = state.lBraceCount;
            lParenCount = state.lParenCount;
            yybegin(state.state);
        }

        /** Map output within conditional blocks to comments if the condition is false. */
        private IElementType emitToken(IElementType tokenType) {
            if (ccsupport.currentContextIsActive()) {
               if (tokenType != null && !WHITESPACES.contains(tokenType) && !COMMENTS.contains(tokenType)) {
                   lastSignificantToken = tokenType;
               }
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
                log.debug("Unexpected lexical state. Missing starting #if?");
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
%public

%function advance
%type IElementType

%eof{
%eof}

%xstate QUO_STRING APOS_STRING SHORT_TEMPLATE_ENTRY LONG_TEMPLATE_ENTRY COMPILER_CONDITIONAL CC_STRING CC_APOS_STRING CC_BLOCK METADATA XML_CONTENT

WHITE_SPACE_CHAR=[\ \n\r\t\f]
//WHITE_SPACE={WHITE_SPACE_CHAR}+

mLETTER = [:letter:] | "_"
mDIGIT = [:digit:]

// allowed escapes: https://haxe.org/manual/std-String-literals.html
STRING_ESCAPE_SINGLE_CHAR=\\(t|n|r|\"|'|\\)
STRING_ESCAPE_OCTETS=\\({mOCT_DIGIT}){3}
STRING_ESCAPE_HEX=\\x({mHEX_DIGIT}){2}
STRING_ESCAPE_UNICODE_FIXED=\\u({mHEX_DIGIT}){4}
STRING_ESCAPE_UNICODE_VARIABLE=\\u\{({mHEX_DIGIT}){1,6}\}

STRING_ESCAPE_PART= {STRING_ESCAPE_SINGLE_CHAR} | {STRING_ESCAPE_OCTETS} | {STRING_ESCAPE_HEX} | {STRING_ESCAPE_UNICODE_FIXED} | {STRING_ESCAPE_UNICODE_VARIABLE}
STRING_INVALID_ESCAPE =\\.




mMETA_PART = {mLETTER} ({mDIGIT} | {mLETTER})*
META_ID =  ({mMETA_PART} ("." {mMETA_PART})*)?
COMPILE_META_PREFIX="@:"
RUNTIME_META_PREFIX="@"
META=({RUNTIME_META_PREFIX} | {COMPILE_META_PREFIX}) {META_ID}
META_WITH_ARGS={META} "("
META_WITH_ARGS_END=")"

C_STYLE_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
END_OF_LINE_COMMENT="/""/"[^\r\n]*

mHEX_DIGIT = [0-9A-Fa-f]
mINT_DIGIT = [0-9]
mOCT_DIGIT = [0-7]
mBIN_DIGIT = [0-1]

// handles digit separation with underscore ("_")
mHEX_DIGIT_TAIL =((_|{mHEX_DIGIT})*{mHEX_DIGIT})
mINT_DIGIT_TAIL =((_|{mINT_DIGIT})*{mINT_DIGIT})
mBIN_DIGIT_TAIL =((_|{mBIN_DIGIT})*{mBIN_DIGIT})


mNUM_INT = "0"   | ([1-9] {mINT_DIGIT_TAIL}*)
mNUM_HEX = ("0x" | "0X") {mHEX_DIGIT} {mHEX_DIGIT_TAIL}*
mNUM_BIN = ("0b" | "0B") {mBIN_DIGIT} {mBIN_DIGIT_TAIL}*

// TODOMLO: NOT SURE IF HAXE ACTUALLY SUPPORTS OCTAL NUMBERS
mNUM_OCT = "0" {mOCT_DIGIT}+
ESCAPE_SEQUENCE=\\[^\r\n]
mREG_EXP = "~/" ([^"/"] | {ESCAPE_SEQUENCE})* "/" [igmsu]*

mBFLOAT_DIGIT_TAIL =((_|{mDIGIT})*{mDIGIT})
mFLOAT_DIGITS = {mDIGIT} {mBFLOAT_DIGIT_TAIL}*

mFLOAT_EXPONENT = [eE] [+-]? {mFLOAT_DIGITS}+
mNUM_FLOAT = ( (({mFLOAT_DIGITS}? "." {mFLOAT_DIGITS}) | ({mFLOAT_DIGITS} "." {mFLOAT_DIGITS}?)) {mFLOAT_EXPONENT}?) | ({mFLOAT_DIGITS} {mFLOAT_EXPONENT})


/*
    Strings with templates
*/

REGULAR_QUO_STRING_PART=[^\\\"\$]+
REGULAR_APOS_STRING_PART=[^\\\'\$]+
SHORT_TEMPLATE_ENTRY=\${IDENTIFIER_NO_DOLLAR}
LONELY_DOLLAR=\$
DOUBLE_DOLLAR=\$\$
LONG_TEMPLATE_ENTRY_START=\$\{

IDENTIFIER_START={mLETTER}|"_"
IDENTIFIER_PART={IDENTIFIER_START}|{mDIGIT}

IDENTIFIER_NO_DOLLAR={IDENTIFIER_START}{IDENTIFIER_PART}*
IDENTIFIER_WITH__DOLLAR="$"{IDENTIFIER_START}{IDENTIFIER_PART}*

/*
    Haxe inline XML/markup literal tag names.
    Normal xml tag naming plus '$' (also allowed by the Haxe compiler).
*/
XML_NAME_START_CHAR = ({mLETTER} | [$:])
XML_NAME_CHAR = ({XML_NAME_START_CHAR} | {mDIGIT} | [.\-])
XML_NAME = ({XML_NAME_START_CHAR}{XML_NAME_CHAR}*)

/*
    Compiler conditionals: e.g. "#if (js)...#else...#endif"
    "macro", "this", and "null" are all identifiers as far as CC is concerned.
 */
CONDITIONAL_IDENTIFIER={IDENTIFIER_NO_DOLLAR} ("." {IDENTIFIER_NO_DOLLAR})*

// Treat #line and #error as end of line comments
CONDITIONAL_LINE="#line"[^\r\n]*
CONDITIONAL_ERROR="#error"[^\r\n]*

%%

<YYINITIAL, CC_BLOCK, METADATA> "{"       { return emitToken( PLCURLY); }
<YYINITIAL, CC_BLOCK, METADATA> "}"       { return emitToken( PRCURLY); }
<LONG_TEMPLATE_ENTRY> "{"                 { lBraceCount++; return emitToken( PLCURLY); }
<LONG_TEMPLATE_ENTRY> "}"                 {
                                              if (lBraceCount == 0) {
                                                popState();
                                                return emitToken( LONG_TEMPLATE_ENTRY_END);
                                              }
                                              lBraceCount--;
                                              return emitToken( PRCURLY);
                                          }

<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY, METADATA> {META_WITH_ARGS}  { pushState(METADATA); return emitToken(META_WITH_ARGS);}
<METADATA> "("                              { lParenCount++; return emitToken(PLPAREN); }
<METADATA> {META_WITH_ARGS_END}             {
                                                if (lParenCount == 0) {
                                                  popState();
                                                  return emitToken(META_WITH_ARGS_END);
                                                }
                                                lParenCount--;
                                                return emitToken(PRPAREN);
                                            }

<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY> "("   { return emitToken(PLPAREN); }
<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY> ")"   { return emitToken(PRPAREN); }

<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY, METADATA> {

{WHITE_SPACE_CHAR}+                       { return emitToken( com.intellij.psi.TokenType.WHITE_SPACE);}

{CONDITIONAL_LINE}                        { return emitToken( MSL_COMMENT); }
{CONDITIONAL_ERROR}                       { return emitToken( CONDITIONAL_ERROR); }
{END_OF_LINE_COMMENT}                     { return emitToken( MSL_COMMENT); }
{C_STYLE_COMMENT}                         { return emitToken( MML_COMMENT); }
{DOC_COMMENT}                             { return emitToken( DOC_COMMENT); }

"..."                                     { return emitToken( OTRIPLE_DOT); }

// Detect the '...' in 'for (a in 0...10)' so that '0.' is not picked up as a float.
// This rule must appear before the '{mNUM_FLOAT}' rule.
// This is used instead of the lookahead '{mNUM_FLOAT} / [^"."]}' regexp because the
// lookahead precludes proper detection at the end of the file/stream.
{mNUM_INT} ".."                           {  yypushback(2); return emitToken( LITINT); }

{mNUM_FLOAT}                              {  return emitToken( LITFLOAT); }
{mNUM_OCT}                                {  return emitToken( LITOCT); }
{mNUM_BIN}                                {  return emitToken( LITBIN); }
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
"from"                                    {  return emitToken( KFROM);  }
"to"                                      {  return emitToken( KTO );  }

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
"final"                                   {  return emitToken( KFINAL);  }
"public"                                  {  return emitToken( KPUBLIC);  }
"private"                                 {  return emitToken( KPRIVATE);  }
"static"                                  {  return emitToken( KSTATIC);  }
"dynamic"                                 {  return emitToken( KDYNAMIC);  }
"overload"                                {  return emitToken( KOVERLOAD);  }
"never"                                   {  return emitToken( KNEVER);  }
"override"                                {  return emitToken( KOVERRIDE);  }
"inline"                                  {  return emitToken( KINLINE);  }

// "macro" is a valid package name so to avoid problems with qnames we require the keyword to be followed
// by whitespace or other common symbols used in code that does not involve references.
"macro" /{WHITE_SPACE_CHAR}               {  return emitToken( KMACRO2); }
"macro" /\(                               {  return emitToken( KMACRO2); }
"macro" /:                                {  return emitToken( KMACRO2); }


"untyped"                                 {  return emitToken( KUNTYPED);  }
"typedef"                                 {  return emitToken( KTYPEDEF);  }

"extern" /{WHITE_SPACE_CHAR}+             {  return emitToken( KEXTERN);  }

"try"                                     {  return emitToken( KTRY);  }
"catch"                                   {  return emitToken( KCATCH);  }

{META}                                    {  return emitToken( META_ID); }
{IDENTIFIER_WITH__DOLLAR}                 {  return emitToken( MACRO_ID); }
{IDENTIFIER_NO_DOLLAR}                    {  return emitToken( ID); }

"?."                                      { return emitToken( OQUEST_DOT); }
"."                                       { return emitToken( ODOT); }

"["                                       { return emitToken( PLBRACK); }
"]"                                       { return emitToken( PRBRACK); }

":"                                       { return emitToken( OCOLON); }
";"                                       { return emitToken( OSEMI); }
","                                       { return emitToken( OCOMMA); }

"->"                                      { return emitToken( OARROW); }

"=="                                      { return emitToken( OEQ); }
"="                                       { return emitToken( OASSIGN); }

"!="                                      { return emitToken( ONOT_EQ); }
"!"                                       { return emitToken( ONOT); }
"~" / [^"/"]                              { return emitToken( OCOMPLEMENT); }

"??="                                     { return emitToken( OQUEST_QUEST_ASSIGN);}
"??"                                      { return emitToken( OQUEST_QUEST);}
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

// Haxe 4 inline XML/markup literal, e.g. <xml attr>..</xml>, <xml/>, or a fragment <>...</>.
// Only attempted where an expression is expected (see isExpressionExpected()); this never
// conflicts with generics/comparisons since those always follow an identifier or value.
// Also only attempted in an *active* conditional-compilation branch: inside an inactive
// "#if"/"#end" block this rule must stay as inert as the plain "<" it replaces -- pushing
// lexer state from dead code would derail lexing of whatever comes after the inactive block.
"<" {XML_NAME}?                           {
                                              if (isExpressionExpected() && ccsupport.currentContextIsActive()) {
                                                  String text = yytext().toString();
                                                  String name = text.substring(1); // drops the "<"
                                                  xmlContexts.push(new XmlContext(text, "</" + name + ">", !name.isEmpty()));
                                                  pushState(XML_CONTENT);
                                                  return emitToken( XML_TAG_START);
                                              } else {
                                                  yypushback(yylength() - 1);
                                                  return emitToken( OLESS);
                                              }
                                          }

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
// avoid BAD_CHARACTER for conditional expressions (#...)
"#"                                       { return processConditional(PPHASH); }
// avoid BAD_CHARACTER for reification (${...})
"$"                                      { return emitToken( DOLLAR); }
} // <YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY>

// "


<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY, METADATA> \"   { pushState(QUO_STRING); return emitToken( OPEN_QUOTE); }
<QUO_STRING> \"                            { popState(); return emitToken( CLOSING_QUOTE); }
<QUO_STRING> {STRING_ESCAPE_PART}          { return emitToken( ESCAPED_STRING_PART); }
<QUO_STRING> {STRING_INVALID_ESCAPE}       { return emitToken( STRING_INVALID_ESCAPE); }

<QUO_STRING> {DOUBLE_DOLLAR}               { return emitToken( REGULAR_STRING_PART); }
<QUO_STRING> {REGULAR_QUO_STRING_PART}     { return emitToken( REGULAR_STRING_PART); }
<QUO_STRING> {SHORT_TEMPLATE_ENTRY}        { return emitToken( REGULAR_STRING_PART); }

<QUO_STRING> {LONG_TEMPLATE_ENTRY_START}   { return emitToken( REGULAR_STRING_PART); }
<QUO_STRING> {LONELY_DOLLAR}               { return emitToken( REGULAR_STRING_PART); }

// Support single quote strings: "'"

<YYINITIAL, CC_BLOCK, LONG_TEMPLATE_ENTRY, METADATA> \'     { pushState(APOS_STRING); return emitToken( OPEN_QUOTE); }
<APOS_STRING> \'                            { popState(); return emitToken( CLOSING_QUOTE); }
<APOS_STRING> {STRING_ESCAPE_PART}          { return emitToken( ESCAPED_STRING_PART); }
<APOS_STRING> {STRING_INVALID_ESCAPE}       { return emitToken( STRING_INVALID_ESCAPE); }
<APOS_STRING> {DOUBLE_DOLLAR}               { return emitToken( REGULAR_STRING_PART); }

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


<XML_CONTENT> {
{WHITE_SPACE_CHAR}+                       { return emitToken(com.intellij.psi.TokenType.WHITE_SPACE);}
// space between < and tag name is not allowed so we do not have to worry about that here when creating patterns.
// (space before a "<" in XML literals will make the compiler return Error: Unterminated markup literal)

// Closing-tag-shaped text (e.g. "</xml>" or, for a fragment, "</>"). Only ends the literal when
// it matches this context's own close tag at depth 0; otherwise it's just more content text.
"</" {XML_NAME}? ">"                      {
                                              XmlContext ctx = xmlContexts.peek();
                                              String s = yytext().toString();
                                              if (s.equals(ctx.closeTag)) {
                                                  if (ctx.depth == 0) {
                                                      xmlContexts.pop();
                                                      popState();
                                                      return emitToken(XML_TAG_END);
                                                  }
                                                  ctx.depth--;
                                                  ctx.inOpenTag = false;
                                                  return emitToken(XML_SUB_TAG_CONTAINER_END);
                                              }
                                              ctx.inOpenTag = false;
                                              return emitToken(XML_SUB_TAG_CONTAINER_END);
                                          }

// Opening-tag-shaped text. Only a re-occurrence of the *same* tag name increases nesting depth.
"<" {XML_NAME}                            {
                                              XmlContext ctx = xmlContexts.peek();
                                              String s = yytext().toString();
                                              if (s.equals(ctx.openTag)) {
                                                  ctx.depth++;
                                                  ctx.inOpenTag = true;
                                              } else {
                                                  ctx.inOpenTag = false;
                                              }
                                              return emitToken(XML_SUB_TAG_START);
                                          }


// Self-close. Only counts while still positioned right after an (outer or same-name) open tag.
"/>"                                       {
                                              XmlContext ctx = xmlContexts.peek();
                                              if (ctx.inOpenTag) {
                                                  ctx.depth--;
                                              }
                                              ctx.inOpenTag = false;
                                              if (ctx.depth < 0) {
                                                  xmlContexts.pop();
                                                  popState();
                                                  return emitToken( XML_TAG_END);
                                              }
                                              return emitToken(XML_SUB_TAG_EMPTY_END);
                                          }

{DOUBLE_DOLLAR}                            { return emitToken( XML_TEXT); }
{SHORT_TEMPLATE_ENTRY}                     {
                                                  pushState(SHORT_TEMPLATE_ENTRY);
                                                  yypushback(yylength() - 1);
                                                  return emitToken(SHORT_TEMPLATE_ENTRY_START);
                                           }
{LONG_TEMPLATE_ENTRY_START}                {
                                                  pushState(LONG_TEMPLATE_ENTRY);
                                                 return emitToken(LONG_TEMPLATE_ENTRY_START);
                                           }

{LONELY_DOLLAR}                            { return emitToken( XML_TEXT); }

// the following tokens have been added in an attempt to provide some highlighting inside XML blocks.
"true"                                    { return emitToken( KTRUE ); }
"false"                                   { return emitToken( KFALSE ); }

{mNUM_FLOAT}                              { return emitToken( LITFLOAT ); }
{mNUM_OCT}                                { return emitToken( LITOCT ); }
{mNUM_HEX}                                { return emitToken( LITHEX ); }
{mNUM_INT}                                { return emitToken( LITINT ); }

"("                                       { return emitToken( PLPAREN ); }
")"                                       { return emitToken( PRPAREN ); }

"{"                                       { return emitToken( PLCURLY ); }
"}"                                       { return emitToken( PRCURLY ); }
\"                                        { return emitToken( XML_TEXT ); }

"="                                       { return emitToken( OASSIGN ); }
","                                       { return emitToken( OCOMMA); }
"."                                       { return emitToken( ODOT); }

//  must exclude  '<' / '/' / '>' to avoid consuming xml end tags.
// (we also exclude other tags that we intend to use for highlighting)
[^<\/>$\s=\{\}\(\)\"\,\.]+                { return emitToken( XML_TEXT ); }


// and lone '<' / '/' / '>' that didn't form one of the patterns above.
// (can anything inside the XML but ">" can also be part of the end of a tag)
"<" | "/" | ">"                           { return emitToken( XML_TEXT); }

}

// Parses the *condition* in a compiler conditional construct (e.g. #if <condition> ...)
<COMPILER_CONDITIONAL> {

{WHITE_SPACE_CHAR}+                       { return conditionAppend(com.intellij.psi.TokenType.WHITE_SPACE);}

"true"                                    { return conditionAppend( KTRUE ); }
"false"                                   { return conditionAppend( KFALSE ); }

{CONDITIONAL_IDENTIFIER}                  { return conditionAppend( ID ); }

{mNUM_FLOAT}                              { return conditionAppend( LITFLOAT ); }
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
                                            log.debug("Bad termination of PP condition: \"" + yytext() + "\"");
                                            yypushback(1);
                                            conditionEnd();
                                            return PPBODY;
                                          }
}

// Strings inside of compiler conditionals.  They can't use string interpolation/templates (e.g. $var).
<CC_STRING> {
\"                                        { popState(); return conditionAppend( CLOSING_QUOTE ); }
{STRING_ESCAPE_PART}                      { return conditionAppend( ESCAPED_STRING_PART ); }
{REGULAR_QUO_STRING_PART}                 { return conditionAppend( REGULAR_STRING_PART ); }
}
<CC_APOS_STRING> {
\'                                        { popState(); return conditionAppend( CLOSING_QUOTE ); }
{STRING_ESCAPE_PART}                      { return conditionAppend( ESCAPED_STRING_PART ); }
{REGULAR_APOS_STRING_PART}                { return conditionAppend( REGULAR_STRING_PART ); }
}

<QUO_STRING, APOS_STRING, SHORT_TEMPLATE_ENTRY, LONG_TEMPLATE_ENTRY, CC_BLOCK, METADATA, XML_CONTENT> .  { return emitToken( com.intellij.psi.TokenType.BAD_CHARACTER ); }

.                                         {
                                            yybegin(YYINITIAL);
                                            return emitToken( com.intellij.psi.TokenType.BAD_CHARACTER );
                                          }
