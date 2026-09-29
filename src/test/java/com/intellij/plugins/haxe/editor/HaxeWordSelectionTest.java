package com.intellij.plugins.haxe.editor;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The first step of select-word (Ctrl+W) - the same handler chain a double-click runs for its caret. */
@DisplayName("Editor: word selection")
public class HaxeWordSelectionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/editor/";
  }

  @Test
  @DisplayName("double click in a line comment selects the word")
  public void testDoubleClickInALineCommentSelectsTheWord() {
    assertSelectsWord("""
      class Main {
        function f() {
          // keep the list, t<caret>he item is the first entry
        }
      }""", "the");
  }

  @Test
  @DisplayName("double click in a doc comment selects the word")
  public void testDoubleClickInADocCommentSelectsTheWord() {
    assertSelectsWord("""
      class Main {
        /**
          Returns the items th<caret>at were added to this list.
        **/
        function f() {}
      }""", "that");
  }

  @Test
  @DisplayName("double click in a block comment selects the word")
  public void testDoubleClickInABlockCommentSelectsTheWord() {
    assertSelectsWord("""
      class Main {
        /* a bl<caret>ock comment */
        function f() {}
      }""", "block");
  }

  private void assertSelectsWord(String source, String expected) {
    myFixture.configureByText("Main.hx", source);
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET);
    assertEquals(expected, myFixture.getEditor().getSelectionModel().getSelectedText());
  }
}
