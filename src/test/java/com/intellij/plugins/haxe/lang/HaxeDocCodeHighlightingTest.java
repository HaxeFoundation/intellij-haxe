package com.intellij.plugins.haxe.lang;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.ide.documentation.settings.HaxeDocSettings;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedSymbolInspection;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedLocalVarInspection;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * KDoc-style doc comment code: markdown fences become language-injected
 * fragments (real highlighting, foreign languages included), while their
 * semantic and parse errors stay invisible - sample snippets resolve
 * nothing. Inline backtick spans get the doc-code attribute.
 */
@DisplayName("Highlighting: doc comment code")
public class HaxeDocCodeHighlightingTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/parsing/";
  }

  @Test
  @DisplayName("haxe fence injects a haxe fragment")
  public void testHaxeFenceInjectsAHaxeFragment() {
    String source = """
      class Foo {
      \t/**
      \t\tExample:
      \t\t```haxe
      \t\tvar first = 1;

      \t\tvar second = 2;
      \t\t```
      \t**/
      \tfunction f():Void {}
      }""";
    PsiFile file = configure(source);

    PsiElement injected = injectedAt(file, source.indexOf("var first"));
    assertNotNull(injected, "the fence content must be language-injected");
    assertEquals(HaxeLanguage.INSTANCE, injected.getContainingFile().getLanguage());
    assertEquals("var first = 1;\n\nvar second = 2;", injected.getContainingFile().getText(),
                 "the fragment joins the fence lines, blank lines included");
  }

  @Test
  @DisplayName("foreign fence injects its own language")
  public void testForeignFenceInjectsItsOwnLanguage() {
    assumeTrue(Language.findLanguageByID("XML") != null, "XML language not registered in this environment");
    String source = """
      class Foo {
      \t/**
      \t\t```xml
      \t\t<project name="demo"/>
      \t\t```
      \t**/
      \tfunction f():Void {}
      }""";
    PsiFile file = configure(source);

    PsiElement injected = injectedAt(file, source.indexOf("<project"));
    assertNotNull(injected, "a known fence tag must inject that language");
    assertEquals("XML", injected.getContainingFile().getLanguage().getID());
  }

  @Test
  @DisplayName("multi word info string injects by its first word")
  public void testMultiWordInfoStringInjectsByItsFirstWord() {
    String source = """
      class Foo {
      \t/**
      \t\t```haxe linenos
      \t\tvar first = 1;
      \t\t```
      \t**/
      \tfunction f():Void {}
      }""";
    PsiFile file = configure(source);

    PsiElement injected = injectedAt(file, source.indexOf("var first"));
    assertNotNull(injected, "the tag is the info string's first word");
    assertEquals(HaxeLanguage.INSTANCE, injected.getContainingFile().getLanguage());
  }

  @Test
  @DisplayName("unknown fence tag injects nothing")
  public void testUnknownFenceTagInjectsNothing() {
    String source = """
      class Foo {
      \t/**
      \t\t```mysterylang
      \t\tvar x = 1;
      \t\t```
      \t**/
      \tfunction f():Void {}
      }""";
    PsiFile file = configure(source);

    assertNull(injectedAt(file, source.indexOf("var x")));
  }

  @Test
  @DisplayName("fragment errors stay invisible")
  public void testFragmentErrorsStayInvisible() {
    String source = """
      class Foo {
      \t/**
      \t\t```haxe
      \t\tvar broken = new UnknownType(;
      \t\t```
      \t**/
      \tfunction f():Void {}
      }""";
    PsiFile file = configure(source);

    PsiElement injected = injectedAt(file, source.indexOf("var broken"));
    assertNotNull(injected);
    assertTrue(AnnotatorUtil.isInDocCodeFragment(injected), "semantic annotators must skip the fragment");

    List<HighlightInfo> errors = myFixture.doHighlighting(HighlightSeverity.ERROR);
    assertTrue(errors.isEmpty(), "broken sample code must not produce error markers: " + errors);
  }

  @Test
  @DisplayName("unresolved and unused inspections stay silent in fragments")
  public void testUnresolvedAndUnusedInspectionsStaySilentInFragments() {
    myFixture.enableInspections(new HaxeUnresolvedSymbolInspection(), new HaxeUnusedLocalVarInspection());
    String source = """
      class Foo {
      \t/**
      \t\t```haxe
      \t\tvar sample = new FileFilter("Images", "*.jpg");
      \t\t```
      \t**/
      \tfunction f():Void {}
      }""";
    configure(source);

    // only the fence is asserted on: the host code's own markers (no Haxe std
    // in the light test project, so even Void is unresolved) are not the point
    int fenceStart = source.indexOf("var sample");
    int fenceEnd = source.indexOf("```", fenceStart);
    List<HighlightInfo> warnings = myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING).stream()
      .filter(info -> info.getEndOffset() > fenceStart && info.getStartOffset() < fenceEnd)
      .toList();
    assertTrue(warnings.isEmpty(), "sample code resolves nothing by design - no markers expected: " + warnings);
  }

  @Test
  @DisplayName("completion stays off inside fences")
  public void testCompletionStaysOffInsideFences() {
    String source = """
      class Foo {
      \t/**
      \t\t```haxe
      \t\tva<caret>
      \t\t```
      \t**/
      \tfunction f():Void {}
      }""";
    myFixture.configureByText("Doc.hx", source);

    // must not throw either: item insertion through the fragment's
    // DocumentWindow trips assertions, so nothing may be offered at all
    var lookupElements = myFixture.completeBasic();

    boolean anythingOffered = lookupElements != null && lookupElements.length > 0;
    assertFalse(anythingOffered, "fences are highlight-only - no completion items expected");
  }

  @Test
  @DisplayName("inline span gets the doc code attribute")
  public void testInlineSpanGetsTheDocCodeAttribute() {
    String source = """
      class Foo {
      \t/**
      \t\tCancels a `setInterval()` call.
      \t**/
      \tfunction f():Void {}
      }""";
    configure(source);
    List<HighlightInfo> infos = myFixture.doHighlighting();

    int spanOffset = source.indexOf("setInterval");
    boolean spanColored = infos.stream().anyMatch(info -> covers(info, spanOffset, HaxeSyntaxHighlighterColors.DOC_CODE));
    assertTrue(spanColored, "the backtick span must get the doc-code attribute");
  }

  @Test
  @DisplayName("doc tags get the doc tag attribute")
  public void testDocTagsGetTheDocTagAttribute() {
    String source = """
      class Foo {
      \t/**
      \t\t@param\tx\tthe value
      \t**/
      \tfunction f(x:Int):Void {}
      }""";
    configure(source);
    List<HighlightInfo> infos = myFixture.doHighlighting();

    int tagOffset = source.indexOf("@param");
    boolean tagColored = infos.stream().anyMatch(info -> covers(info, tagOffset, HaxeSyntaxHighlighterColors.DOC_TAG));
    assertTrue(tagColored, "the haxedoc tag must get the doc-tag attribute");
  }

  @Test
  @DisplayName("doc tag completion offers the known tags")
  public void testDocTagCompletionOffersTheKnownTags() {
    myFixture.configureByText("Doc.hx", """
      class Foo {
      \t/**
      \t\t@pa<caret>
      \t**/
      \tfunction f(x:Int):Void {}
      }""");

    var lookupElements = myFixture.completeBasic();

    // a single match (@pa -> @param) is auto-inserted and the lookup is null
    boolean paramInserted = lookupElements == null
                            && myFixture.getEditor().getDocument().getText().contains("@param");
    boolean paramOffered = lookupElements != null
                           && Arrays.stream(lookupElements).anyMatch(e -> e.getLookupString().equals("@param"));
    assertTrue(paramInserted || paramOffered, "typing @ in doc prose must offer the haxedoc tags");
  }

  @Test
  @DisplayName("markup toggle off leaves docs unannotated")
  public void testMarkupToggleOffLeavesDocsUnannotated() {
    HaxeDocSettings.State state = HaxeDocSettings.getInstance().getState();
    state.highlightDocMarkup = false;
    try {
      String source = """
        class Foo {
        \t/**
        \t\tCancels a `setInterval()` call.
        \t\t@param\tx\tthe value
        \t**/
        \tfunction f(x:Int):Void {}
        }""";
      configure(source);
      List<HighlightInfo> infos = myFixture.doHighlighting();

      boolean anyMarkup = infos.stream().anyMatch(info -> info.forcedTextAttributesKey == HaxeSyntaxHighlighterColors.DOC_CODE
                                                          || info.forcedTextAttributesKey == HaxeSyntaxHighlighterColors.DOC_TAG);
      assertFalse(anyMarkup, "the toggle must turn all doc markup coloring off");
    }
    finally {
      state.highlightDocMarkup = true;
    }
  }

  @Test
  @DisplayName("injection toggle off leaves fences plain")
  public void testInjectionToggleOffLeavesFencesPlain() {
    HaxeDocSettings.State state = HaxeDocSettings.getInstance().getState();
    state.injectCodeFences = false;
    try {
      String source = """
        class Foo {
        \t/**
        \t\t```haxe
        \t\tvar x = 1;
        \t\t```
        \t**/
        \tfunction f():Void {}
        }""";
      PsiFile file = configure(source);

      assertNull(injectedAt(file, source.indexOf("var x")), "the toggle must turn fence injection off");
    }
    finally {
      state.injectCodeFences = true;
    }
  }

  private PsiFile configure(String source) {
    return myFixture.configureByText("Doc.hx", source);
  }

  private PsiElement injectedAt(PsiFile file, int offset) {
    return InjectedLanguageManager.getInstance(getProject()).findInjectedElementAt(file, offset);
  }

  private static boolean covers(HighlightInfo info, int offset, TextAttributesKey key) {
    return info.forcedTextAttributesKey == key && info.getStartOffset() <= offset && offset < info.getEndOffset();
  }
}
