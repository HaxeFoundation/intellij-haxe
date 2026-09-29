package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.SuggestedNameInfo;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Rename: name suggestions")
public class HaxeNameSuggestionProviderTest extends HaxeLightFixtureTestCase {

  private static final String TYPED_LOCAL_SOURCE = """
    class Main {
    	static function main() {
    		var <caret>x:String = "a";
    	}
    }
    """;
  private static final String INITIALIZED_LOCAL_SOURCE = """
    class Box {
    	public function new() {}
    }
    class Main {
    	static function main() {
    		var <caret>x = new Box();
    	}
    }
    """;
  private static final String PARAMETER_SOURCE = """
    class Main {
    	static function run(<caret>count:Int) {}
    }
    """;
  private static final String METHOD_SOURCE = """
    class Main {
    	static function <caret>run() {}
    }
    """;
  private static final String CLASHING_LOCAL_SOURCE = """
    class Main {
    	static function main() {
    		var str = "b";
    		var <caret>x:String = "a";
    	}
    }
    """;
  private static final String LOWERCASE_CLASS_SOURCE = """
    class <caret>myThing {}
    """;
  private static final String CAPITALIZED_METHOD_SOURCE = """
    class Main {
    	static function <caret>GetValue() {}
    }
    """;
  private static final String CONSTANT_SOURCE = """
    class Main {
    	static final <caret>maxCount = 5;
    }
    """;
  private static final String NULLABLE_PARAMETER_SOURCE = """
    class Main {
    	static function run(<caret>x:Null<Int>) {}
    }
    """;
  private static final String ARRAY_FIELD_SOURCE = """
    class Box {}
    class Main {
    	var <caret>x:Array<Box>;
    }
    """;
  private static final String OVERRIDE_PARAMETER_SOURCE = """
    class Base {
    	public function new() {}
    	public function update(elapsed:Float, paused:Bool) {}
    }
    class Main extends Base {
    	override public function update(dt:Float, <caret>p:Bool) {}
    }
    """;
  private static final String INTERFACE_PARAMETER_SOURCE = """
    interface Drawable {
    	function draw(canvas:String):Void;
    }
    class Main implements Drawable {
    	public function draw(<caret>c:String) {}
    }
    """;
  private static final String SETTER_PARAMETER_SOURCE = """
    class Main {
    	public var width(default, set):Float;
    	function set_width(<caret>v:Float):Float return width = v;
    }
    """;
  private static final String CAPITALIZED_TYPED_LOCAL_SOURCE = """
    class Main {
    	static function main() {
    		var <caret>Label:String = "a";
    	}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a typed local suggests names from its type")
  public void testATypedLocalSuggestsNamesFromItsType() {
    Set<String> names = suggestionsAtCaret(TYPED_LOCAL_SOURCE);

    assertTrue(names.contains("str"), names.toString());
    assertTrue(names.contains("string"), names.toString());
  }

  @Test
  @DisplayName("an initialized local suggests names from its initializer")
  public void testAnInitializedLocalSuggestsNamesFromItsInitializer() {
    Set<String> names = suggestionsAtCaret(INITIALIZED_LOCAL_SOURCE);

    assertTrue(names.contains("box"), names.toString());
  }

  @Test
  @DisplayName("a parameter suggests names from its type")
  public void testAParameterSuggestsNamesFromItsType() {
    Set<String> names = suggestionsAtCaret(PARAMETER_SOURCE);

    assertTrue(names.contains("i"), names.toString());
  }

  @Test
  @DisplayName("a method named to convention gets no suggestions")
  public void testAMethodNamedToConventionGetsNoSuggestions() {
    myFixture.configureByText("Main.hx", METHOD_SOURCE);
    Set<String> names = new LinkedHashSet<>();

    SuggestedNameInfo info = new HaxeNameSuggestionProvider().getSuggestedNames(componentAtCaret(), null, names);

    assertNull(info);
    assertTrue(names.isEmpty(), names.toString());
  }

  @Test
  @DisplayName("a name in use nearby is avoided")
  public void testANameInUseNearbyIsAvoided() {
    Set<String> names = suggestionsAtCaret(CLASHING_LOCAL_SOURCE);

    assertFalse(names.contains("str"), "str is taken by the sibling local: " + names);
    assertTrue(names.contains("str1"), names.toString());
  }

  @Test
  @DisplayName("rename started on the name element itself gets the same suggestions")
  public void testRenameStartedOnTheNameElementItselfGetsTheSameSuggestions() {
    myFixture.configureByText("Main.hx", TYPED_LOCAL_SOURCE);
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    HaxeComponentName name = PsiTreeUtil.getParentOfType(leaf, HaxeComponentName.class);
    assertNotNull(name);
    Set<String> names = new LinkedHashSet<>();

    SuggestedNameInfo info = new HaxeNameSuggestionProvider().getSuggestedNames(name, null, names);

    assertNotNull(info, "the provider answers for the name element the in-place renamer hands over");
    assertTrue(names.contains("str"), names.toString());
  }

  @Test
  @DisplayName("an overriding method's parameter is offered the overridden parameter's name first")
  public void testAnOverridingMethodsParameterIsOfferedTheOverriddenParametersNameFirst() {
    Set<String> names = suggestionsAtCaret(OVERRIDE_PARAMETER_SOURCE);

    assertEquals("paused", names.iterator().next(), names.toString());
  }

  @Test
  @DisplayName("an implementing method's parameter is offered the interface parameter's name first")
  public void testAnImplementingMethodsParameterIsOfferedTheInterfaceParametersNameFirst() {
    Set<String> names = suggestionsAtCaret(INTERFACE_PARAMETER_SOURCE);

    assertEquals("canvas", names.iterator().next(), names.toString());
  }

  @Test
  @DisplayName("a setter's parameter is offered value")
  public void testASettersParameterIsOfferedValue() {
    Set<String> names = suggestionsAtCaret(SETTER_PARAMETER_SOURCE);

    assertEquals("value", names.iterator().next(), names.toString());
  }

  @Test
  @DisplayName("the recased name leads the type based names")
  public void testTheRecasedNameLeadsTheTypeBasedNames() {
    Set<String> names = suggestionsAtCaret(CAPITALIZED_TYPED_LOCAL_SOURCE);

    assertEquals("label", names.iterator().next(), names.toString());
    assertTrue(names.contains("str"), names.toString());
  }

  @Test
  @DisplayName("a class gets its name capitalized")
  public void testAClassGetsItsNameCapitalized() {
    Set<String> names = suggestionsAtCaret(LOWERCASE_CLASS_SOURCE);

    assertEquals(List.of("MyThing"), List.copyOf(names));
  }

  @Test
  @DisplayName("a method gets its name decapitalized")
  public void testAMethodGetsItsNameDecapitalized() {
    Set<String> names = suggestionsAtCaret(CAPITALIZED_METHOD_SOURCE);

    assertEquals(List.of("getValue"), List.copyOf(names));
  }

  @Test
  @DisplayName("a constant gets upper snake case")
  public void testAConstantGetsUpperSnakeCase() {
    Set<String> names = suggestionsAtCaret(CONSTANT_SOURCE);

    assertTrue(names.contains("MAX_COUNT"), names.toString());
  }

  @Test
  @DisplayName("a nullable parameter is named by the wrapped type")
  public void testANullableParameterIsNamedByTheWrappedType() {
    Set<String> names = suggestionsAtCaret(NULLABLE_PARAMETER_SOURCE);

    assertTrue(names.contains("i"), names.toString());
  }

  @Test
  @DisplayName("an array field is named by its element type in the plural")
  public void testAnArrayFieldIsNamedByItsElementTypeInThePlural() {
    Set<String> names = suggestionsAtCaret(ARRAY_FIELD_SOURCE);

    assertTrue(names.contains("boxes"), names.toString());
  }

  private Set<String> suggestionsAtCaret(String source) {
    myFixture.configureByText("Main.hx", source);
    Set<String> names = new LinkedHashSet<>();
    SuggestedNameInfo info = new HaxeNameSuggestionProvider().getSuggestedNames(componentAtCaret(), null, names);
    assertNotNull(info, "the provider answers for a value declaration");
    return names;
  }

  private PsiElement componentAtCaret() {
    PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    HaxeNamedComponent component = PsiTreeUtil.getParentOfType(leaf, HaxeNamedComponent.class);
    assertNotNull(component);
    return component;
  }
}
