package com.intellij.plugins.haxe.display.haxe3;

import com.intellij.plugins.haxe.display.protocol.JsonTypeRef;
import com.intellij.plugins.haxe.display.protocol.JsonTypeRef.Argument;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Display protocol: haxe3 types")
public class Haxe3TypesTest {

  @Test
  @DisplayName("named type keeps its package and parameters")
  public void namedTypeKeepsItsPackageAndParameters() {
    JsonTypeRef type = parsed("haxe.ds.StringMap<Array<Int>>");

    assertTrue(type.isNamedType());
    assertEquals("haxe.ds.StringMap", type.dotPath());
    assertEquals("Array", type.typeParameters().getFirst().dotPath());
    assertEquals("Int", type.typeParameters().getFirst().typeParameters().getFirst().dotPath());
  }

  @Test
  @DisplayName("function keeps its argument names and optionality")
  public void functionKeepsItsArgumentNamesAndOptionality() {
    JsonTypeRef type = parsed("name : String -> ?times : Null<Int> -> String");

    List<Argument> arguments = type.functionArguments();
    assertEquals(2, arguments.size());
    assertEquals("name", arguments.getFirst().name());
    assertFalse(arguments.getFirst().optional());
    assertEquals("times", arguments.get(1).name());
    assertTrue(arguments.get(1).optional());
    assertEquals("Null", arguments.get(1).type().dotPath());
    assertEquals("String", type.returnType().dotPath());
  }

  @Test
  @DisplayName("function without arguments prints void")
  public void functionWithoutArgumentsPrintsVoid() {
    JsonTypeRef type = parsed("Void -> Float");

    assertTrue(type.isFunction());
    assertTrue(type.functionArguments().isEmpty());
    assertEquals("Float", type.returnType().dotPath());
  }

  @Test
  @DisplayName("function argument in parentheses")
  public void functionArgumentInParentheses() {
    JsonTypeRef type = parsed("(Int -> Void) -> Bool");

    assertEquals(1, type.functionArguments().size());
    assertTrue(type.functionArguments().getFirst().type().isFunction());
    assertEquals("Bool", type.returnType().dotPath());
  }

  @Test
  @DisplayName("unbound dynamic and anonymous types")
  public void unboundDynamicAndAnonymousTypes() {
    JsonTypeRef unbound = parsed("Unknown<0>");
    JsonTypeRef dynamic = parsed("Dynamic");
    JsonTypeRef anonymous = parsed("{ x : Int, ?label : String }");

    assertTrue(unbound.isMonomorph());
    assertTrue(dynamic.isDynamic());
    assertTrue(anonymous.isAnonymous());
  }

  @Test
  @DisplayName("std types name their module")
  public void stdTypesNameTheirModule() {
    JsonTypeRef type = parsed("Int");

    assertEquals("StdTypes.Int", type.qualifiedName());
  }

  @ParameterizedTest(name = "\"{0}\"")
  @ValueSource(strings = {"", "->", "Map<Int", "Int ->", "{ x : }"})
  @DisplayName("text that is no type reads as null")
  public void textThatIsNoTypeReadsAsNull(String printed) {
    assertNull(Haxe3Types.parse(printed));
  }

  private static JsonTypeRef parsed(String printed) {
    JsonTypeRef type = Haxe3Types.parse(printed);
    assertNotNull(type, "parses: " + printed);
    return type;
  }
}
