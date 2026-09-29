package com.intellij.plugins.haxe.lang;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeStringLiteralImpl;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parsing: string literal manipulator")
public class HaxeStringLiteralManipulatorTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/parsing/";
  }

  @Test
  @DisplayName("content change keeps the quotes")
  public void testContentChangeKeepsTheQuotes() {
    PsiFile file = myFixture.configureByText("Main.hx", "class Main { var text = \"abc\"; }");
    HaxeStringLiteralImpl literal = PsiTreeUtil.findChildOfType(file, HaxeStringLiteralImpl.class);
    assertNotNull(literal, "the fixture holds a string literal");

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      ElementManipulators.handleContentChange(literal, "xyz");
    });

    assertEquals("\"xyz\"", literal.getText());
  }

  @Test
  @DisplayName("partial content change keeps the rest")
  public void testPartialContentChangeKeepsTheRest() {
    PsiFile file = myFixture.configureByText("Main.hx", "class Main { var text = 'one two'; }");
    HaxeStringLiteralImpl literal = PsiTreeUtil.findChildOfType(file, HaxeStringLiteralImpl.class);
    assertNotNull(literal, "the fixture holds a string literal");

    TextRange secondWord = new TextRange(5, 8);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      ElementManipulators.getManipulator(literal).handleContentChange(literal, secondWord, "2");
    });

    assertEquals("'one 2'", literal.getText());
  }
}
