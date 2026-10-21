package com.intellij.plugins.haxe.lang.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.lookup.HaxeMemberLookupElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The lookup's type column is produced by the EXPENSIVE renderer on a
 * background thread against the completion COPY of the file - a context the
 * inlay tests never exercise. These tests render elements exactly the way
 * the popup does: cheap presentation first, then the expensive upgrade.
 */
@DisplayName("Completion: lookup type text")
public class HaxeCompletionTypeTextTest extends HaxeToolkitLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/completion/";
  }

  @Test
  @DisplayName("local typed through untyped parameters gets type text")
  public void testLocalFromUntypedParameters() {
    myFixture.configureByText("Test.hx", """
      class Test {
          public static function main() {
              span(3, 9);
          }
          static function span(from, to) {
              var total = to - from;
              var totalCount = 1;
              tot<caret>
          }
      }""");

    myFixture.completeBasic();

    LookupElementPresentation presentation = renderFully(findElement("total"));
    assertEquals("Int", presentation.getTypeText(),
                 "the local's type flows through call-site-typed parameters and must survive the completion copy");
  }

  @Test
  @DisplayName("uninitialized local typed by later assignments gets type text")
  public void testUninitializedLocalTypedByAssignment() {
    myFixture.configureByText("Test.hx", """
      class Test {
          public static function main() {
              span(3, 9);
          }
          static function pick(m, n) {
              return m;
          }
          static function span(from, to) {
              var total, totalCount;
              if (from > 0) {
                  total = to - from;
              } else {
                  total = pick(from, to);
              }
              totalCount = 1;
              tot<caret>
          }
      }""");

    myFixture.completeBasic();

    LookupElementPresentation presentation = renderFully(findElement("total"));
    assertEquals("Int", presentation.getTypeText(),
                 "an uninitialized local typed by assignments (ArraySort's first_cut shape) must show its type");
  }

  @SuppressWarnings("unchecked")
  private LookupElementPresentation renderFully(LookupElement element) {
    LookupElementPresentation presentation = new LookupElementPresentation();
    LookupElementRenderer<LookupElement> expensive = (LookupElementRenderer<LookupElement>)element.getExpensiveRenderer();
    assertNotNull(expensive, "member lookup elements must provide the expensive renderer");
    expensive.renderElement(element, presentation);
    return presentation;
  }

  private LookupElement findElement(String lookupString) {
    LookupElement[] elements = myFixture.getLookupElements();
    assertNotNull(elements, "completion produced no lookup");
    for (LookupElement element : elements) {
      if (element instanceof HaxeMemberLookupElement && element.getLookupString().equals(lookupString)) {
        return element;
      }
    }
    fail("no member lookup element for '" + lookupString + "'");
    return null;
  }
}
