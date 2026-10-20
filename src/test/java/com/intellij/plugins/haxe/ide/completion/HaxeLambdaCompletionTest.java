package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLightProjectDescriptors;
import com.intellij.testFramework.LightProjectDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Completion: lambda at an expected function")
public class HaxeLambdaCompletionTest extends HaxeLightFixtureTestCase {

  private static final String FILTER_ARGUMENT_SOURCE = """
    class Main {
    	static function main() {
    		[1, 2, 3].filter(<caret>);
    	}
    }
    """;
  private static final String NAMED_PARAMETERS_SOURCE = """
    class Main {
    	static function run(callback:(count:Int, label:String) -> Void) {}
    	static function main() {
    		run(<caret>);
    	}
    }
    """;
  private static final String INT_ARGUMENT_SOURCE = """
    class Main {
    	static function take(n:Int) {}
    	static function main() {
    		take(<caret>);
    	}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The array case needs the std's Array, whose filter parameter is generic. */
  @Override
  protected LightProjectDescriptor lightProjectDescriptor() {
    return HaxeLightProjectDescriptors.WITH_TOOLKIT;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // keeps an inserted template interactive, as the editor does, instead of finishing it at once
    TemplateManagerImpl.setTemplateTesting(getTestRootDisposable());
  }

  @Test
  @DisplayName("a function argument offers an arrow and a function literal")
  public void testAFunctionArgumentOffersAnArrowAndAFunctionLiteral() {
    myFixture.configureByText("Main.hx", FILTER_ARGUMENT_SOURCE);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();

    assertNotNull(lookups);
    assertTrue(lookups.contains("i ->"), "an unnamed Int parameter gets the suggester's conventional name: " + lookups);
    assertTrue(lookups.contains("function(i) {}"), lookups.toString());
  }

  @Test
  @DisplayName("the signature's parameter names shape the lambda")
  public void testTheSignaturesParameterNamesShapeTheLambda() {
    myFixture.configureByText("Main.hx", NAMED_PARAMETERS_SOURCE);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();

    assertNotNull(lookups);
    assertTrue(lookups.contains("(count, label) ->"), lookups.toString());
    assertTrue(lookups.contains("function(count, label) {}"), lookups.toString());
  }

  @Test
  @DisplayName("a non function argument offers no lambda")
  public void testANonFunctionArgumentOffersNoLambda() {
    myFixture.configureByText("Main.hx", INT_ARGUMENT_SOURCE);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();

    boolean lambdaOffered = lookups != null && lookups.stream().anyMatch(lookup -> lookup.endsWith("->") || lookup.startsWith("function("));
    assertFalse(lambdaOffered, "no lambda where an Int is expected: " + lookups);
  }

  @Test
  @DisplayName("choosing the arrow inserts it with the parameter name")
  public void testChoosingTheArrowInsertsItWithTheParameterName() {
    myFixture.configureByText("Main.hx", FILTER_ARGUMENT_SOURCE);
    LookupElement[] elements = myFixture.completeBasic();
    LookupElement arrow = Arrays.stream(elements)
      .filter(element -> "i ->".equals(element.getLookupString()))
      .findFirst()
      .orElseThrow();

    myFixture.getLookup().setCurrentItem(arrow);
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);

    String text = myFixture.getEditor().getDocument().getText();
    assertTrue(text.contains("[1, 2, 3].filter(i -> )"), text);
    assertTrue(HaxeLambdaLookups.isLambdaTemplateActive(getProject(), myFixture.getEditor().getDocument()),
               "the parameter stop is being filled in");
  }

  @Test
  @DisplayName("completion inside the parameter stop offers no second lambda")
  public void testCompletionInsideTheParameterStopOffersNoSecondLambda() {
    myFixture.configureByText("Main.hx", FILTER_ARGUMENT_SOURCE);
    LookupElement[] elements = myFixture.completeBasic();
    LookupElement arrow = Arrays.stream(elements)
      .filter(element -> "i ->".equals(element.getLookupString()))
      .findFirst()
      .orElseThrow();
    myFixture.getLookup().setCurrentItem(arrow);
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);

    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();

    boolean lambdaOffered = lookups != null && lookups.stream().anyMatch(lookup -> lookup.endsWith("->") || lookup.startsWith("function("));
    assertFalse(lambdaOffered, "no lambda while its parameter stop is filled in: " + lookups);
  }
}
