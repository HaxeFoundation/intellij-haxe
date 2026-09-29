package com.intellij.plugins.haxe.lang.completion;

import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.fixtures.CompletionAutoPopupTester;
import com.intellij.testFramework.junit5.RunInEdt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// the auto-popup tester waits for the popup off the EDT; setUp/tearDown then run off it too
@RunInEdt(allMethods = false, writeIntent = true)
@DisplayName("Completion: comment auto-popup")
public class CommentAutoPopupTest extends HaxeCompletionTestBase {
  public CommentAutoPopupTest() {
    super("completion");
  }

  /** (position, source with the caret before the typed character, typed character, popup expected). */
  static final List<Arguments> POSITIONS = List.of(
    arguments("dot ending a line comment", "// prose<caret>\nclass A {}", '.', false),
    arguments("letter in a line comment", "// prose <caret>\nclass A {}", 'p', false),
    arguments("dot ending a doc comment line", "/**\n  prose<caret>\n**/\nclass A {}", '.', false),
    arguments("dot in code", "class A { function foo(a:A) { a<caret> } }", '.', true),
    // comment-shaped for the platform, but still code
    arguments("dot in an inactive branch", "class A { function foo(a:A) {\n#if never\na<caret>\n#end\n} }", '.', true));

  @ParameterizedTest(name = "{0}")
  @FieldSource("POSITIONS")
  @DisplayName("auto-popup only outside comments")
  public void testAutoPopupOnlyOutsideComments(String position, String source, char typed, boolean popupExpected) throws Throwable {
    CompletionAutoPopupTester tester = new CompletionAutoPopupTester(myFixture);
    EdtTestUtil.runInEdtAndWait(() -> configureFileByText("A.hx", source));

    tester.runWithAutoPopupEnabled(() -> tester.typeWithPauses(String.valueOf(typed)));

    boolean popupShown = tester.getLookup() != null;
    assertEquals(popupExpected, popupShown);
  }
}
