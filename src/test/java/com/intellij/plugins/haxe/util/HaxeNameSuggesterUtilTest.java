package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLightProjectDescriptors;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeVarInit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.statistics.StatisticsManager;
import com.intellij.psi.statistics.impl.StatisticsManagerImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Refactoring: name suggester")
public class HaxeNameSuggesterUtilTest extends HaxeLightFixtureTestCase {

  /** Declarations every case can refer to; {@code %s} is the statement holding the caret. */
  private static final String MAIN_HX_SOURCE = """
    class Sprite {
    	public var name:String;
    	public function new() {}
    	public function getName():String return name;
    	public function toString():String return name;
    }
    class ParseException {
    	public function new() {}
    }
    class Main {
    	static function setColor(color:Int) {}
    	static function parse(text:String):Sprite return null;
    	static function main() {
    		var sprite = new Sprite();
    		var items = [1, 2];
    		var raw:Dynamic = null;
    		%s
    	}
    }
    """;

  /** (initializer of the caret's declaration, names expected among the suggestions, in this order). */
  static final List<Arguments> INITIALIZER_NAMES = List.of(
    // a getter call names the property, qualified by a variable receiver, then the type
    arguments("var v = <caret>sprite.getName();", List.of("name", "spriteName", "str")),
    arguments("var v = <caret>sprite.toString();", List.of("string", "spriteString", "str")),
    // any other call names its callee; parse is the method in scope, so the name gets a suffix
    arguments("var v = <caret>parse(\"x\");", List.of("parse1", "sprite1")),
    // an indexed array names one element
    arguments("var v = <caret>items[0];", List.of("item", "i")),
    // a construction names the class; sprite is the local in scope
    arguments("var v = <caret>new Sprite();", List.of("sprite1")),
    // a short string literal of identifier words names them; a keyword or a non-ASCII letter disqualifies it
    arguments("var v = <caret>\"Der Kommisar\";", List.of("derKommisar", "str")),
    arguments("var v = <caret>\"Größe\";", List.of("str")),
    arguments("var v = <caret>\"class\";", List.of("str")),
    // parentheses and casts are looked through to the operand; a cast adds its target type
    arguments("var v = <caret>(sprite.getName());", List.of("name")),
    arguments("var v = <caret>cast(raw, Sprite);", List.of("raw1", "sprite1")),
    // an array literal pluralizes its element type
    arguments("var v = <caret>[new Sprite()];", List.of("sprites", "arr")),
    // a map names its key and value types
    arguments("var v = <caret>new Map<String, Int>();", List.of("map", "stringIntMap")),
    // an exception type also offers e
    arguments("var v = <caret>new ParseException();", List.of("parseException", "e")),
    // a literal with nothing to say falls back to its type
    arguments("var v = <caret>true;", List.of("b")),
    arguments("var v = <caret>1.5;", List.of("f")));

  /** (expression under the caret, names its place gives it). */
  static final List<Arguments> PLACE_NAMES = List.of(
    // an argument takes the name of the parameter it feeds
    arguments("setColor(<caret>0xff0000);", List.of("color")),
    // the right side of an assignment takes the target's name
    arguments("sprite.name = <caret>\"x\";", List.of("name")));

  /** (kind, expected first name for the getter initializer). */
  static final List<Arguments> KIND_CASING = List.of(
    arguments(HaxeNameKind.VARIABLE, "name"),
    arguments(HaxeNameKind.METHOD, "name"),
    arguments(HaxeNameKind.TYPE, "Name"),
    arguments(HaxeNameKind.ENUM_VALUE, "Name"),
    arguments(HaxeNameKind.CONSTANT, "NAME"));

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The standard library, for Map. */
  @Override
  protected LightProjectDescriptor lightProjectDescriptor() {
    return HaxeLightProjectDescriptors.WITH_TOOLKIT;
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("INITIALIZER_NAMES")
  public void testNamesFromAnInitializer(String statement, List<String> expectedInOrder) {
    HaxeExpression initializer = initializerAtCaret(statement);

    List<String> names = HaxeNameSuggesterUtil.suggest(initializer, null, HaxeNameKind.VARIABLE, initializer, Set.of()).names();

    assertInOrder(expectedInOrder, names);
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("PLACE_NAMES")
  public void testNamesFromTheExpressionsPlace(String statement, List<String> expectedInOrder) {
    HaxeExpression expression = expressionAtCaret(statement);

    List<String> names = HaxeNameSuggesterUtil.suggest(expression, null, HaxeNameKind.VARIABLE, expression, Set.of()).names();

    assertInOrder(expectedInOrder, names);
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("KIND_CASING")
  public void testEachKindCasesItsNames(HaxeNameKind kind, String expectedFirst) {
    HaxeExpression initializer = initializerAtCaret("var v = <caret>sprite.getName();");

    HaxeSuggestedNames suggested = HaxeNameSuggesterUtil.suggest(initializer, null, kind, initializer, Set.of());

    assertEquals(expectedFirst, suggested.first(), suggested.names().toString());
  }

  @Test
  @DisplayName("a constant offers upper snake case before lower camel case")
  public void testAConstantOffersUpperSnakeCaseBeforeLowerCamelCase() {
    HaxeExpression initializer = initializerAtCaret("var v = <caret>sprite.getName();");

    List<String> names = HaxeNameSuggesterUtil.suggest(initializer, null, HaxeNameKind.CONSTANT, initializer, Set.of()).names();

    assertInOrder(List.of("NAME", "SPRITE_NAME", "name", "spriteName"), names);
  }

  @Test
  @DisplayName("a name in use nearby gets a numeric suffix")
  public void testANameInUseNearbyGetsANumericSuffix() {
    HaxeExpression initializer = initializerAtCaret("var v = <caret>new Sprite();");
    HaxeExpression taken = initializerAtCaret("var sprite = <caret>new Sprite();");

    List<String> free = HaxeNameSuggesterUtil.suggest(initializer, null, HaxeNameKind.VARIABLE, null, Set.of("sprite")).names();
    List<String> inScope = HaxeNameSuggesterUtil.suggest(taken, null, HaxeNameKind.VARIABLE, taken, Set.of()).names();

    assertEquals("sprite1", free.getFirst(), free.toString());
    assertEquals("sprite1", inScope.getFirst(), inScope.toString());
  }

  @Test
  @DisplayName("a name chosen before ranks first and an often chosen one is added")
  public void testANameChosenBeforeRanksFirstAndAnOftenChosenOneIsAdded() {
    // the store ignores every count in tests unless switched on
    ((StatisticsManagerImpl)StatisticsManager.getInstance()).enableStatistics(getTestRootDisposable());
    HaxeExpression initializer = initializerAtCaret("var v = <caret>sprite.getName();");
    HaxeSuggestedNames before = HaxeNameSuggesterUtil.suggest(initializer, null, HaxeNameKind.VARIABLE, initializer, Set.of());
    assertEquals("name", before.first(), before.names().toString());

    before.recordChosen("spriteName");
    for (int i = 0; i < 3; i++) before.recordChosen("label");
    HaxeSuggestedNames after = HaxeNameSuggesterUtil.suggest(initializer, null, HaxeNameKind.VARIABLE, initializer, Set.of());

    assertEquals("label", after.first(), after.names().toString());
    assertEquals("spriteName", after.names().get(1), after.names().toString());
  }

  @Test
  @DisplayName("a lambda parameter is named by its type name alone")
  public void testALambdaParameterIsNamedByItsTypeNameAlone() {
    List<String> fromType = HaxeNameSuggesterUtil.suggestForType(null, "Int", false, null, Set.of());
    List<String> declared = HaxeNameSuggesterUtil.suggestForType("index", "Int", false, null, Set.of());
    List<String> function = HaxeNameSuggesterUtil.suggestForType(null, null, true, null, Set.of());

    assertEquals("i", fromType.getFirst(), fromType.toString());
    assertEquals("index", declared.getFirst(), declared.toString());
    assertEquals("func", function.getFirst(), function.toString());
  }

  private HaxeExpression initializerAtCaret(String statement) {
    HaxeVarInit initializer = PsiTreeUtil.getParentOfType(configure(statement), HaxeVarInit.class);
    assertNotNull(initializer, "the caret sits in an initializer");
    return initializer.getExpression();
  }

  /** The widest expression starting at the caret that spans no more than the innermost one. */
  private HaxeExpression expressionAtCaret(String statement) {
    HaxeExpression expression = PsiTreeUtil.getParentOfType(configure(statement), HaxeExpression.class);
    assertNotNull(expression, "the caret sits in an expression");
    while (expression.getParent() instanceof HaxeExpression parent && parent.getTextRange().equals(expression.getTextRange())) {
      expression = parent;
    }
    return expression;
  }

  private PsiElement configure(String statement) {
    myFixture.configureByText("Main.hx", MAIN_HX_SOURCE.formatted(statement));
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(leaf);
    return leaf;
  }

  private static void assertInOrder(List<String> expectedInOrder, List<String> names) {
    int position = -1;
    for (String expected : expectedInOrder) {
      int found = names.indexOf(expected);
      assertTrue(found > position, expected + " expected after position " + position + " in " + names);
      position = found;
    }
  }
}
