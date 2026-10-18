package com.intellij.plugins.haxe.lang;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiComment;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lexer.Lexer;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedFieldInspection;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexer;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclarationList;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.plugins.haxe.util.HaxeTestDefines;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Behavior of INACTIVE conditional branches. The parser lexes a dead region
 * as ONE PPBODY blob (lazily parsed into real PSI), the editor highlighter
 * keeps real token types there instead - and everything downstream builds on
 * that split: analysis exemption, dimmed colors, best-effort completion and
 * token-stream editing mechanics. Incremental-relex restartability across
 * conditionals lives in HaxeLexerRestartTest.
 */
@DisplayName("Conditional compilation: inactive branches")
public class HaxeInactiveBranchesTest extends HaxeLightFixtureTestCase {

  // required by the base; every fixture here is inline configureByText
  @Override
  protected String getBasePath() {
    return "/parsing/";
  }

  @Test
  @DisplayName("dead branch is one blob token")
  public void testDeadBranchIsOneBlobToken() {
    List<String> bodies = ppBodyTokens("""
      class Foo {
      \t#if never
      \tfunction dead():Void {
      \t\ttrace("gone");
      \t}
      \t#end
      \tfunction live():Void {}
      }""");

    assertEquals(1, bodies.size(), "the whole branch must merge into one PPBODY: " + bodies);
    assertTrue(bodies.get(0).contains("function dead"), "the blob carries the branch's full text");
    assertTrue(bodies.get(0).contains("trace"), "nested content stays inside the blob");
  }

  @Test
  @DisplayName("each region between directives is its own blob")
  public void testEachRegionBetweenDirectivesIsItsOwnBlob() {
    List<String> bodies = ppBodyTokens("""
      class Foo {
      \t#if never
      \tvar a:Int;
      \t#elseif never_either
      \tvar b:Int;
      \t#end
      }""");

    assertEquals(2, bodies.size(), "one blob per inactive region: " + bodies);
    assertTrue(bodies.get(0).contains("var a"));
    assertTrue(bodies.get(1).contains("var b"));
  }

  @Test
  @DisplayName("nested region lexes into the enclosing blob")
  public void testNestedRegionLexesIntoTheEnclosingBlob() {
    List<String> bodies = ppBodyTokens(NESTED_REGION_SOURCE);

    assertEquals(1, bodies.size(), "the nested region must not split the branch: " + bodies);
    assertTrue(bodies.get(0).contains("#if debug"), "the inner directives lie inside the blob");
    assertTrue(bodies.get(0).contains("#end\n\t}"), "the inner #end and the closing brace lie inside the blob");
  }

  @Test
  @DisplayName("code after a nested section stays inside the dead branch")
  public void testCodeAfterANestedSectionStaysInsideTheDeadBranch() {
    String source = """
      package p;

      #if (never && sys)
      import a.A;
      #if (lime && !macro)
      import b.B;
      #end
      import c.C;

      class File {}
      #end
      """;

    List<String> bodies = ppBodyTokens(source);
    assertEquals(1, bodies.size(), "the dead branch must stay ONE blob past the nested #end: " + bodies);
    assertTrue(bodies.get(0).contains("import c.C") && bodies.get(0).contains("class File"), bodies.get(0));

    PsiFile file = myFixture.configureByText("File.hx", source);
    PsiElement afterNested = file.findElementAt(source.indexOf("c.C"));
    assertNotNull(afterNested);
    PsiComment enclosing = PsiTreeUtil.getParentOfType(afterNested, PsiComment.class);
    assertInstanceOf(HaxeInactiveBody.class, enclosing, "the import after the nested #end sits inside the outer dead blob");
    HaxeInactiveBody outer = PsiTreeUtil.getParentOfType(afterNested, HaxeInactiveBody.class, true, PsiFile.class);
    assertNotNull(outer);
    assertTrue(outer.getText().contains("import a.A"), "...the SAME blob that holds the branch's first lines");
  }

  @Test
  @DisplayName("branch holding a nested region parses as code")
  public void testBranchHoldingANestedRegionParsesAsCode() {
    HaxeInactiveBody body = inactiveBody(NESTED_REGION_SOURCE);

    HaxeMethodDeclaration dead = PsiTreeUtil.findChildOfType(body, HaxeMethodDeclaration.class);
    assertNotNull(dead, "the dead method with a nested region must parse into real member PSI");
    assertNotNull(PsiTreeUtil.findChildOfType(dead, HaxeInactiveBody.class), "the inner branch not taken nests as a blob of its own");
  }

  private static final String NESTED_REGION_SOURCE = """
    class Foo {
    \t#if never
    \tfunction dead():Void {
    \t\t#if debug
    \t\ttrace("debug");
    \t\t#else
    \t\ttrace("release");
    \t\t#end
    \t}
    \t#end
    }""";

  @Test
  @DisplayName("statement level branch parses as statements")
  public void testStatementLevelBranchParsesAsStatements() {
    HaxeInactiveBody body = inactiveBody("""
      class Foo {
      \tfunction f():Void {
      \t\t#if never
      \t\tvar x = 1;
      \t\ttrace(x);
      \t\t#end
      \t}
      }""");

    assertNotNull(PsiTreeUtil.findChildOfType(body, HaxeLocalVarDeclarationList.class),
                  "dead statements must parse into real statement PSI");
  }

  @Test
  @DisplayName("trivial fragments stay error-free token soup")
  public void testTrivialFragmentsStayErrorFreeTokenSoup() {
    // recovery that pins no real structure (half an operator, a spliced
    // keyword) reads better as flat soup - no error shells in the tree
    HaxeInactiveBody body = inactiveBody("""
      class Foo {
      \tfunction f():Void {
      \t\tvar x = 1 #if never < #else > #end 2;
      \t}
      }""");

    assertFalse(body.hasCleanParse(), "a cross-branch splice cannot parse clean - the formatter must preserve it");
    assertNull(PsiTreeUtil.findChildOfType(body, PsiErrorElement.class),
               "the soup fallback carries no error elements");
    assertTrue(body.getNode().getFirstChildNode() != null, "the soup still exposes the raw tokens");
  }

  @Test
  @DisplayName("broken statement keeps the rest of the branch navigable")
  public void testBrokenStatementKeepsTheRestOfTheBranchNavigable() {
    // a missing semicolon must stay a LOCAL error: the sibling method keeps
    // real PSI and its references keep resolving instead of the whole branch
    // falling back to flat tokens
    PsiFile file = myFixture.configureByText("Foo.hx", """
      class Helper {}
      class Foo {
      \t#if never
      \tfunction broken():Void {
      \t\tvar x = 1
      \t\treturn;
      \t}
      \tfunction sibling(arg:Helper):Void {}
      \t#end
      }""");
    HaxeInactiveBody body = PsiTreeUtil.findChildOfType(file, HaxeInactiveBody.class);
    assertNotNull(body);

    assertFalse(body.hasCleanParse(), "the missing semicolon rules out a clean parse");
    assertEquals(2, PsiTreeUtil.findChildrenOfType(body, HaxeMethodDeclaration.class).size(),
                 "recovery keeps BOTH methods as real PSI");
    HaxeReferenceExpression helperReference = PsiTreeUtil.findChildrenOfType(body, HaxeReferenceExpression.class).stream()
      .filter(reference -> reference.textMatches("Helper"))
      .findFirst()
      .orElse(null);
    assertNotNull(helperReference, "the parameter's type stays a real reference");
    assertNotNull(helperReference.resolve(), "and it resolves to the same-file class");
  }

  @Test
  @DisplayName("dead code produces no error or warning markers")
  public void testDeadCodeProducesNoErrorOrWarningMarkers() {
    String source = """
      class Foo {
      \t#if never
      \tthis is not even haxe !!
      \tfunction broken(:Void {
      \t#end
      \tfunction live():Void {}
      }""";
    myFixture.configureByText("Foo.hx", source);

    var errors = myFixture.doHighlighting(HighlightSeverity.ERROR);
    assertTrue(errors.isEmpty(), "inactive branches are analysis-exempt: " + errors);
    var warnings = markersInsideTheConditional(HighlightSeverity.WARNING, source);
    assertTrue(warnings.isEmpty(), "dead code gets no warning markers either: " + warnings);
  }

  @Test
  @DisplayName("dead branch containing a doc comment still parses")
  public void testDeadBranchContainingADocCommentStillParses() {
    // the doc comment is a NESTED chameleon; grading must not force it to
    // parse while the candidate tree is detached (the platform forbids that)
    HaxeInactiveBody body = inactiveBody("""
      class Foo {
      \t#if never
      \t/**
      \t\tdocs for the dead method
      \t**/
      \tpublic function dead():Void {}
      \t#end
      }""");

    assertNotNull(PsiTreeUtil.findChildOfType(body, HaxeMethodDeclaration.class),
                  "member grading must survive a nested doc comment");
  }

  @Test
  @DisplayName("unused inspection ignores dead module fields")
  public void testUnusedInspectionIgnoresDeadModuleFields() {
    myFixture.enableInspections(new HaxeUnusedFieldInspection());
    String source = """
      package;
      #if never
      /**
      \tdocs
      **/
      var deadModuleField:String;
      #end
      class Foo {
      \tfunction f():Void {}
      }""";
    myFixture.configureByText("Foo.hx", source);

    var warnings = markersInsideTheConditional(HighlightSeverity.WEAK_WARNING, source);
    assertTrue(warnings.isEmpty(), "dead fields get no unused markers and no usage searches: " + warnings);
  }

  @Test
  @DisplayName("dead code gets dimmed per token colors")
  public void testDeadCodeGetsDimmedPerTokenColors() {
    String source = """
      class Foo {
      \tfunction f():Void {
      \t\t#if never
      \t\tvar x = "dead";
      \t\t#end
      \t}
      }""";
    myFixture.configureByText("Foo.hx", source);

    int varStart = source.indexOf("var x");
    var dimmed = myFixture.doHighlighting().stream()
      .filter(info -> info.getStartOffset() == varStart && info.getEndOffset() == varStart + 3)
      .filter(info -> info.forcedTextAttributes != null)
      .toList();
    assertFalse(dimmed.isEmpty(), "the dead 'var' keyword must carry dimmed enforced attributes");
    assertNotNull(dimmed.get(0).forcedTextAttributes.getForegroundColor(), "dimming blends the foreground");
  }

  @Test
  @DisplayName("dead identifiers without a dedicated color dim too")
  public void testDeadIdentifiersWithoutADedicatedColorDimToo() {
    // plainRef has no syntax-highlighter key; it must still get dimmed
    // default-text attributes instead of the flat dead-code color
    String source = """
      class Foo {
      \tfunction f():Void {
      \t\t#if never
      \t\ttrace(plainRef);
      \t\t#end
      \t}
      }""";
    myFixture.configureByText("Foo.hx", source);

    int refStart = source.indexOf("plainRef");
    boolean refDimmed = myFixture.doHighlighting().stream()
      .anyMatch(info -> info.getStartOffset() == refStart
                        && info.getEndOffset() == refStart + "plainRef".length()
                        && info.forcedTextAttributes != null);
    assertTrue(refDimmed, "every dead leaf gets dim attributes - nothing may keep the flat CC color");
  }

  @Test
  @DisplayName("dead metadata dims instead of losing its color")
  public void testDeadMetadataDimsInsteadOfLosingItsColor() {
    // metadata is its own PSI language - the dim annotator needs its
    // HaxeMetadata registration or dead @:meta renders as plain text
    String source = """
      class Foo {
      \t#if never
      \t@:keep var deadField:Int;
      \t#end
      }""";
    myFixture.configureByText("Foo.hx", source);

    int metaStart = source.indexOf("@:keep");
    boolean metaDimmed = myFixture.doHighlighting().stream()
      .anyMatch(info -> info.getStartOffset() >= metaStart
                        && info.getEndOffset() <= metaStart + "@:keep".length()
                        && info.forcedTextAttributes != null);
    assertTrue(metaDimmed, "dead metadata tokens must carry dim attributes");
  }

  @Test
  @DisplayName("dead doc comment dims as one block without markup accents")
  public void testDeadDocCommentDimsAsOneBlockWithoutMarkupAccents() {
    String source = """
      class Foo {
      \t#if never
      \t/**
      \t\tDead docs.
      \t\t@param value ignored
      \t**/
      \tfunction dead(value:Int):Void {}
      \t#end
      }""";
    myFixture.configureByText("Foo.hx", source);
    var infos = myFixture.doHighlighting();

    int docStart = source.indexOf("/**");
    int docEnd = source.indexOf("**/") + 3;
    boolean docDimmed = infos.stream()
      .anyMatch(info -> info.getStartOffset() == docStart && info.getEndOffset() == docEnd
                        && info.forcedTextAttributes != null);
    assertTrue(docDimmed, "the whole dead doc comment gets one dim annotation");

    var docTagKey = HaxeSyntaxHighlighterColors.DOC_TAG;
    boolean tagAccented = infos.stream()
      .anyMatch(info -> info.getStartOffset() >= docStart && info.getEndOffset() <= docEnd
                        && docTagKey.equals(info.forcedTextAttributesKey));
    assertFalse(tagAccented, "@param markup must not punch through the dimmed dead doc");
  }

  @Test
  @DisplayName("highlighting lexer keeps real token types in dead code")
  public void testHighlightingLexerKeepsRealTokenTypesInDeadCode() {
    // the editor's token-stream mechanics (brace matching/auto-close, enter
    // between braces) are blind wherever the lexer flattens to PPBODY
    String source = """
      class Foo {
      \t#if never
      \tfunction dead():Void {}
      \t#end
      }""";
    Lexer lexer = HaxeLexer.forHighlighting(getProject());
    lexer.start(source);
    List<IElementType> types = new ArrayList<>();
    while (lexer.getTokenType() != null) {
      types.add(lexer.getTokenType());
      lexer.advance();
    }

    assertFalse(types.contains(HaxeTokenTypeSets.PPBODY), "dead code must keep real token types: " + types);
    assertTrue(types.contains(HaxeTokenTypes.PPIF), "directives keep their identity");
    long curlies = types.stream().filter(t -> t == HaxeTokenTypes.PLCURLY).count();
    assertEquals(2, curlies, "brace tokens must be visible to the brace matcher");
  }

  @Test
  @DisplayName("typing a brace in a dead branch auto closes it")
  public void testTypingABraceInADeadBranchAutoClosesIt() {
    myFixture.configureByText("Foo.hx", """
      class Foo {
      \tfunction f():Void {
      \t\t#if never
      \t\tif (true)<caret>
      \t\t#end
      \t}
      }""");

    myFixture.type('{');

    String text = myFixture.getEditor().getDocument().getText();
    assertTrue(text.contains("if (true){}"), "the closing brace must auto-insert:\n" + text);
  }

  @Test
  @DisplayName("enter between dead braces splits and indents")
  public void testEnterBetweenDeadBracesSplitsAndIndents() {
    myFixture.configureByText("Foo.hx", """
      class Foo {
      \tfunction f():Void {
      \t\t#if never
      \t\tif (true){<caret>}
      \t\t#end
      \t}
      }""");

    myFixture.type('\n');

    String text = myFixture.getEditor().getDocument().getText();
    assertFalse(text.contains("{}"), "the brace pair must split across lines:\n" + text);
    int caretOffset = myFixture.getCaretOffset();
    int caretLineStart = text.lastIndexOf('\n', caretOffset - 1) + 1;
    String caretIndent = text.substring(caretLineStart, caretOffset);
    assertTrue(caretIndent.isBlank() && caretIndent.length() > "\t\t".length(),
               "the new line indents deeper than the if line, got '" + caretIndent + "' in:\n" + text);
  }

  @Test
  @DisplayName("keyword completion works inside a dead branch")
  public void testKeywordCompletionWorksInsideADeadBranch() {
    myFixture.configureByText("Foo.hx", """
      class Foo {
      \tfunction f():Void {
      \t\t#if never
      \t\ti<caret>
      \t\t#end
      \t}
      }""");

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "completion must run inside the parsed dead branch");
    // keyword items carry template text ("if (<CARET>)"), so match by prefix
    boolean ifOffered = lookups.stream().anyMatch(lookup -> lookup.startsWith("if"));
    assertTrue(ifOffered, "statement keywords must be offered: " + lookups);
    assertTrue(lookups.contains("function "), "member keywords must be offered: " + lookups);
  }

  @Test
  @DisplayName("local variables complete inside a dead branch")
  public void testLocalVariablesCompleteInsideADeadBranch() {
    myFixture.configureByText("Foo.hx", """
      class Foo {
      \tfunction f():Void {
      \t\t#if never
      \t\tvar counter = 1;
      \t\tcou<caret>
      \t\t#end
      \t}
      }""");

    myFixture.completeBasic();
    // the local is the single match, so completion auto-inserts it - the
    // second occurrence in the document is the proof it resolved
    long occurrences = myFixture.getEditor().getDocument().getText().split("counter", -1).length - 1;
    assertEquals(2, occurrences, "same-tree locals must resolve for completion");
  }

  // (defines, condition, whether the #if branch is active) - every row was
  // verified against haxe 4.3.7; rows the compiler hard-errors on (invalid
  // version literal, unknown function, non-literal argument, float-vs-version
  // compare) evaluate to an inactive branch here
  static final List<Arguments> CONDITION_EVALUATIONS = List.of(
    arguments("hl_ver=1.12.0", "(hl_ver >= version(\"1.12.0\"))", true),
    arguments("hl_ver=1.13.0", "(hl_ver >= version(\"1.12.0\"))", true),
    // a define VALUE wrapped in quotes (as typed into the overrides UI) de-quotes
    arguments("hl_ver=\"1.13.1\"", "(hl_ver >= version(\"1.12.0\"))", true),
    arguments("hl_ver=1.11.0", "(hl_ver >= version(\"1.12.0\"))", false),
    arguments("haxe=4.3.7", "(haxe >= version(\"4.1.0\"))", true),
    arguments("haxe_ver=4.307", "(haxe_ver >= 4.1)", true),
    // the compiler's numeric haxe_ver (4.3.7 is 4.307) compares at patch level
    arguments("haxe_ver=4.307", "(haxe_ver >= 4.305)", true),
    arguments("haxe_ver=4.307", "(haxe_ver >= 4.400)", false),
    arguments("v=1.2", "(v == 1.2)", true),
    arguments("", "(version(\"1.10.0\") > version(\"1.9.0\"))", true),
    arguments("", "(version(\"1.0.0-rc.1\") < version(\"1.0.0\"))", true),
    arguments("", "(version(\"1.0.0-alpha.2\") < version(\"1.0.0-alpha.10\"))", true),
    arguments("", "(undef_thing > 3)", false),
    // an undefined define is the compiler's TNull: != true, every other comparison false
    arguments("", "(undef_thing == false)", false),
    arguments("", "(undef_thing != \"x\")", true),
    arguments("hl_ver=1.12", "(hl_ver >= version(\"1.12.0\"))", false),
    arguments("", "(version(\"banana\") > version(\"1.0.0\"))", false),
    arguments("", "(version(\"1.0.0+build5\") == version(\"1.0.0\"))", false),
    arguments("", "(foo(\"x\"))", false),
    arguments("hl_ver=1.12.0", "(version(hl_ver) > version(\"1.0.0\"))", false),
    // an operator outside the condition grammar is a compiler error - inactive here
    arguments("A=1,B=2", "(B-A)", false),
    arguments("A=1,B=2", "(B > A)", true),
    // a '-' before a number is the number's sign, not an operation
    arguments("", "(-1)", true),
    arguments("v=-1", "(v == -1)", true),
    // a bare name is its define's value: only unset, 0 and "0" leave the branch inactive
    arguments("cpp=true", "(cpp)", true),
    arguments("cpp=\"2.1\"", "(cpp)", true),
    arguments("cpp=1.2", "(cpp)", true),
    arguments("cpp=\"0\"", "(cpp)", false),
    arguments("cpp=0", "(cpp)", false),
    arguments("", "(!cpp)", true),
    // a valued define compares against a version string
    arguments("myVersion=9.2.0", "(myVersion < \"9.0.0\")", false),
    // either operand set, any value; a parenthesized condition may span lines
    arguments("cpp", "(cpp || js)", true),
    arguments("js", "(cpp || js)", true),
    arguments("js,cpp", "(cpp || js)", true),
    arguments("js=1,cpp=2.3", "(cpp || js)", true),
    arguments("js=\"foo\"", "(cpp\n\t||\n\tjs)", true),
    // one operand unset or false
    arguments("cpp,js=false", "(cpp && js)", false),
    arguments("cpp=false,js", "(cpp && js)", false),
    arguments("cpp", "(cpp && js)", false),
    arguments("js", "(cpp && js)", false));

  @ParameterizedTest(name = "{1} with [{0}]")
  @FieldSource("CONDITION_EVALUATIONS")
  @DisplayName("condition evaluation matches the compiler")
  public void testConditionEvaluationMatchesTheCompiler(String defines, String condition, boolean active) {
    HaxeTestDefines.set(getProject(), defines);
    try {
      PsiFile file = myFixture.configureByText("Foo.hx", """
        class Foo {
        \t#if %s
        \tvar marker:Int;
        \t#end
        }""".formatted(condition));

      boolean branchLive = PsiTreeUtil.findChildOfType(file, HaxeInactiveBody.class) == null;
      assertEquals(active, branchLive, "wrong activeness for " + condition + " with defines [" + defines + "]");
    }
    finally {
      HaxeTestDefines.set(getProject(), null);
    }
  }

  @Test
  @DisplayName("conditions in dead regions stay quiet")
  public void testConditionsInDeadRegionsStayQuiet() {
    // the compiler never evaluates (or validates) a condition inside an
    // inactive outer region
    List<String> errors = conditionErrors("""
      class Foo {
      \t#if never
      \t#if (version("banana") > version("1.0.0"))
      \tvar marker:Int;
      \t#end
      \t#end
      }""");

    assertTrue(errors.isEmpty(), "skipped conditions are never validated: " + errors);
  }

  @Test
  @DisplayName("elseif after a taken branch stays quiet")
  public void testElseifAfterATakenBranchStaysQuiet() {
    List<String> errors = conditionErrors("""
      class Foo {
      \t#if (1 == 1)
      \tvar taken:Int;
      \t#elseif (version("banana") > version("1.0.0"))
      \tvar skipped:Int;
      \t#end
      }""");

    assertTrue(errors.isEmpty(), "a settled chain skips later conditions: " + errors);
  }

  @Test
  @DisplayName("elseif after an untaken branch is flagged")
  public void testElseifAfterAnUntakenBranchIsFlagged() {
    List<String> errors = conditionErrors("""
      class Foo {
      \t#if never
      \tvar untaken:Int;
      \t#elseif (version("banana") > version("1.0.0"))
      \tvar marker:Int;
      \t#end
      }""");

    boolean flagged = errors.stream().anyMatch(error -> error.contains("Should follow SemVer"));
    assertTrue(flagged, "the compiler evaluates and rejects this condition: " + errors);
  }

  // (condition as written after #if, its PPEXPRESSION run, the start of the inactive body after it)
  static final List<Arguments> CONDITION_RUNS = List.of(
    arguments("(utest-abc)", "(utest-abc)", "\n\tvar marker"),
    arguments("(B - A)", "(B - A)", "\n\tvar marker"),
    arguments("(a >>> b = c)", "(a >>> b = c)", "\n\tvar marker"),
    // without parentheses the condition is one token: '-flag' is already body
    arguments("my-flag", "my", "-flag\n\tvar marker"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("CONDITION_RUNS")
  @DisplayName("rejected operators stay inside a parenthesized condition")
  public void testRejectedOperatorsStayInsideAParenthesizedCondition(String condition, String run, String bodyStart) {
    String source = """
      class Foo {
      \t#if %s
      \tvar marker:Int;
      \t#end
      }""".formatted(condition);

    List<String> runs = conditionRuns(source);
    List<String> bodies = ppBodyTokens(source);

    assertEquals(List.of(run), runs, "the whole parenthesized expression is ONE condition run");
    assertTrue(bodies.get(0).startsWith(bodyStart), "the body starts right after the condition: " + bodies);
  }

  // (defines, condition, the ONE error the compiler reports for it - null when it compiles)
  static final List<Arguments> CONDITION_DIAGNOSTICS = List.of(
    // operators outside the condition grammar
    arguments("my-flag", "(my-flag)", "Unsupported operation: '-' is not allowed in a condition"),
    arguments("A=1,B=2", "(B-A)", "Unsupported operation: '-' is not allowed in a condition"),
    arguments("", "(a * b)", "Unsupported operation: '*' is not allowed in a condition"),
    arguments("", "(a >> b)", "Unsupported operation: '>>' is not allowed in a condition"),
    arguments("", "(-x)", "Invalid condition expression"),
    arguments("", "(~x)", "Invalid condition expression"),
    arguments("A=1,B=2", "(B > A)", null),
    arguments("", "(a == -1)", null),
    // without parentheses '-flag' is body, and the condition 'my' is fine
    arguments("my-flag", "my-flag", null),
    // invalid version literal
    arguments("", "(version(\"banana\") > version(\"1.0.0\"))", "Invalid version string \"banana\". Should follow SemVer."),
    // shortened version value suggests the full form
    arguments("hl_ver=1.13", "(hl_ver >= version(\"1.12.0\"))",
              "Invalid version \"1.13\": a version comparison needs all three major.minor.patch parts - did you mean \"1.13.0\"?"),
    // unknown function
    arguments("", "(foo(\"x\"))", "Invalid condition expression"),
    // undefined define comparisons stay quiet: a null-poisoned comparison is legal
    arguments("", "(undef_thing > 3)", null),
    // multi dot number stays quiet: no flex rule yields one token, so the
    // scanner declines it instead of crashing on Float.valueOf
    arguments("", "(foo == 1.2.3)", null));

  @ParameterizedTest(name = "{1} with [{0}]")
  @FieldSource("CONDITION_DIAGNOSTICS")
  @DisplayName("condition diagnostics match the compiler")
  public void testConditionDiagnosticsMatchTheCompiler(String defines, String condition, String expectedError) {
    HaxeTestDefines.set(getProject(), defines);
    try {
      List<String> errors = conditionErrors("""
        class Foo {
        \t#if %s
        \tvar marker:Int;
        \t#end
        }""".formatted(condition));

      List<String> expected = expectedError == null ? List.of() : List.of(expectedError);
      assertEquals(expected, errors, "wrong verdict for " + condition);
    }
    finally {
      HaxeTestDefines.set(getProject(), null);
    }
  }

  @Test
  @DisplayName("dead branch completion offers rich member elements")
  public void testDeadBranchCompletionOffersRichMemberElements() {
    // completion runs on a COPY with a dummy identifier at the caret, which
    // breaks the clean parse - the copy must keep the recovered parse
    // errors-and-all or the rich reference pipeline finds no reference and
    // only the flat word-completion fallback fills the popup
    myFixture.configureByText("Foo.hx", """
      class Foo {
      \t#if never
      \tpublic static function setClipboardText(text:String):Bool {
      \t\tt<caret>
      \t\treturn false;
      \t}
      \tpublic static function getClipboardText():String { return ""; }
      \t#end
      }""");

    LookupElement[] items = myFixture.completeBasic();
    assertNotNull(items, "several candidates must match the ambiguous prefix");

    boolean richMembers = Arrays.stream(items)
      .anyMatch(item -> item.getClass().getSimpleName().equals("HaxeMemberLookupElement"));
    assertTrue(richMembers, "typed member elements must appear, not just word-completion text");
  }

  @Test
  @DisplayName("dead members stay out of the stub tree")
  public void testDeadMembersStayOutOfTheStubTree() {
    PsiFile file = myFixture.configureByText("Foo.hx", """
      class Foo {
      \t#if never
      \tpublic function dead():Void {}
      \t#end
      \tpublic function live():Void {}
      }""");
    // force the chameleon to expand BEFORE stub building - the skip must hold even then
    assertNotNull(PsiTreeUtil.findChildOfType(file, HaxeInactiveBody.class).getFirstChild());

    var stubTree = ((PsiFileImpl)file).calcStubTree();
    boolean deadStubbed = stubTree.getPlainList().stream()
      .anyMatch(stub -> String.valueOf(stub).contains("dead"));
    assertFalse(deadStubbed, "inactive declarations must never reach the stub tree/indexes");
  }

  private List<String> ppBodyTokens(String source) {
    Lexer lexer = new HaxeLexer(getProject());
    lexer.start(source);
    List<String> bodies = new ArrayList<>();
    while (lexer.getTokenType() != null) {
      if (lexer.getTokenType() == HaxeTokenTypeSets.PPBODY) {
        bodies.add(lexer.getTokenText());
      }
      lexer.advance();
    }
    return bodies;
  }

  /** The text of each PPEXPRESSION run the parser lexer produces, surrounding whitespace stripped. */
  private List<String> conditionRuns(String source) {
    Lexer lexer = new HaxeLexer(getProject());
    lexer.start(source);
    List<String> runs = new ArrayList<>();
    StringBuilder run = null;
    while (lexer.getTokenType() != null) {
      if (lexer.getTokenType() == HaxeTokenTypeSets.PPEXPRESSION) {
        if (run == null) run = new StringBuilder();
        run.append(lexer.getTokenText());
      }
      else if (run != null) {
        runs.add(run.toString().strip());
        run = null;
      }
      lexer.advance();
    }
    return runs;
  }

  private HaxeInactiveBody inactiveBody(String source) {
    PsiFile file = myFixture.configureByText("Foo.hx", source);
    HaxeInactiveBody body = PsiTreeUtil.findChildOfType(file, HaxeInactiveBody.class);
    assertNotNull(body);
    return body;
  }

  private List<String> conditionErrors(String source) {
    myFixture.configureByText("Foo.hx", source);
    return myFixture.doHighlighting(HighlightSeverity.ERROR).stream()
      .map(info -> String.valueOf(info.getDescription()))
      .toList();
  }

  /** The markers of at least the severity that lie inside the configured source's #if..#end region. */
  private List<HighlightInfo> markersInsideTheConditional(HighlightSeverity minimumSeverity, String source) {
    int regionStart = source.indexOf("#if");
    int regionEnd = source.indexOf("#end");
    return myFixture.doHighlighting(minimumSeverity).stream()
      .filter(info -> info.getStartOffset() >= regionStart && info.getEndOffset() <= regionEnd)
      .toList();
  }
}
