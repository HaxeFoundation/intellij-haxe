package com.intellij.plugins.haxe.ide.editor;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("Editor: color provider")
public class HaxeColorProviderTest extends HaxeLightFixtureTestCase {
  private final HaxeColorProvider provider = new HaxeColorProvider();

  @Override
  protected String getBasePath() {
    return "/editor/";
  }

  @Test
  @DisplayName("six digit hex literal is a color")
  public void testSixDigitHexLiteralIsAColor() {
    PsiElement literal = literalAtCaret("class Main { var c = 0xFF<caret>8800; }");

    assertEquals(new Color(0xff8800), provider.getColorFrom(literal));
  }

  @Test
  @DisplayName("other literals are not colors")
  public void testOtherLiteralsAreNotColors() {
    assertNull(provider.getColorFrom(literalAtCaret("class Main { var c = 0x<caret>ff; }")));
    assertNull(provider.getColorFrom(literalAtCaret("class Main { var c = 0x<caret>80ff8800; }")));
    assertNull(provider.getColorFrom(literalAtCaret("class Main { var c = 16<caret>7772; }")));
  }

  @Test
  @DisplayName("picked color replaces the literal and keeps the swatch")
  public void testPickedColorReplacesTheLiteralAndKeepsTheSwatch() {
    PsiElement literal = literalAtCaret("class Main { var c = 0xff<caret>0000; }");

    WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> provider.setColorTo(literal, new Color(0x0000ff)));

    myFixture.checkResult("class Main { var c = 0x0000ff; }");
    assertEquals(new Color(0x0000ff), provider.getColorFrom(literalAtCaret()));
  }

  @Test
  @DisplayName("the picker can change the color repeatedly through the element it opened on")
  public void testThePickerCanChangeTheColorRepeatedlyThroughTheElementItOpenedOn() {
    PsiElement literal = literalAtCaret("class Main { var c = 0xff<caret>0000; }");

    WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> provider.setColorTo(literal, new Color(0x00ff00)));
    WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> provider.setColorTo(literal, new Color(0x0000ff)));

    assertFalse(literal.isValid(), "the picker keeps handing over the first literal, which the first pick replaced");
    myFixture.checkResult("class Main { var c = 0x0000ff; }");
  }

  private PsiElement literalAtCaret(String source) {
    myFixture.configureByText("Main.hx", source);
    return literalAtCaret();
  }

  private PsiElement literalAtCaret() {
    return myFixture.getFile().findElementAt(myFixture.getCaretOffset());
  }
}
