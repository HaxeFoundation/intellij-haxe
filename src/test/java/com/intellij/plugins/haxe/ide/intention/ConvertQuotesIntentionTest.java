package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Intention: convert quotes")
public class ConvertQuotesIntentionTest extends HaxeLightFixtureTestCase {
  private static final String TO_DOUBLE_QUOTES = HaxeBundle.message("haxe.quickfix.surround.with.double.quotation.marks");
  private static final String TO_SINGLE_QUOTES = HaxeBundle.message("haxe.quickfix.surround.with.single.quotation.marks");
  private static final String TO_SINGLE_INTERPOLATING = HaxeBundle.message("haxe.quickfix.convert.to.single.quotes.interpolating");
  private static final String TO_SINGLE_KEEP_TEXT = HaxeBundle.message("haxe.quickfix.convert.to.single.quotes.keep.text");
  private static final String TO_DOUBLE_PLAIN_TEXT = HaxeBundle.message("haxe.quickfix.convert.to.double.quotes.plain.text");

  @Override
  protected String getBasePath() {
    return "/intention/";
  }

  @Test
  @DisplayName("single quotes without interpolation convert to double quotes")
  public void testSingleQuotesWithoutInterpolationConvertToDoubleQuotes() {
    myFixture.configureByText("Main.hx", "class Main { var s = 'plain <caret>text'; }");

    myFixture.launchAction(myFixture.findSingleIntention(TO_DOUBLE_QUOTES));

    myFixture.checkResult("class Main { var s = \"plain text\"; }");
  }

  @Test
  @DisplayName("interpolating single quotes convert to double quotes under a label that says the text goes plain")
  public void testInterpolatingSingleQuotesConvertToDoubleQuotesUnderALabelThatSaysTheTextGoesPlain() {
    myFixture.configureByText("Main.hx", "class Main { var name = 'x'; var s = 'Hi $na<caret>me and ${name}'; }");

    List<IntentionAction> plainOffered = myFixture.filterAvailableIntentions(TO_DOUBLE_QUOTES);
    assertTrue(plainOffered.isEmpty(), "the plain label would hide that the interpolation stops");
    myFixture.launchAction(myFixture.findSingleIntention(TO_DOUBLE_PLAIN_TEXT));

    myFixture.checkResult("class Main { var name = 'x'; var s = \"Hi $name and ${name}\"; }");
  }

  @Test
  @DisplayName("double quotes with a dollar offer interpolating and text keeping conversions")
  public void testDoubleQuotesWithADollarOfferInterpolatingAndTextKeepingConversions() {
    myFixture.configureByText("Main.hx", "class Main { var s = \"Hi $na<caret>me\"; }");

    myFixture.launchAction(myFixture.findSingleIntention(TO_SINGLE_INTERPOLATING));
    myFixture.checkResult("class Main { var s = 'Hi $name'; }");

    myFixture.configureByText("Main.hx", "class Main { var s = \"Hi $na<caret>me\"; }");
    myFixture.launchAction(myFixture.findSingleIntention(TO_SINGLE_KEEP_TEXT));
    myFixture.checkResult("class Main { var s = 'Hi $$name'; }");
  }

  @Test
  @DisplayName("double quotes without a dollar offer the plain conversion only")
  public void testDoubleQuotesWithoutADollarOfferThePlainConversionOnly() {
    myFixture.configureByText("Main.hx", "class Main { var s = \"plain <caret>text\"; }");

    List<IntentionAction> keepText = myFixture.filterAvailableIntentions(TO_SINGLE_KEEP_TEXT);
    assertTrue(keepText.isEmpty(), "without a $ both conversions give the same text");
    myFixture.launchAction(myFixture.findSingleIntention(TO_SINGLE_QUOTES));

    myFixture.checkResult("class Main { var s = 'plain text'; }");
  }
}
