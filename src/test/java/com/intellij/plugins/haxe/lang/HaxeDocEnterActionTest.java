package com.intellij.plugins.haxe.lang;

import com.intellij.openapi.editor.Document;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.documentation.settings.HaxeDocSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enter inside a doc comment continues at the PREVIOUS line's indentation,
 * preserving hand-aligned haxedoc layouts (tag description columns).
 */
@DisplayName("Editor: doc comment enter")
public class HaxeDocEnterActionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/editor/";
  }

  @Test
  @DisplayName("enter continues at the previous lines indentation")
  public void testEnterContinuesAtThePreviousLinesIndentation() {
    String indent = newLineIndent("""
      class Foo {
      \t/**
      \t\tThe ID of the call<caret>
      \t**/
      \tfunction f():Void {}
      }""");

    assertEquals("\t\t", indent);
  }

  @Test
  @DisplayName("hand aligned continuation columns are preserved")
  public void testHandAlignedContinuationColumnsArePreserved() {
    String indent = newLineIndent("""
      class Foo {
      \t/**
      \t\t@param\tid\tThe ID of the call, set
      \t\t\t\t\tto a variable<caret>
      \t**/
      \tfunction f(id:Int):Void {}
      }""");

    assertEquals("\t\t\t\t\t", indent, "the description column must carry to the next line");
  }

  @Test
  @DisplayName("toggle off falls back to the platform indent")
  public void testToggleOffFallsBackToThePlatformIndent() {
    HaxeDocSettings.State state = HaxeDocSettings.getInstance().getState();
    state.enterKeepsIndentation = false;
    try {
      String indent = newLineIndent("""
        class Foo {
        \t/**
        \t\t@param\tid\tThe ID of the call, set
        \t\t\t\t\tto a variable<caret>
        \t**/
        \tfunction f(id:Int):Void {}
        }""");

      assertNotEquals("\t\t\t\t\t", indent, "with the toggle off the hand alignment must not be forced");
    }
    finally {
      state.enterKeepsIndentation = true;
    }
  }

  /** The caret line's leading whitespace after typing enter at the given fixture. */
  private String newLineIndent(String source) {
    myFixture.configureByText("Doc.hx", source);
    myFixture.type('\n');
    Document document = myFixture.getEditor().getDocument();
    int offset = myFixture.getCaretOffset();
    int lineStart = document.getLineStartOffset(document.getLineNumber(offset));
    return document.getText().substring(lineStart, offset);
  }
}
