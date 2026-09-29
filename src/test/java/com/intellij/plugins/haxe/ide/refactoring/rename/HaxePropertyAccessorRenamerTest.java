package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  @DisplayName("the renamer names every other member after the property")
  public void testTheRenamerNamesEveryOtherMemberAfterTheProperty() {
    myFixture.addFileToProject(TALL_SHAPE_HX_NAME, TALL_SHAPE_HX_SOURCE);
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_HX_SOURCE);
    PsiElement property = myFixture.getElementAtCaret();

    AutomaticRenamer renamer = new HaxePropertyAccessorRenamerFactory().createRenamer(property, "size", List.of());

    assertTrue(renamer.hasAnythingToRename());
    assertEquals(Map.of("get_width", "get_size", "set_width", "set_size"), namesOf(renamer.getRenames()));
    long gettersRenamed = renamer.getRenames().values().stream().filter("get_size"::equals).count();
    assertEquals(2, gettersRenamed, "both getters, the override included");
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

  /** Old name to new name; the two getters share both names, so the map holds them once. */
  private static Map<String, String> namesOf(Map<PsiNamedElement, String> renames) {
    Map<String, String> names = new TreeMap<>();
    renames.forEach((member, newName) -> names.put(member.getName(), newName));
    return names;
  }

  private String textOf(String fileName) {
    PsiFile file = PsiManager.getInstance(getProject()).findFile(myFixture.findFileInTempDir(fileName));
    assertNotNull(file);
    return file.getText();
  }
}
