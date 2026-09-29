package com.intellij.plugins.haxe.ide.folding;

import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.PPELSE;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.PPELSEIF;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.PPIF;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Folding: conditional regions")
public class HaxeConditionalFoldingTest extends HaxeLightFixtureTestCase {

  private static final TokenSet REGION_OPENERS = TokenSet.create(PPIF, PPELSEIF, PPELSE);

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("region nested in a dead branch folds")
  public void testRegionNestedInADeadBranchFolds() {
    // the nested region lies inside the dead branch's blob; folding reaches
    // it through the blob's parsed children
    String source = """
      class Foo {
      \t#if never
      \tfunction dead():Void {
      \t\t#if debug
      \t\ttrace("debug");
      \t\t#else
      \t\ttrace("release");
      \t\t#end
      \t}
      \t#end
      }""";
    PsiFile file = myFixture.configureByText("Foo.hx", source);
    Document document = myFixture.getEditor().getDocument();

    FoldingDescriptor[] regions = new HaxeFoldingBuilder().buildFoldRegions(file.getNode(), document);
    List<Integer> regionStarts = Arrays.stream(regions)
      .filter(region -> REGION_OPENERS.contains(elementType(region)))
      .map(region -> region.getRange().getStartOffset())
      .sorted()
      .toList();

    List<Integer> directiveOffsets = Stream.of("#if never", "#if debug", "#else").map(source::indexOf).sorted().toList();
    assertEquals(directiveOffsets, regionStarts, "the dead branch, the nested branch and its #else branch each fold from their directive");
  }

  private static IElementType elementType(FoldingDescriptor region) {
    return region.getElement().getElementType();
  }
}
