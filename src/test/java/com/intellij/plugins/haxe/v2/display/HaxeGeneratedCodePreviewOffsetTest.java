package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Navigation: generated preview offset")
public class HaxeGeneratedCodePreviewOffsetTest extends HaxeLightFixtureTestCase {

  /** A rule set as the dump prints it: the macro's function binds {@code lexer} around the action. */
  private static final String PREVIEW_SOURCE = """
    package haxeparser;
    class HaxeLexer {
    	public static var string2:hxparse.Ruleset<Int> = hxparse.Lexer.buildRuleset([{rule : "a", func : function(lexer:hxparse.Lexer) {
    		return lexer.curPos().pmax;
    	}}], "string2");
    	static function other(lexer:Int) {}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("the caret lands on the local declared inside the member")
  public void testTheCaretLandsOnTheLocalDeclaredInsideTheMember() {
    LightVirtualFile file = new LightVirtualFile("HaxeLexer.generated.hx", HaxeLanguage.INSTANCE, PREVIEW_SOURCE);
    HaxeGeneratedPreviewSpot spot = new HaxeGeneratedPreviewSpot("haxeparser.HaxeLexer", "haxeparser.HaxeLexer", "string2", "lexer");

    int offset = HaxeGeneratedCodePreview.spotOffset(getProject(), file, spot);

    assertEquals(PREVIEW_SOURCE.indexOf("lexer:hxparse.Lexer"), offset);
  }

  @Test
  @DisplayName("without a local the caret lands on the member name")
  public void testWithoutALocalTheCaretLandsOnTheMemberName() {
    LightVirtualFile file = new LightVirtualFile("HaxeLexer.generated.hx", HaxeLanguage.INSTANCE, PREVIEW_SOURCE);
    HaxeGeneratedPreviewSpot spot = HaxeGeneratedPreviewSpot.ofType("haxeparser.HaxeLexer", "string2");

    int offset = HaxeGeneratedCodePreview.spotOffset(getProject(), file, spot);

    assertEquals(PREVIEW_SOURCE.indexOf("string2:hxparse"), offset);
  }

  @Test
  @DisplayName("a local missing from the member leaves the caret on the member name")
  public void testALocalMissingFromTheMemberLeavesTheCaretOnTheMemberName() {
    LightVirtualFile file = new LightVirtualFile("HaxeLexer.generated.hx", HaxeLanguage.INSTANCE, PREVIEW_SOURCE);
    HaxeGeneratedPreviewSpot spot = new HaxeGeneratedPreviewSpot("haxeparser.HaxeLexer", "haxeparser.HaxeLexer", "string2", "buf");

    int offset = HaxeGeneratedCodePreview.spotOffset(getProject(), file, spot);

    assertEquals(PREVIEW_SOURCE.indexOf("string2:hxparse"), offset);
  }
}
