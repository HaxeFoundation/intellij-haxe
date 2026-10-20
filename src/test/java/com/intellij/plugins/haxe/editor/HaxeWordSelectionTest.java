package com.intellij.plugins.haxe.editor;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** The first step of select-word (Ctrl+W) - the same handler chain a double-click runs for its caret. */
@DisplayName("Editor: word selection")
public class HaxeWordSelectionTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /** (source with the caret inside a comment word, the word the selection covers). */
  static final List<Arguments> COMMENT_WORDS = List.of(
    // double click in a line comment selects the word
    arguments("""
      class Main {
        function f() {
          // keep the list, t<caret>he item is the first entry
        }
      }""", "the"),
    // double click in a doc comment selects the word
    arguments("""
      class Main {
        /**
          Returns the items th<caret>at were added to this list.
        **/
        function f() {}
      }""", "that"),
    // double click in a block comment selects the word
    arguments("""
      class Main {
        /* a bl<caret>ock comment */
        function f() {}
      }""", "block"));

  @ParameterizedTest(name = "{1}")
  @FieldSource("COMMENT_WORDS")
  @DisplayName("double click in a comment selects the word")
  public void testDoubleClickInACommentSelectsTheWord(String source, String expected) {
    myFixture.configureByText("Main.hx", source);

    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET);

    assertEquals(expected, myFixture.getEditor().getSelectionModel().getSelectedText());
  }
}
