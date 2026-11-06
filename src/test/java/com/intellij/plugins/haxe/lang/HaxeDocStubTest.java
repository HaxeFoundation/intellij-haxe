package com.intellij.plugins.haxe.lang;

import com.intellij.codeInsight.editorActions.FixDocCommentAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.documentation.providers.HaxeDocumentationStubProvider;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** The declaration and tag stub the stub provider offers the platform's doc comment actions. */
@DisplayName("Documentation: doc comment stub")
public class HaxeDocStubTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/editor/";
  }

  @Test
  @DisplayName("the stub of a haxedoc comment has bare tag lines")
  public void testTheStubOfAHaxedocCommentHasBareTagLines() {
    configure("""
      class Test {
          /**
              Text.
          **/
          function foo(a:Int):Int { return a; }
      }
      """);

    String stub = new HaxeDocumentationStubProvider().documentationStub(declaration(HaxeMethod.class));

    assertEquals("@param a\n@return\n", stub);
  }

  @Test
  @DisplayName("the stub of a javadoc comment stars its tag lines")
  public void testTheStubOfAJavadocCommentStarsItsTagLines() {
    configure("""
      class Test {
          /**
           * Text.
           */
          function foo(a:Int):Int { return a; }
      }
      """);

    String stub = new HaxeDocumentationStubProvider().documentationStub(declaration(HaxeMethod.class));

    assertEquals("* @param a\n* @return\n", stub);
  }

  @Test
  @DisplayName("a field's stub is empty")
  public void testAFieldsStubIsEmpty() {
    configure("""
      class Test {
          /** Text. **/
          var foo:Int;
      }
      """);

    String stub = new HaxeDocumentationStubProvider().documentationStub(declaration(HaxeFieldDeclaration.class));

    assertEquals("", stub);
  }

  @Test
  @DisplayName("the documented element around a parameter is its method")
  public void testTheDocumentedElementAroundAParameterIsItsMethod() {
    configure("""
      class Test {
          function foo(a<caret>:Int):Void {}
      }
      """);

    PsiElement documented = new HaxeDocumentationStubProvider().findDocumentedElement(elementAtCaret());

    HaxeMethod method = assertInstanceOf(HaxeMethod.class, documented);
    assertEquals("foo", method.getName());
  }

  @Test
  @DisplayName("fix doc comment documents a method without a comment")
  public void testFixDocCommentDocumentsAMethodWithoutAComment() {
    configure("""
      class Test {
          function f<caret>oo(a:Int):Int { return a; }
      }
      """);

    FixDocCommentAction.generateOrFixComment(elementAtCaret(), getProject(), myFixture.getEditor());

    myFixture.checkResult("""
      class Test {
          /**
           * <caret>
           * @param a
           * @return
           **/
          function foo(a:Int):Int { return a; }
      }
      """);
  }

  private void configure(String source) {
    myFixture.configureByText("Test.hx", source);
  }

  private <T extends PsiElement> T declaration(Class<T> type) {
    return PsiTreeUtil.findChildOfType(myFixture.getFile(), type);
  }

  private PsiElement elementAtCaret() {
    return myFixture.getFile().findElementAt(myFixture.getCaretOffset());
  }
}
