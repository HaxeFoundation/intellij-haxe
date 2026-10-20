package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.plugins.haxe.util.HaxeDocumentationUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Indent stripping for rendered docs follows the doc lexer's rule: blank
 * (whitespace-only) lines and hard-wrapped column-0 lines never drag the
 * common prefix down - leftover indentation would render every paragraph
 * as a markdown code block.
 */
@DisplayName("Documentation: rendered doc indent stripping")
public class HaxeDocRenderingIndentTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/parsing/";
  }

  @Test
  @DisplayName("indented blank line before the closer does not leave a code block")
  public void testIndentedBlankLineBeforeTheCloserDoesNotLeaveACodeBlock() {
    String docs = docsOf("""
      class Foo {
      \t/**
      \t\tCreates a new instance.

      \t\t@param\tid\tthe id
      \t
      \t**/
      \tfunction f(id:Int):Void {}
      }""");

    boolean anyIndented = docs.lines().anyMatch(line -> line.startsWith("\t") || line.startsWith("    "));
    assertFalse(anyIndented, "no line may keep code-block indentation:\n" + docs);
    assertTrue(docs.startsWith("Creates a new instance."), "the body must strip to column 0:\n" + docs);
  }

  @Test
  @DisplayName("indented blank line between paragraphs strips to empty")
  public void testIndentedBlankLineBetweenParagraphsStripsToEmpty() {
    String docs = docsOf("""
      class Foo {
      \t/**
      \t\tFirst paragraph.
      \t\t\t
      \t\tSecond paragraph.
      \t**/
      \tfunction f():Void {}
      }""");

    assertEquals("First paragraph.\n\nSecond paragraph.", docs);
  }

  @Test
  @DisplayName("column zero wrapped line does not drag the prefix")
  public void testColumnZeroWrappedLineDoesNotDragThePrefix() {
    String docs = docsOf("""
      class Foo {
      \t/**
      \t\tA description that is hard-wrapped
      at column zero.
      \t**/
      \tfunction f():Void {}
      }""");

    assertEquals("A description that is hard-wrapped\nat column zero.", docs);
  }

  @Test
  @DisplayName("markdown depth beyond the prefix survives")
  public void testMarkdownDepthBeyondThePrefixSurvives() {
    String docs = docsOf("""
      class Foo {
      \t/**
      \t\tA list:
      \t\t- item
      \t\t\t- nested
      \t**/
      \tfunction f():Void {}
      }""");

    assertEquals("A list:\n- item\n\t- nested", docs, "author-chosen depth is markdown meaning");
  }

  @Test
  @DisplayName("compiler doc body strips like a comment's")
  public void testCompilerDocBodyStripsLikeAComments() {
    // display/completion sends the text between the delimiters, indentation and all
    String body = "\n\t\tReturns the absolute value of `v`.\n\n\t\t- If `v` is positive, the result is unchanged.\n\t";

    String docs = HaxeDocumentationUtil.stripForRendering(body);

    assertEquals("Returns the absolute value of `v`.\n\n- If `v` is positive, the result is unchanged.", docs);
  }

  private String docsOf(String source) {
    PsiFile file = myFixture.configureByText("Doc.hx", source);
    HaxePsiDocCommentImpl docComment = PsiTreeUtil.findChildOfType(file, HaxePsiDocCommentImpl.class);
    assertNotNull(docComment);
    return docComment.getDocsWithoutIndents();
  }
}
