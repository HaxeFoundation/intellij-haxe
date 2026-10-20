package com.intellij.plugins.haxe.runner.debugger.interp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

/**
 * The gate in front of the eval VM's breakpoint registration: a condition the
 * VM cannot parse hangs the whole session (its socket thread dies without an
 * error response), so only text that is exactly one well-formed Haxe
 * expression may be sent.
 */
@DisplayName("Debugger: interp condition validator")
public class InterpConditionValidatorTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  /** (breakpoint condition, whether it may be sent to the VM). */
  static final List<Arguments> CONDITIONS = List.of(
    arguments("i == 3", true),
    arguments("i >= 4 && total > 0", true),
    arguments("items[0] == \"x\"", true),
    arguments("  i == 3  ", true),
    // the adapter drops a trailing semicolon before sending
    arguments("i == 3;", true),
    // truncated or unbalanced: the VM's parser fails on these
    arguments("i == ", false),
    arguments("(i == 3", false),
    // two statements: the VM parses one expression only
    arguments("i == 3; 1", false));

  @ParameterizedTest(name = "{0}")
  @FieldSource("CONDITIONS")
  @DisplayName("lets through exactly one well formed expression")
  public void testLetsThroughExactlyOneWellFormedExpression(String condition, boolean accepted) {
    String problem = InterpConditionValidator.problem(getProject(), condition);
    assertEquals(accepted, problem == null, "'" + condition + "' -> " + problem);
  }
}
