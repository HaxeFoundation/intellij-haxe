package com.intellij.plugins.haxe.resolve;

import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumExtractedValueReference;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueDeclarationConstructor;
import com.intellij.plugins.haxe.lang.psi.HaxeEnumValueReference;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeSwitchCaseCapture;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A position in a switch pattern is typed from what the switch matches,
 * walked down the pattern: an array element, an object pattern's field, an
 * enum constructor's argument. hxparse's stream DSL ({@code switch stream}
 * in a {@code hxparse.ParserBuilder} class) matches the parser's token list,
 * and a capture with a call binds the call's result.
 */
@DisplayName("Resolve: pattern matched type")
public class HaxePatternMatchedTypeTest extends HaxeToolkitLightFixtureTestCase {

  private static final String TOKEN_SOURCE_HX_SOURCE = """
    package hxparse;
    interface TokenSource<Token> {
      function token():Token;
    }
    """;

  private static final String PARSER_HX_SOURCE = """
    package hxparse;
    class Parser<S:TokenSource<Token>, Token> {
      var stream:S;
      public function new(stream:S) {
        this.stream = stream;
      }
      function peek(n:Int):Token {
        return stream.token();
      }
    }
    """;

  private static final String PARSER_BUILDER_HX_SOURCE = """
    package hxparse;
    interface ParserBuilder {}
    """;

  private static final String TOKENS_HX_SOURCE = """
    class Position {
      public var line:Int;
      public function new(line:Int) {
        this.line = line;
      }
    }
    enum Keyword {
      KwdIf;
      KwdElse;
    }
    enum Constant {
      CIdent(s:String);
      CInt(i:String, s:String);
    }
    enum TokenDef {
      Const(c:Constant);
      Kwd(k:Keyword);
      POpen;
      PClose;
    }
    class Token {
      public var tok:TokenDef;
      public var pos:Position;
      public function new(tok:TokenDef, pos:Position) {
        this.tok = tok;
        this.pos = pos;
      }
    }
    class Source implements hxparse.TokenSource<Token> {
      public function new() {}
      public function token():Token {
        return null;
      }
    }
    """;

  private static final String COND_PARSER_HX_SOURCE = """
    import Tokens;
    class CondParser extends hxparse.Parser<Source, Token> implements hxparse.ParserBuilder {
      public function new(stream:Source) {
        super(stream);
      }
      function next():Position {
        return null;
      }
      function parse():String {
        return switch stream {
          case [{tok:Const(CIdent(t)), pos:p}]: t;
          case [{tok:Kwd(k), pos:p}, o = next()]: "";
          case [{tok:POpen, pos:p1}, o = next(), {tok:PClose, pos:p2}]: "";
        }
      }
    }
    """;

  private static final String ARRAY_PATTERNS_HX_SOURCE = """
    import Tokens;
    class ArrayPatterns {
      static function first(tokens:Array<Token>):String {
        return switch tokens {
          case [{tok:Const(CIdent(t)), pos:p}]: t;
          case _: "";
        }
      }
      static function sum(numbers:Array<Int>):Int {
        return switch numbers {
          case [a, b]: a + b;
          case _: 0;
        }
      }
      static function parenthesis(tok:TokenDef):Bool {
        return switch tok {
          case POpen | PClose: true;
          case _: false;
        }
      }
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @BeforeEach
  void addFixtureFiles() {
    myFixture.addFileToProject("hxparse/TokenSource.hx", TOKEN_SOURCE_HX_SOURCE);
    myFixture.addFileToProject("hxparse/Parser.hx", PARSER_HX_SOURCE);
    myFixture.addFileToProject("hxparse/ParserBuilder.hx", PARSER_BUILDER_HX_SOURCE);
    myFixture.addFileToProject("Tokens.hx", TOKENS_HX_SOURCE);
  }

  @Test
  @DisplayName("a stream patterns field capture types as the tokens field")
  public void testAStreamPatternsFieldCaptureTypesAsTheTokensField() {
    PsiFile parser = myFixture.addFileToProject("CondParser.hx", COND_PARSER_HX_SOURCE);

    assertEquals("Position", typeOf(extractedValue(parser, "p")));
  }

  @Test
  @DisplayName("a stream patterns nested constructor capture types as the constructors parameter")
  public void testAStreamPatternsNestedConstructorCaptureTypesAsTheConstructorsParameter() {
    PsiFile parser = myFixture.addFileToProject("CondParser.hx", COND_PARSER_HX_SOURCE);

    PsiElement constructor = enumValueReference(parser, "CIdent").getReferenceExpression().resolve();

    assertEquals("CIdent", assertInstanceOf(HaxeEnumValueDeclarationConstructor.class, constructor).getName());
    assertEquals("String", typeOf(extractedValue(parser, "t")));
  }

  @Test
  @DisplayName("a stream capture with a call binds the calls result")
  public void testAStreamCaptureWithACallBindsTheCallsResult() {
    PsiFile parser = myFixture.addFileToProject("CondParser.hx", COND_PARSER_HX_SOURCE);

    HaxeSwitchCaseCapture capture = PsiTreeUtil.findChildrenOfType(parser, HaxeSwitchCaseCapture.class).stream()
      .filter(c -> c.getText().startsWith("o = next()"))
      .findFirst()
      .orElseThrow();

    assertEquals("Position", typeOf(capture));
  }

  @Test
  @DisplayName("an array patterns field capture types as the elements field")
  public void testAnArrayPatternsFieldCaptureTypesAsTheElementsField() {
    PsiFile patterns = myFixture.addFileToProject("ArrayPatterns.hx", ARRAY_PATTERNS_HX_SOURCE);

    assertEquals("Position", typeOf(extractedValue(patterns, "p")));
    assertEquals("String", typeOf(extractedValue(patterns, "t")));
  }

  @Test
  @DisplayName("an array patterns element capture types as the element")
  public void testAnArrayPatternsElementCaptureTypesAsTheElement() {
    PsiFile patterns = myFixture.addFileToProject("ArrayPatterns.hx", ARRAY_PATTERNS_HX_SOURCE);

    assertEquals("Int", typeOf(extractedValue(patterns, "a")));
  }

  /** A bare enum value has no extractor around it; the enum it names is the one the switch matches. */
  @Test
  @DisplayName("a bare enum value resolves in the matched enum")
  public void testABareEnumValueResolvesInTheMatchedEnum() {
    PsiFile patterns = myFixture.addFileToProject("ArrayPatterns.hx", ARRAY_PATTERNS_HX_SOURCE);

    PsiElement resolved = referenceExpression(patterns, "PClose").resolve();

    assertEquals("PClose", assertInstanceOf(HaxeEnumValueDeclaration.class, resolved).getName());
  }

  private static String typeOf(PsiElement element) {
    return HaxeExpressionEvaluator.evaluate(element).result.toStringWithoutConstant();
  }

  private static HaxeEnumExtractedValueReference extractedValue(PsiFile file, String name) {
    return firstWithText(PsiTreeUtil.findChildrenOfType(file, HaxeEnumExtractedValueReference.class), name);
  }

  private static HaxeEnumValueReference enumValueReference(PsiFile file, String name) {
    return firstWithText(PsiTreeUtil.findChildrenOfType(file, HaxeEnumValueReference.class), name);
  }

  private static HaxeReferenceExpression referenceExpression(PsiFile file, String name) {
    return firstWithText(PsiTreeUtil.findChildrenOfType(file, HaxeReferenceExpression.class), name);
  }

  private static <T extends PsiElement> T firstWithText(Collection<T> elements, String text) {
    T element = elements.stream().filter(e -> e.textMatches(text)).findFirst().orElse(null);
    assertNotNull(element, "'" + text + "' in the fixture");
    return element;
  }
}
