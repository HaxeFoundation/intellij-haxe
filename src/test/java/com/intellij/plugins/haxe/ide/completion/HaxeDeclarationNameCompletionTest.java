package com.intellij.plugins.haxe.ide.completion;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Completion: declaration name")
public class HaxeDeclarationNameCompletionTest extends HaxeLightFixtureTestCase {

  private static final String LOCAL_SOURCE = """
    class Main {
    	static function main() {
    		var sprite = 1;
    		var <caret>
    		total = sprite * 2;
    		trace(total);
    	}
    }
    """;
  private static final String PARAMETER_SOURCE = """
    class Main {
    	static function run(<caret>) {
    		trace(limit);
    	}
    }
    """;
  private static final String CONSTRUCTOR_SOURCE = """
    class Box {
    	var width:Float;
    	var height:Float;
    	public function new(<caret>) {}
    }
    """;
  private static final String FIELD_SOURCE = """
    class Box {
    	var <caret>
    	function area():Float return this.width * other.depth;
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a local is offered the undeclared names of its function")
  public void testALocalIsOfferedTheUndeclaredNamesOfItsFunction() {
    List<String> lookups = complete(LOCAL_SOURCE);

    assertTrue(lookups.contains("total"), lookups.toString());
    assertFalse(lookups.contains("sprite"), "a declared name is no candidate: " + lookups);
  }

  @Test
  @DisplayName("a parameter is offered the undeclared names of its function")
  public void testAParameterIsOfferedTheUndeclaredNamesOfItsFunction() {
    List<String> lookups = complete(PARAMETER_SOURCE);

    assertTrue(lookups.contains("limit"), lookups.toString());
  }

  @Test
  @DisplayName("a constructor parameter is offered the fields")
  public void testAConstructorParameterIsOfferedTheFields() {
    List<String> lookups = complete(CONSTRUCTOR_SOURCE);

    assertTrue(lookups.containsAll(List.of("width", "height")), lookups.toString());
  }

  @Test
  @DisplayName("a field is offered the undeclared members the class uses")
  public void testAFieldIsOfferedTheUndeclaredMembersTheClassUses() {
    List<String> lookups = complete(FIELD_SOURCE);

    assertTrue(lookups.contains("width"), lookups.toString());
    assertFalse(lookups.contains("depth"), "a name reached through another object is not this class's: " + lookups);
  }

  private List<String> complete(String source) {
    myFixture.configureByText("Main.hx", source);
    myFixture.completeBasic();
    List<String> lookups = myFixture.getLookupElementStrings();
    assertNotNull(lookups, "several items, so nothing is inserted outright");
    return lookups;
  }
}
