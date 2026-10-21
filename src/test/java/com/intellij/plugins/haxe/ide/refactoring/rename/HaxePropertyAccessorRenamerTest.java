package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.rename.RenameProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Rename: property accessor renamer")
public class HaxePropertyAccessorRenamerTest extends HaxeLightFixtureTestCase {

  private static final String SHAPE_HX_NAME = "Shape.hx";
  private static final String SHAPE_HX_SOURCE = """
    class Shape {
    	public var <caret>width(get, set):Int;
    	function get_width():Int return 0;
    	function set_width(value:Int):Int return value;
    }
    """;
  private static final String TALL_SHAPE_HX_NAME = "TallShape.hx";
  private static final String TALL_SHAPE_HX_SOURCE = """
    class TallShape extends Shape {
    	override function get_width():Int return 1;
    }
    """;
  private static final String MAIN_HX_NAME = "Main.hx";
  private static final String MAIN_HX_SOURCE = """
    class Main {
    	static function main() {
    		var shape = new Shape();
    		shape.width = shape.width + 1;
    	}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("the rename dialog path renames the family and its usages")
  public void testTheRenameDialogPathRenamesTheFamilyAndItsUsages() {
    myFixture.addFileToProject(TALL_SHAPE_HX_NAME, TALL_SHAPE_HX_SOURCE);
    myFixture.addFileToProject(MAIN_HX_NAME, MAIN_HX_SOURCE);
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_HX_SOURCE);
    RenameProcessor processor = new RenameProcessor(getProject(), myFixture.getElementAtCaret(), "size", false, false);
    processor.addRenamerFactory(new HaxePropertyAccessorRenamerFactory());

    processor.run();

    String shape = myFixture.getFile().getText();
    assertTrue(shape.contains("var size(get, set)"), shape);
    assertTrue(shape.contains("get_size()"), shape);
    assertTrue(shape.contains("set_size("), shape);
    assertTrue(textOf(TALL_SHAPE_HX_NAME).contains("get_size()"), textOf(TALL_SHAPE_HX_NAME));
    assertTrue(textOf(MAIN_HX_NAME).contains("shape.size = shape.size + 1"), textOf(MAIN_HX_NAME));
  }

  private String textOf(String fileName) {
    PsiFile file = PsiManager.getInstance(getProject()).findFile(myFixture.findFileInTempDir(fileName));
    assertNotNull(file);
    return file.getText();
  }
}
