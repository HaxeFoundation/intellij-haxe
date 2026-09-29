package com.intellij.plugins.haxe.lang;

import com.intellij.lexer.Lexer;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.lexer.HaxeDocLexer;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The doc comment chameleon: one flex token, lazily parsed into line-oriented
 * sub-tokens where only the managed leading whitespace is WHITE_SPACE - deeper
 * markdown indentation and trailing spaces stay inside DOC_DATA.
 */
@DisplayName("Parsing: doc comment sub-tree")
public class HaxeDocCommentPsiTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/parsing/";
  }

  @Test
  @DisplayName("tokens roundtrip to the original text")
  public void testTokensRoundtripToTheOriginalText() {
    List<String> samples = List.of(
      "/**/", "/***/", "/** one line **/", "/** unclosed",
      "/**\n\t\tbody\n\t**/",
      "/**\n\t * starred\n\t */",
      "/**\n\t\t@param\tx\tvalue\nwrapped at column zero\n\t**/",
      "/**\r\n\t\twindows line ends\r\n\t**/");
    for (String sample : samples) {
      assertEquals(sample, concat(lex(sample)), "lost characters lexing: " + sample);
    }
  }

  @Test
  @DisplayName("haxedoc style manages only the common prefix")
  public void testHaxedocStyleManagesOnlyTheCommonPrefix() {
    List<Token> tokens = lex("""
      /**
      \t\tBody line.
      \t\t\tdeeper markdown
      column zero
      \t**/""");

    Token body = tokens.stream().filter(t -> t.text().equals("Body line.")).findFirst().orElseThrow();
    assertEquals(DOC_DATA, body.type(), "body text is opaque data");
    boolean deeperKeepsDepth = tokens.stream().anyMatch(t -> t.type() == DOC_DATA && t.text().equals("\tdeeper markdown"));
    assertTrue(deeperKeepsDepth, "depth beyond the common prefix stays inside the data token");
    boolean columnZeroBare = tokens.stream().anyMatch(t -> t.type() == DOC_DATA && t.text().equals("column zero"));
    assertTrue(columnZeroBare, "a column-zero line has no managed whitespace of its own");
    assertEquals(DOC_END, tokens.get(tokens.size() - 1).type());
    assertEquals("**/", tokens.get(tokens.size() - 1).text());
  }

  @Test
  @DisplayName("javadoc style tokenizes the leading asterisks")
  public void testJavadocStyleTokenizesTheLeadingAsterisks() {
    List<Token> tokens = lex("""
      /**
       * Body line.
       * @param x value
       */""");

    long asterisks = tokens.stream().filter(t -> t.type() == DOC_LEADING_ASTERISK).count();
    assertEquals(2, asterisks, "each content line's star is its own token");
    boolean tagFound = tokens.stream().anyMatch(t -> t.type() == DOC_TAG_NAME && t.text().equals("@param"));
    assertTrue(tagFound);
    assertEquals("*/", tokens.get(tokens.size() - 1).text());
  }

  @Test
  @DisplayName("trailing spaces stay inside the data token")
  public void testTrailingSpacesStayInsideTheDataToken() {
    List<Token> tokens = lex("/**\n\t\thard break  \n\t\tnext line\n\t**/");

    boolean hardBreakKept = tokens.stream().anyMatch(t -> t.type() == DOC_DATA && t.text().equals("hard break  "));
    assertTrue(hardBreakKept, "markdown hard breaks (trailing double space) must never become WHITE_SPACE");
  }

  @Test
  @DisplayName("doc comment parses as a lazy composite comment")
  public void testDocCommentParsesAsALazyCompositeComment() {
    PsiFile file = myFixture.configureByText("Foo.hx", """
      class Foo {
      \t/**
      \t\tdocs with a tag
      \t\t@param x value
      \t**/
      \tfunction f(x:Int):Void {}
      }""");

    PsiComment docComment = PsiTreeUtil.findChildrenOfType(file, PsiComment.class).stream()
      .filter(comment -> comment.getTokenType() == HaxeTokenTypeSets.DOC_COMMENT)
      .findFirst()
      .orElseThrow();
    assertInstanceOf(HaxePsiDocCommentImpl.class, docComment);

    var children = docComment.getNode().getChildren(null);
    assertTrue(children.length > 1, "the chameleon must expand into sub-tokens");
    assertEquals(DOC_START, children[0].getElementType());
    boolean managedWhitespace = false;
    boolean tagTokenized = false;
    for (var child : children) {
      if (child.getElementType() == TokenType.WHITE_SPACE) managedWhitespace = true;
      if (child.getElementType() == DOC_TAG_NAME) tagTokenized = true;
    }
    assertTrue(managedWhitespace, "line-leading whitespace must be real WHITE_SPACE for the formatter");
    assertTrue(tagTokenized);
  }

  private static List<Token> lex(String docText) {
    Lexer lexer = new HaxeDocLexer();
    lexer.start(docText, 0, docText.length(), 0);
    List<Token> tokens = new ArrayList<>();
    while (lexer.getTokenType() != null) {
      tokens.add(new Token(lexer.getTokenType(), docText.substring(lexer.getTokenStart(), lexer.getTokenEnd())));
      lexer.advance();
    }
    return tokens;
  }

  private static String concat(List<Token> tokens) {
    return tokens.stream().map(Token::text).reduce("", String::concat);
  }

  private record Token(IElementType type, String text) {
  }
}
