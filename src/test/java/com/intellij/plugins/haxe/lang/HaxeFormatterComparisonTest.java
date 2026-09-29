package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.ide.formatter.hxformat.HxformatDefaultProfile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.testFramework.junit5.RunInEdt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parity with HaxeCheckstyle's haxe-formatter (the vshaxe formatter). Each
 * rule's fixture directory holds a deliberately misformatted input.hx and
 * the real tool's output for it, hxformat.hx; the fixture README describes
 * how to regenerate them. The tests run under the haxe-formatter DEFAULTS
 * profile, and every rule asserts that the output equals the tool's, apart
 * from line endings and trailing newlines.
 */
@DisplayName("Formatting: haxe-formatter comparison")
public class HaxeFormatterComparisonTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/formatter/comparison/";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // the production default profile is the one under test, so the fixtures
    // also guard the baseline of the hxformat.json import
    installTemporarySettings(HxformatDefaultProfile::apply);
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("spacing")
  class Spacing {
    @Test
    @DisplayName("spacing keyword parens")
    public void testSpacingKeywordParens() throws Exception {
      doParityTest("spacing-keyword-parens");
    }

    @Test
    @DisplayName("spacing operators")
    public void testSpacingOperators() throws Exception {
      doParityTest("spacing-operators");
    }

    @Test
    @DisplayName("spacing ternary and colons")
    public void testSpacingTernaryAndColons() throws Exception {
      doParityTest("spacing-ternary-and-colons");
    }

    @Test
    @DisplayName("spacing commas")
    public void testSpacingCommas() throws Exception {
      doParityTest("spacing-commas");
    }

    @Test
    @DisplayName("spacing within parens")
    public void testSpacingWithinParens() throws Exception {
      doParityTest("spacing-within-parens");
    }

    @Test
    @DisplayName("function types")
    public void testFunctionTypes() throws Exception {
      doParityTest("function-types");
    }

    @Test
    @DisplayName("bracket spacing")
    public void testBracketSpacing() throws Exception {
      doParityTest("bracket-spacing");
    }

    @Test
    @DisplayName("type param spacing")
    public void testTypeParamSpacing() throws Exception {
      doParityTest("type-param-spacing");
    }

    @Test
    @DisplayName("type check colon")
    public void testTypeCheckColon() throws Exception {
      doParityTest("type-check-colon");
    }

    @Test
    @DisplayName("metadata parens")
    public void testMetadataParens() throws Exception {
      doParityTest("metadata-parens");
    }

    @Test
    @DisplayName("multi var and patterns")
    public void testMultiVarAndPatterns() throws Exception {
      doParityTest("multi-var-and-patterns");
    }

    @Test
    @DisplayName("string interpolation")
    public void testStringInterpolation() throws Exception {
      doParityTest("string-interpolation");
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("blank lines")
  class BlankLines {
    @Test
    @DisplayName("blank lines members")
    public void testBlankLinesMembers() throws Exception {
      doParityTest("blank-lines-members");
    }

    @Test
    @DisplayName("blank lines header")
    public void testBlankLinesHeader() throws Exception {
      doParityTest("blank-lines-header");
    }

    @Test
    @DisplayName("single line types")
    public void testSingleLineTypes() throws Exception {
      doParityTest("single-line-types");
    }

    @Test
    @DisplayName("type blank lines")
    public void testTypeBlankLines() throws Exception {
      // emptyLines.betweenTypes=1 inserts the blank between multi-line types
      // (typedefs included) and caps a run of blanks; single-line types stay snug
      doParityTest("type-blank-lines");
    }

    @Test
    @DisplayName("block edge blanks")
    public void testBlockEdgeBlanks() throws Exception {
      // emptyLines.afterLeftCurly/beforeRightCurly/beforeBlocks=Remove: blanks
      // hugging block braces and case colons go, statement blanks keep max 1
      doParityTest("block-edge-blanks");
    }

    @Test
    @DisplayName("field group blanks")
    public void testFieldGroupBlanks() throws Exception {
      // classEmptyLines.afterStaticVars/afterPrivateVars: a staticness or
      // visibility change splits the var block with one blank, landing BEFORE
      // a field's leading comment; same-group vars stay snug
      doParityTest("field-group-blanks");
    }

    @Test
    @DisplayName("documented field blanks")
    public void testDocumentedFieldBlanks() throws Exception {
      // beforeDocCommentEmptyLines/afterFieldsWithDocComments: a field's doc
      // comment stands one blank off from BOTH neighbors; plain line and
      // block comments between same-group fields stay snug
      doParityTest("documented-field-blanks");
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("braces and bodies")
  class BracesAndBodies {
    @Test
    @DisplayName("braces placement")
    public void testBracesPlacement() throws Exception {
      doParityTest("braces-placement");
    }

    @Test
    @DisplayName("empty curly")
    public void testEmptyCurly() throws Exception {
      doParityTest("empty-curly");
    }

    @Test
    @DisplayName("same line bodies")
    public void testSameLineBodies() throws Exception {
      doParityTest("same-line-bodies");
    }

    @Test
    @DisplayName("function body next line")
    public void testFunctionBodyNextLine() throws Exception {
      doParityTest("function-body-next-line");
    }

    @Test
    @DisplayName("typedef extension")
    public void testTypedefExtension() throws Exception {
      doParityTest("typedef-extension");
    }

    @Test
    @DisplayName("expression same line")
    public void testExpressionSameLine() throws Exception {
      // expressionIf/expressionTry=Same: a value-position if or try joins
      // onto one line whatever the statement-body policies say - written
      // breaks and missing spaces (")1", ")-1") included; an expression
      // switch keeps its case lines (expressionCase=Keep) with the colon spaced
      doParityTest("expression-same-line");
    }

    @Test
    @DisplayName("return join")
    public void testReturnJoin() throws Exception {
      doParityTest("return-join");
    }

    @Test
    @DisplayName("openfl braces")
    public void testOpenflBraces() throws Exception {
      // lineEnds.leftCurly=both, objectLiteralCurly.leftCurly=after (fixture
      // hxformat.json): Allman blocks with cuddled object literals
      doParityTest("openfl-braces", HaxeCodeStyleTweaks::allmanBraces);
    }

    @Test
    @DisplayName("allman value blocks")
    public void testAllmanValueBlocks() throws Exception {
      // same config as openfl-braces: a block, function literal or arrow body
      // used as a VALUE opens on its own line one step in from the declaration
      doParityTest("allman-value-blocks", HaxeCodeStyleTweaks::allmanBraces);
    }

    @Test
    @DisplayName("allman typedef braces")
    public void testAllmanTypedefBraces() throws Exception {
      // lineEnds.leftCurly=both reaches typedefCurly/anonTypeCurly too: a
      // typedef body opens on its own line and lists one field per line; a
      // multi-line anonymous type in a type hint opens one step in; one-line
      // anonymous types in hints stay inline
      doParityTest("allman-typedef-braces", HaxeCodeStyleTweaks::allmanBraces);
    }

    @Test
    @DisplayName("allman meta first member")
    public void testAllmanMetaFirstMember() throws Exception {
      // classEmptyLines.beginType=0 holds when the first member opens with
      // metadata, a multi-line string argument included
      doParityTest("allman-meta-first-member", HaxeCodeStyleTweaks::allmanBraces);
    }

    @Test
    @DisplayName("if body same line")
    public void testIfBodySameLine() throws Exception {
      // sameLine.ifBody=same (fixture hxformat.json): a non-block if-body
      // JOINS the guard's line; else/while bodies keep the Next default
      doParityTest("if-body-same-line", settings -> {
        HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
        haxe.IF_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE;
      });
    }

    @Test
    @DisplayName("metadata with next line braces")
    public void testMetadataWithNextLineBraces() throws Exception {
      // lineEnds.leftCurly/rightCurly=both (fixture hxformat.json): metadata
      // opening a declaration's line must not cost the next-line '{' its
      // member indent
      doParityTest("metadata-braces", HaxeCodeStyleTweaks::allmanBraces);
    }

    @Test
    @DisplayName("case body next line")
    public void testCaseBodyNextLine() throws Exception {
      // sameLine.caseBody=next: an inline case body breaks onto its own line
      // in a STATEMENT switch; an expression switch keeps inline bodies
      // (expressionCase=keep)
      doParityTest("case-body-next-line");
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("indentation and wrapping")
  class IndentationAndWrapping {
    @Test
    @DisplayName("indentation tabs")
    public void testIndentationTabs() throws Exception {
      doParityTest("indentation-tabs");
    }

    @Test
    @DisplayName("array literal wrap")
    public void testArrayLiteralWrap() throws Exception {
      doParityTest("array-literal-wrap");
    }

    @Test
    @DisplayName("map literal wrap")
    public void testMapLiteralWrap() throws Exception {
      doParityTest("map-literal-wrap");
    }

    @Test
    @DisplayName("object literal wrap")
    public void testObjectLiteralWrap() throws Exception {
      doParityTest("object-literal-wrap");
    }

    @Test
    @DisplayName("method chain wrap")
    public void testMethodChainWrap() throws Exception {
      doParityTest("method-chain-wrap");
    }

    @Test
    @DisplayName("call args wrap")
    public void testCallArgsWrap() throws Exception {
      // a wrapped argument continues ONE step in from the statement's line,
      // nested calls included; a wrapped chain link after it likewise
      doParityTest("call-args-wrap");
    }

    @Test
    @DisplayName("method signature wrap")
    public void testMethodSignatureWrap() throws Exception {
      // wrapped parameters continue TWO steps in from the declaration, so the
      // signature stands off from the body that follows at one
      doParityTest("method-signature-wrap");
    }

    @Test
    @DisplayName("enum constructor wrap")
    public void testEnumConstructorWrap() throws Exception {
      // a constructor's wrapped arguments continue one step in, like call arguments
      doParityTest("enum-constructor-wrap");
    }

    @Test
    @DisplayName("extends implements wrap")
    public void testExtendsImplementsWrap() throws Exception {
      doParityTest("extends-implements-wrap");
    }

    @Test
    @DisplayName("multi var wrap indent")
    public void testMultiVarWrapIndent() throws Exception {
      // a declarator wrapped onto its own line continues one step in from the
      // var line; a list whose joined line passes 80 columns splits after every
      // comma unless a declarator is short enough (<= 15 as the tool measures
      // it: with its comma, the first one two wider), which keeps it filling
      doParityTest("multi-var-wrap-indent");
    }

    @Test
    @DisplayName("bool chain wrap")
    public void testBoolChainWrap() throws Exception {
      // wrapping.opBoolChain: a chain of more than four operands whose items
      // total 120 columns splits one operand per line, operators leading;
      // each chain level continues ONE step in from the line it starts on;
      // shorter chains keep their written shape
      doParityTest("bool-chain-wrap");
    }

    @Test
    @DisplayName("additive chain rules")
    public void testAdditiveChainRules() throws Exception {
      // wrapping.opAddSubChain on JOINED input: the chains of one call are
      // judged together (operand count and total), a long line explodes or
      // fills them, a short one explodes past 4 operands totalling over 120
      doParityTest("additive-chain-rules");
    }

    @Test
    @DisplayName("additive arg wrap")
    public void testAdditiveArgWrap() throws Exception {
      // a call argument's wrapped arithmetic continuation stays ONE step past
      // the call's line, and the written break points are kept
      doParityTest("additive-arg-wrap");
    }

    @Test
    @DisplayName("meta same line wraps")
    public void testMetaSameLineWraps() throws Exception {
      // metadata opening a declaration's line: the wrapped extends list,
      // parameters, return type and array initializer under it continue
      // from the DECLARATION's indent, not the metadata-less column
      doParityTest("meta-same-line-wraps");
    }

    @Test
    @DisplayName("meta same line wraps allman")
    public void testMetaSameLineWrapsAllman() throws Exception {
      // lineEnds.leftCurly/rightCurly=both (fixture hxformat.json): the same
      // continuations keep the member step when the class body's '{' owns
      // its line, and the next-line body brace sits at the member's indent
      doParityTest("meta-same-line-wraps-allman", HaxeCodeStyleTweaks::allmanBraces);
    }

    @Test
    @DisplayName("signature fill repack")
    public void testSignatureFillRepack() throws Exception {
      // wrapping.functionSignature/anonFunctionSignature fillLine on JOINED
      // input: hand-broken parameters re-pack up to the margin (breaks after
      // the opening and before the closing paren go too); a parameter whose
      // ", " would reach column 160 moves down two steps, the last one also
      // when the paren, the return hint and the brace after it pass 160
      doParityTest("signature-fill-repack");
    }

    @Test
    @DisplayName("call fill repack")
    public void testCallFillRepack() throws Exception {
      // wrapping.callParameter fillLine on JOINED input: hand-broken call,
      // new and lambda arguments re-pack; what follows the closing paren on
      // the line (`;`, `) {`) counts for the last argument; a first argument
      // reaching the margin restarts the count at the continuation, and the
      // line still past the margin then moves the next argument down alone
      doParityTest("call-fill-repack");
    }

    @Test
    @DisplayName("signature wrap indent")
    public void testSignatureWrapIndent() throws Exception {
      // a wrapped parameter continues two steps under a function with
      // statements (an expression body included) and one step under a
      // bodiless declaration or an empty {} - named and anonymous alike; a
      // first parameter reaching the margin moves the second at the same depth
      doParityTest("signature-wrap-indent");
    }

    @Test
    @DisplayName("todo checks")
    public void testTodoChecks() throws Exception {
      // one sample covering the four continuation rules:
      // bodiless and empty-body signatures at one step, a full body at two,
      // a same-line metadata declaration anchoring its chopped initializer,
      // hand-broken signatures and calls re-packed, a nested inactive #if
      doParityTest("todo-checks");
    }

    @Test
    @DisplayName("todo checks allman")
    public void testTodoChecksAllman() throws Exception {
      // the same sample under lineEnds.leftCurly/rightCurly=both: a signature
      // whose `{` moves to the next line no longer counts it, so it fits
      doParityTest("todo-checks-allman", HaxeCodeStyleTweaks::allmanBraces);
    }

    @Test
    @DisplayName("array item rules")
    public void testArrayItemRules() throws Exception {
      // wrapping.arrayWrap on the items (each with its ", "): up to 80 in
      // total stay on one line whatever was written; 10+ items of up to 10
      // fill after a leading break; an item of 30+, 4+ items, or an item
      // written over several lines go one per line
      doParityTest("array-item-rules");
    }

    @Test
    @DisplayName("array wrap checks")
    public void testArrayWrapChecks() throws Exception {
      // one sample for wrapping.arrayWrap: every rule once, hand-broken
      // twins re-joined or re-broken, the margin overflow, and a nested literal
      // judged on its own
      doParityTest("array-wrap-checks");
    }

    @Test
    @DisplayName("map object item rules")
    public void testMapObjectItemRules() throws Exception {
      // wrapping.mapWrap follows the array list on a map's entries;
      // wrapping.objectLiteral keeps up to three fields written on one line
      // however long, chops from four, and any object written over lines
      doParityTest("map-object-item-rules");
    }

    @Test
    @DisplayName("array wrap checks allman")
    public void testArrayWrapChecksAllman() throws Exception {
      doParityTest("array-wrap-checks-allman", HaxeCodeStyleTweaks::allmanBraces);
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("conditionals")
  class Conditionals {
    @Test
    @DisplayName("conditional compilation")
    public void testConditionalCompilation() throws Exception {
      doParityTest("conditional-compilation");
    }

    @Test
    @DisplayName("conditional inline spacing")
    public void testConditionalInlineSpacing() throws Exception {
      // directives INSIDE an expression or a type stay on their line; #end
      // takes a space before it (except after an opening bracket) and after
      // it before an identifier/keyword/opening bracket, never before , or ;
      doParityTest("conditional-inline-spacing");
    }

    @Test
    @DisplayName("conditional case end")
    public void testConditionalCaseEnd() throws Exception {
      // a directive closing a region opened INSIDE a case body aligns with
      // that body, even though it sits past the body's last statement; a
      // region wrapping whole cases keeps its directives at case level
      doParityTest("conditional-case-end");
    }

    @Test
    @DisplayName("conditional inactive branches")
    public void testConditionalInactiveBranches() throws Exception {
      // inactive branches format with the SAME rules as active code, since
      // the tool does not tell them apart; branches that do not parse, like
      // the lone-operator case, stay verbatim, as in the tool's output
      doParityTest("conditional-inactive");
    }

    @Test
    @DisplayName("inactive branch bodies")
    public void testInactiveBranchBodies() throws Exception {
      // an inactive #if region around whole members formats like active code:
      // non-block else/for bodies still break, and a conditional holding only
      // an INACTIVE import stays snug in the import section
      doParityTest("inactive-branch-bodies");
    }

    @Test
    @DisplayName("conditional nested inactive")
    public void testConditionalNestedInactive() throws Exception {
      // a region nested inside an inactive branch splits it into fragments
      // (a '{' in one, its '}' in another); the branch aligns as one group,
      // each line's depth following the braces the fragments before it opened
      doParityTest("conditional-nested-inactive");
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("comments")
  class Comments {
    @Test
    @DisplayName("doc comment blanks")
    public void testDocCommentBlanks() throws Exception {
      doParityTest("doc-comment-blanks");
    }

    @Test
    @DisplayName("file header comment")
    public void testFileHeaderComment() throws Exception {
      doParityTest("file-header-comment");
    }

    @Test
    @DisplayName("comment blanks")
    public void testCommentBlanks() throws Exception {
      // comments stacked before a member stay snug and take the member's
      // indent; the blanks around them follow the member rules (a run caps
      // at one, the function gap is inserted) and a blank BETWEEN two
      // stacked block comments goes (betweenMultilineComments=0)
      doParityTest("comment-blanks");
    }

    @Test
    @DisplayName("doc comment indent")
    public void testDocCommentIndent() throws Exception {
      // inner doc comment lines: the body sits one level deeper than the
      // comment and keeps its markdown indentation, a continuation written at
      // column 0 moves to the body's indent, and a starred comment aligns its
      // stars one space in
      doParityTest("doc-comment-indent");
    }

    @Test
    @DisplayName("multiline comments")
    public void testMultilineComments() throws Exception {
      // inner lines of plain /*..*/ comments: common leading whitespace
      // removed, middle lines one level deeper, leading stars aligned under
      // the opener, empty inner lines left empty
      doParityTest("multiline-comments");
    }

    @Test
    @DisplayName("switch case comment")
    public void testSwitchCaseComment() throws Exception {
      // a comment standing alone between cases indents as case-BODY content
      doParityTest("switch-case-comment");
    }

    @Test
    @DisplayName("line comment space")
    public void testLineCommentSpace() throws Exception {
      // whitespace.addLineCommentSpace: "//text" becomes "// text"; divider
      // art, extra slashes and already-spaced content keep their shape
      doParityTest("line-comment-space");
    }
  }

  @Nested
  @RunInEdt(writeIntent = true)
  @DisplayName("imports")
  class Imports {
    @Test
    @DisplayName("import blanks")
    public void testImportBlanks() throws Exception {
      doParityTest("import-blanks");
    }

    @Test
    @DisplayName("import grouping")
    public void testImportGrouping() throws Exception {
      // betweenImports=1, betweenImportsLevel=firstLevelPackage (fixture hxformat.json)
      doParityTest("import-grouping", settings -> {
        HaxeCodeStyleSettings haxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
        haxe.BLANK_LINES_BETWEEN_IMPORT_GROUPS = 1;
        haxe.IMPORT_GROUP_PACKAGE_DEPTH = 1;
      });
    }

    @Test
    @DisplayName("import conditional blanks")
    public void testImportConditionalBlanks() throws Exception {
      // conditional-compilation directives wrapping imports format as part of
      // the import section: betweenImports=0 spans them, the before-type gap
      // follows the closing #end
      doParityTest("import-conditional-blanks");
    }

    @Test
    @DisplayName("import end meta conditional")
    public void testImportEndMetaConditional() throws Exception {
      // the section-end gap follows the import conditional's closing #end even
      // when ANOTHER conditional (wrapping class metadata, not imports) comes
      // next; that conditional then stays snug with its class
      doParityTest("import-end-meta-conditional");
    }
  }

  /** Formats the rule's input.hx and compares the result with the tool's hxformat.hx. */
  private void doParityTest(String rule) throws Exception {
    String actual = normalize(reformatFile(rule + "/input.hx"));

    assertEquals(fixture(rule, "hxformat.hx"), actual, "our output must match haxe-formatter for " + rule);
  }

  /**
   * Parity under a NON-default hxformat option. The fixture directory holds
   * the hxformat.json that produced hxformat.hx, and the tweak applies the
   * equivalent settings on top of the default profile.
   */
  private void doParityTest(String rule, Consumer<CodeStyleSettings> tweak) throws Exception {
    Consumer<CodeStyleSettings> profile = HxformatDefaultProfile::apply;
    installTemporarySettings(profile.andThen(tweak));
    doParityTest(rule);
  }

  @NotNull
  private String fixture(String rule, String name) throws IOException {
    return normalize(Files.readString(Path.of(getTestDataPath(), rule, name)));
  }

  /** Unifies line endings and drops trailing newlines, which the IDE manages at save time rather than in the formatter. */
  @NotNull
  private static String normalize(@NotNull String text) {
    return text.replace("\r\n", "\n").replaceAll("\n+$", "");
  }
}
