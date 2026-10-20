package com.intellij.plugins.haxe.v2.display;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Compiler services: compiler resolve service")
public class HaxeCompilerResolveServiceTest {

  /** (type dot path, module path holding it). */
  static final List<Arguments> MODULE_PATHS = List.of(
    arguments("Main", "Main"),
    arguments("pack.Main", "pack.Main"),
    arguments("pack.sub.Shapes", "pack.sub.Shapes"),
    // a sub-type: the uppercase segment before the type name is its module
    arguments("pack.Shapes.Circle", "pack.Shapes"),
    arguments("StdTypes.Void", "StdTypes"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("MODULE_PATHS")
  @DisplayName("module path of a type")
  public void modulePathOfAType(String dotPath, String modulePath) {
    assertEquals(modulePath, HaxeCompilerResolveService.modulePathOf(dotPath));
  }

  /** (dot path, whether it names a type parameter, which no module declares). */
  static final List<Arguments> TYPE_PARAMETER_PATHS = List.of(
    arguments("haxe.iterators.ArrayIterator", false),
    arguments("pack.Shapes.Circle", false),
    // a class's type parameter, as the PSI qualifies it
    arguments("haxe.iterators.ArrayIterator:T", true),
    // a method's type parameter
    arguments("pack.Owner#method:T", true));

  @ParameterizedTest(name = "{0}")
  @FieldSource("TYPE_PARAMETER_PATHS")
  @DisplayName("a type parameter's path asks for no blueprint")
  public void aTypeParametersPathAsksForNoBlueprint(String dotPath, boolean typeParameter) {
    assertEquals(typeParameter, HaxeCompilerResolveService.namesTypeParameter(dotPath));
  }
}
