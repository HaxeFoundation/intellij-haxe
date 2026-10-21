package com.intellij.plugins.haxe.runner.debugger.eval;

import static com.intellij.plugins.haxe.runner.debugger.eval.EvalDebugAdapter.stripTrailingSemicolons;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

@DisplayName("Eval debugger: debug adapter")
public class EvalDebugAdapterTest {

  /** (expression as typed, expression as handed to the VM). */
  static final List<Arguments> TRAILING_SEMICOLON_STRIPS = List.of(
    // strips one trailing semicolon: the eval VM's expression parser rejects a trailing ';' (Unexpected ;)
    arguments("this.member = 1;", "this.member = 1"),
    arguments("member;", "member"),
    // strips semicolons with surrounding whitespace
    arguments("  x + y ; ", "x + y"),
    arguments("a ; ;", "a"),
    // leaves clean expressions untouched
    arguments("this.member = 1", "this.member = 1"),
    arguments("arr[0]", "arr[0]"),
    // does not touch interior semicolons: no valid single expression has one,
    // but if present it is the VM's to reject
    arguments("f(a; b);", "f(a; b)"),
    // null passes through
    arguments(null, null));

  @ParameterizedTest(name = "{0}")
  @FieldSource("TRAILING_SEMICOLON_STRIPS")
  @DisplayName("strips trailing semicolons")
  public void stripsTrailingSemicolons(String expression, String expected) {
    assertEquals(expected, stripTrailingSemicolons(expression));
  }
}
