package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringSupportProvider;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.rename.RenameHandler;
import com.intellij.refactoring.rename.RenameHandlerRegistry;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler;
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Rename: in place")
public class HaxeInplaceRenameTest extends HaxeLightFixtureTestCase {

  private static final String HELPER_HX_NAME = "Helper.hx";
  private static final String HELPER_HX_SOURCE = """
    class Helper {
    	public static function <caret>run() {}
    }
    """;
  private static final String MAIN_HX_NAME = "Main.hx";
  private static final String MAIN_HX_SOURCE = """
    class Main {
    	static function main() {
    		Helper.run();
    	}
    }
    """;
  private static final String LOCAL_SOURCE = """
    class Main {
    	static function main() {
    		var <caret>count = 1;
    		trace(count);
    	}
    }
    """;
  private static final String HELPER_HX_DECLARATION = """
    class Helper {
    	public static function run() {}
    }
    """;
  private static final String MAIN_HX_REFERENCE = """
    class Main {
    	static function main() {
    		Helper.<caret>run();
    	}
    }
    """;
  private static final String LOCAL_REFERENCE_SOURCE = """
    class Main {
    	static function main() {
    		var count = 1;
    		trace(<caret>count);
    	}
    }
    """;
  private static final String HELPER_WITH_CONSTRUCTOR_HX_SOURCE = """
    class Helper {
    	public function new() {}
    }
    """;
  private static final String NEW_EXPRESSION_REFERENCE_SOURCE = """
    class Main {
    	static function main() {
    		var helper = new <caret>Helper();
    	}
    }
    """;
  private static final String TYPED_LOCAL_SOURCE = """
    class Main {
    	static function main() {
    		var <caret>x:String = "a";
    	}
    }
    """;
  private static final String PARAMETER_REFERENCE_SOURCE = """
    class Main {
    	static function main(count:Int) {
    		trace(<caret>count);
    	}
    }
    """;
  private static final String SHAPE_HX_NAME = "Shape.hx";
  private static final String SHAPE_PROPERTY_HX_SOURCE = """
    class Shape {
    	public var <caret>width(get, set):Int;
    	function get_width():Int return 0;
    	function set_width(value:Int):Int return value;
    }
    """;
  private static final String SHAPE_PROPERTY_WITH_CALL_HX_SOURCE = """
    class Shape {
    	public var <caret>width(get, set):Int;
    	function get_width():Int return 0;
    	function set_width(value:Int):Int return value;
    	function reset() set_width(0);
    }
    """;
  private static final String SHAPE_GETTER_HX_SOURCE = """
    class Shape {
    	public var width(get, set):Int;
    	function get_<caret>width():Int return 0;
    	function set_width(value:Int):Int return value;
    }
    """;
  private static final String TALL_SHAPE_HX_NAME = "TallShape.hx";
  private static final String TALL_SHAPE_HX_SOURCE = """
    class TallShape extends Shape {
    	override function get_width():Int return 1;
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      HaxePropertyInplaceRenameHandler.renameAccessorsInTests = null;
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("a member renames in place and the other files follow")
  public void testAMemberRenamesInPlaceAndTheOtherFilesFollow() {
    myFixture.addFileToProject(MAIN_HX_NAME, MAIN_HX_SOURCE);
    myFixture.configureByText(HELPER_HX_NAME, HELPER_HX_SOURCE);
    HaxeRefactoringSupportProvider provider = new HaxeRefactoringSupportProvider();
    PsiElement name = nameAtCaret();
    assertTrue(provider.isMemberInplaceRenameAvailable(name, name));
    assertFalse(provider.isInplaceRenameAvailable(name, name), "a member is not a variable to the variable renamer");

    CodeInsightTestUtil.doInlineRename(new MemberInplaceRenameHandler(), "go", myFixture);

    assertTrue(myFixture.getFile().getText().contains("function go()"), myFixture.getFile().getText());
    assertTrue(textOf(MAIN_HX_NAME).contains("Helper.go()"), textOf(MAIN_HX_NAME));
  }

  @Test
  @DisplayName("the in place name lookup offers the suggestions")
  public void testTheInPlaceNameLookupOffersTheSuggestions() {
    myFixture.configureByText(MAIN_HX_NAME, TYPED_LOCAL_SOURCE);
    // keeps the template interactive, as the editor does, instead of finishing it at once
    TemplateManagerImpl.setTemplateTesting(getTestRootDisposable());
    DataContext context = DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent());

    new VariableInplaceRenameHandler().doRename(nameAtCaret(), myFixture.getEditor(), context);

    Lookup lookup = LookupManager.getActiveLookup(myFixture.getEditor());
    assertNotNull(lookup, "the template stop opens the name lookup");
    List<String> offered = lookup.getItems().stream().map(LookupElement::getLookupString).toList();
    assertTrue(offered.containsAll(List.of("str", "string")), offered.toString());
    TemplateManagerImpl.getTemplateState(myFixture.getEditor()).gotoEnd(false);
  }

  @Test
  @DisplayName("a local renames in place as a variable")
  public void testALocalRenamesInPlaceAsAVariable() {
    myFixture.configureByText(MAIN_HX_NAME, LOCAL_SOURCE);
    HaxeRefactoringSupportProvider provider = new HaxeRefactoringSupportProvider();
    PsiElement name = nameAtCaret();
    assertTrue(provider.isInplaceRenameAvailable(name, name));
    assertFalse(provider.isMemberInplaceRenameAvailable(name, name));

    CodeInsightTestUtil.doInlineRename(new VariableInplaceRenameHandler(), "total", myFixture);

    String text = myFixture.getFile().getText();
    assertTrue(text.contains("var total = 1;") && text.contains("trace(total)"), text);
  }

  @Test
  @DisplayName("a member renames in place from a reference too")
  public void testAMemberRenamesInPlaceFromAReferenceToo() {
    myFixture.addFileToProject(HELPER_HX_NAME, HELPER_HX_DECLARATION);
    myFixture.configureByText(MAIN_HX_NAME, MAIN_HX_REFERENCE);
    HaxeRefactoringSupportProvider provider = new HaxeRefactoringSupportProvider();
    PsiElement target = myFixture.getElementAtCaret();
    assertTrue(provider.isMemberInplaceRenameAvailable(target, target), target.getClass().getName());

    renameInPlaceFromReference(new MemberInplaceRenameHandler(), "go", target);

    assertTrue(myFixture.getFile().getText().contains("Helper.go()"), myFixture.getFile().getText());
    assertTrue(textOf(HELPER_HX_NAME).contains("function go()"), textOf(HELPER_HX_NAME));
  }

  @Test
  @DisplayName("a local renames in place from a reference too")
  public void testALocalRenamesInPlaceFromAReferenceToo() {
    myFixture.configureByText(MAIN_HX_NAME, LOCAL_REFERENCE_SOURCE);
    HaxeRefactoringSupportProvider provider = new HaxeRefactoringSupportProvider();
    PsiElement target = myFixture.getElementAtCaret();
    assertTrue(provider.isInplaceRenameAvailable(target, target), target.getClass().getName());

    renameInPlaceFromReference(new VariableInplaceRenameHandler(), "total", target);

    String text = myFixture.getFile().getText();
    assertTrue(text.contains("var total = 1;") && text.contains("trace(total)"), text);
  }

  @Test
  @DisplayName("a class renames in place from a new expression")
  public void testAClassRenamesInPlaceFromANewExpression() {
    myFixture.addFileToProject(HELPER_HX_NAME, HELPER_WITH_CONSTRUCTOR_HX_SOURCE);
    myFixture.configureByText(MAIN_HX_NAME, NEW_EXPRESSION_REFERENCE_SOURCE);
    HaxeRefactoringSupportProvider provider = new HaxeRefactoringSupportProvider();
    // the target is the constructor, which the generic member handler must leave alone: its name is `new`
    PsiElement target = myFixture.getElementAtCaret();
    assertFalse(provider.isMemberInplaceRenameAvailable(target, target), target.getClass().getName());
    DataContext context = DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent());
    List<? extends RenameHandler> handlers = RenameHandlerRegistry.getInstance().getRenameHandlers(context);
    assertEquals(1, handlers.size(), "one handler, or the platform asks which to use: " + handlers);
    assertInstanceOf(HaxeConstructorCallInplaceRenameHandler.class, handlers.getFirst());

    renameInPlaceFromReference(new HaxeConstructorCallInplaceRenameHandler(), "Gone", target);

    assertTrue(myFixture.getFile().getText().contains("new Gone()"), myFixture.getFile().getText());
    assertTrue(textOf("Gone.hx").contains("class Gone {"), textOf("Gone.hx"));
  }

  @Test
  @DisplayName("a parameter renames in place from a reference too")
  public void testAParameterRenamesInPlaceFromAReferenceToo() {
    myFixture.configureByText(MAIN_HX_NAME, PARAMETER_REFERENCE_SOURCE);
    HaxeRefactoringSupportProvider provider = new HaxeRefactoringSupportProvider();
    PsiElement target = myFixture.getElementAtCaret();
    assertTrue(provider.isInplaceRenameAvailable(target, target), target.getClass().getName());

    renameInPlaceFromReference(new VariableInplaceRenameHandler(), "total", target);

    String text = myFixture.getFile().getText();
    assertTrue(text.contains("main(total:Int)") && text.contains("trace(total)"), text);
  }

  @Test
  @DisplayName("a property offers only the property rename handler")
  public void testAPropertyOffersOnlyThePropertyRenameHandler() {
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_PROPERTY_HX_SOURCE);
    HaxeRefactoringSupportProvider provider = new HaxeRefactoringSupportProvider();
    PsiElement name = nameAtCaret();
    DataContext context = DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent());

    List<? extends RenameHandler> handlers = RenameHandlerRegistry.getInstance().getRenameHandlers(context);

    assertFalse(provider.isMemberInplaceRenameAvailable(name, name), "the generic member renamer leaves the family to its handler");
    assertEquals(1, handlers.size(), "one handler, or the platform asks which to use: " + handlers);
    assertInstanceOf(HaxePropertyInplaceRenameHandler.class, handlers.getFirst());
  }

  @Test
  @DisplayName("a property renames in place with its accessors and their overrides")
  public void testAPropertyRenamesInPlaceWithItsAccessorsAndTheirOverrides() {
    HaxePropertyInplaceRenameHandler.renameAccessorsInTests = true;
    myFixture.addFileToProject(TALL_SHAPE_HX_NAME, TALL_SHAPE_HX_SOURCE);
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_PROPERTY_HX_SOURCE);

    CodeInsightTestUtil.doInlineRename(new HaxePropertyInplaceRenameHandler(), "size", myFixture);

    String shape = myFixture.getFile().getText();
    assertTrue(shape.contains("var size(get, set)"), shape);
    assertTrue(shape.contains("get_size()"), shape);
    assertTrue(shape.contains("set_size("), shape);
    assertTrue(textOf(TALL_SHAPE_HX_NAME).contains("get_size()"), textOf(TALL_SHAPE_HX_NAME));
  }

  @Test
  @DisplayName("a property renames in place on its own when asked to")
  public void testAPropertyRenamesInPlaceOnItsOwnWhenAskedTo() {
    HaxePropertyInplaceRenameHandler.renameAccessorsInTests = false;
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_PROPERTY_HX_SOURCE);

    CodeInsightTestUtil.doInlineRename(new HaxePropertyInplaceRenameHandler(), "size", myFixture);

    String shape = myFixture.getFile().getText();
    assertTrue(shape.contains("var size(get, set)"), shape);
    assertTrue(shape.contains("get_width()"), shape);
    assertTrue(shape.contains("set_width("), shape);
  }

  @Test
  @DisplayName("the accessors follow the property name while it is typed")
  public void testTheAccessorsFollowThePropertyNameWhileItIsTyped() {
    HaxePropertyInplaceRenameHandler.renameAccessorsInTests = true;
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_PROPERTY_WITH_CALL_HX_SOURCE);
    TemplateState template = startPropertyRename();

    typeIntoTemplate(template, "size");

    String live = myFixture.getEditor().getDocument().getText();
    assertTrue(live.contains("function get_size()"), live);
    assertTrue(live.contains("function set_size("), live);
    assertTrue(live.contains("reset() set_size(0)"), live);

    template.gotoEnd(false);

    String committed = myFixture.getFile().getText();
    assertTrue(committed.contains("var size(get, set)"), committed);
    assertTrue(committed.contains("function get_size()"), committed);
    assertTrue(committed.contains("reset() set_size(0)"), committed);
    assertFalse(committed.contains("width"), committed);
  }

  @Test
  @DisplayName("the property follows an accessor name while its part after the prefix is typed")
  public void testThePropertyFollowsAnAccessorNameWhileItsPartAfterThePrefixIsTyped() {
    HaxePropertyInplaceRenameHandler.renameAccessorsInTests = true;
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_GETTER_HX_SOURCE);
    TemplateState template = startPropertyRename();
    TextRange edited = template.getCurrentVariableRange();
    assertNotNull(edited);
    assertEquals("width", edited.substring(myFixture.getEditor().getDocument().getText()), "only the part after get_ is edited");

    typeIntoTemplate(template, "size");

    String live = myFixture.getEditor().getDocument().getText();
    assertTrue(live.contains("var size(get, set)"), live);
    assertTrue(live.contains("function get_size()"), live);
    assertTrue(live.contains("function set_size("), live);

    template.gotoEnd(false);

    String committed = myFixture.getFile().getText();
    assertTrue(committed.contains("var size(get, set)"), committed);
    assertTrue(committed.contains("function get_size()"), committed);
    assertFalse(committed.contains("width"), committed);
  }

  @Test
  @DisplayName("an accessor renames in place with its property")
  public void testAnAccessorRenamesInPlaceWithItsProperty() {
    HaxePropertyInplaceRenameHandler.renameAccessorsInTests = true;
    myFixture.addFileToProject(TALL_SHAPE_HX_NAME, TALL_SHAPE_HX_SOURCE);
    myFixture.configureByText(SHAPE_HX_NAME, SHAPE_GETTER_HX_SOURCE);

    // the template edits the part after get_, so the property name is what gets typed
    CodeInsightTestUtil.doInlineRename(new HaxePropertyInplaceRenameHandler(), "size", myFixture);

    String shape = myFixture.getFile().getText();
    assertTrue(shape.contains("var size(get, set)"), shape);
    assertTrue(shape.contains("get_size()"), shape);
    assertTrue(shape.contains("set_size("), shape);
    assertTrue(textOf(TALL_SHAPE_HX_NAME).contains("get_size()"), textOf(TALL_SHAPE_HX_NAME));
  }

  /**
   * Renames in the editor holding the reference; the target is the declaring
   * component the reference resolves to, which is what the platform hands the
   * handlers. The fixture-only overload opens an editor on the target's file
   * instead, which is a rename from the declaration.
   */
  private void renameInPlaceFromReference(VariableInplaceRenameHandler handler, String newName, PsiElement target) {
    CodeInsightTestUtil.doInlineRename(handler, newName, myFixture.getEditor(), target);
  }

  /** Starts the property rename handler on the name at the caret and returns its running template. */
  private TemplateState startPropertyRename() {
    // keeps the template interactive, as the editor does, instead of finishing it at once
    TemplateManagerImpl.setTemplateTesting(getTestRootDisposable());
    DataContext context = DataManager.getInstance().getDataContext(myFixture.getEditor().getComponent());
    new HaxePropertyInplaceRenameHandler().doRename(nameAtCaret(), myFixture.getEditor(), context);
    TemplateState template = TemplateManagerImpl.getTemplateState(myFixture.getEditor());
    assertNotNull(template, "the template is running");
    return template;
  }

  /** Replaces the text of the template's current stop, as typing over the selected name does. */
  private void typeIntoTemplate(TemplateState template, String text) {
    TextRange edited = template.getCurrentVariableRange();
    assertNotNull(edited);
    Document document = myFixture.getEditor().getDocument();
    WriteCommandAction.writeCommandAction(getProject()).run(() -> document.replaceString(edited.getStartOffset(), edited.getEndOffset(), text));
  }

  private PsiElement nameAtCaret() {
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    HaxeComponentName name = PsiTreeUtil.getParentOfType(leaf, HaxeComponentName.class, false);
    assertNotNull(name);
    return name;
  }

  private String textOf(String fileName) {
    PsiFile file = PsiManager.getInstance(getProject()).findFile(myFixture.findFileInTempDir(fileName));
    assertNotNull(file);
    return file.getText();
  }
}
