package com.intellij.plugins.haxe.lang;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.intellij.plugins.haxe.lang.HaxeCodeStyleTweaks.commonSettings;
import static com.intellij.plugins.haxe.lang.HaxeCodeStyleTweaks.haxeSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Wrap toggles against inline sources. The grammar wraps EVERY binary and
 * ternary operator into a composite element (additiveOperator,
 * questionOperator, ...) - matching the bare tokens silently disables the
 * sign-placement halves of these settings, which is what these tests pin.
 */
@DisplayName("Formatting: wrap settings")
public class HaxeWrapSettingsTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/formatter/";
  }

  @Test
  @DisplayName("ternary wrap forms")
  public void testTernaryWrapForms() {
    Consumer<CommonCodeStyleSettings> wrapAlways = common -> common.TERNARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS;
    String source = """
      class Main {
      	static function main() {
      		var total = 101;
      		var label = total > 100 ? 'large' : 'small';
      		trace(label);
      	}
      }
      """;

    assertEquals("""
      class Main {
          static function main() {
              var total = 101;
              var label = total > 100 ?
                      'large' :
                      'small';
              trace(label);
          }
      }
      """, reformat(commonSettings(wrapAlways.andThen(common -> common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = false)), source));

    assertEquals("""
      class Main {
          static function main() {
              var total = 101;
              var label = total > 100
                      ? 'large'
                      : 'small';
              trace(label);
          }
      }
      """, reformat(commonSettings(wrapAlways.andThen(common -> common.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE = true)), source));
  }

  @Test
  @DisplayName("binary wrap sign placement")
  public void testBinaryWrapSignPlacement() {
    Consumer<CommonCodeStyleSettings> wrapAlways = common -> common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS;
    String source = """
      class Main {
      	static function main() {
      		var total = 1 + 2 * 3;
      		trace(total);
      	}
      }
      """;

    assertEquals("""
      class Main {
          static function main() {
              var total = 1 +
              2 *
              3;
              trace(total);
          }
      }
      """, reformat(commonSettings(wrapAlways.andThen(common -> common.BINARY_OPERATION_SIGN_ON_NEXT_LINE = false)), source));

    assertEquals("""
      class Main {
          static function main() {
              var total = 1
              + 2
              * 3;
              trace(total);
          }
      }
      """, reformat(commonSettings(wrapAlways.andThen(common -> common.BINARY_OPERATION_SIGN_ON_NEXT_LINE = true)), source));
  }

  /**
   * Operand alignment anchors at the binary's first operand; for a binary
   * used as a call ARGUMENT that anchor sits mid-line and every argument
   * staircases deeper than the last - alignment applies only where the
   * expression owns its line.
   */
  @Test
  @DisplayName("binary alignment skips call arguments")
  public void testBinaryAlignmentSkipsCallArguments() {
    Consumer<CommonCodeStyleSettings> configure = common -> {
      common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS;
      common.BINARY_OPERATION_SIGN_ON_NEXT_LINE = true;
      common.ALIGN_MULTILINE_BINARY_OPERATION = true;
    };
    String source = """
      class Main {
      	static function main() {
      		var first = 1;
      		var second = 2;
      		var total = first + second;
      		trace(pick(first + 1, second + 2));
      	}

      	static function pick(a:Int, b:Int):Int {
      		return a + b;
      	}
      }
      """;

    assertEquals("""
      class Main {
          static function main() {
              var first = 1;
              var second = 2;
              var total = first
                          + second;
              trace(pick(first
              + 1, second
              + 2));
          }

          static function pick(a:Int, b:Int):Int {
              return a
                     + b;
          }
      }
      """, reformat(commonSettings(configure), source));
  }

  @Test
  @DisplayName("wrapped operator chain indent")
  public void testWrappedOperatorChainIndent() {
    // off (the plugin default), a written &&-chain wrap follows the classic
    // alignment-driven continuation: operands align under the first one
    // when binary alignment is on; on, every wrapped operand steps ONE
    // level in from the chain's line instead
    Consumer<CodeStyleSettings> alignedOperands = commonSettings(common -> common.ALIGN_MULTILINE_BINARY_OPERATION = true)
      .andThen(haxeSettings(haxe -> haxe.INDENT_WRAPPED_OPERATOR_CHAINS = false));
    Consumer<CodeStyleSettings> steppedOperands = haxeSettings(haxe -> haxe.INDENT_WRAPPED_OPERATOR_CHAINS = true);
    String source = """
      class Main {
      	static function main() {
      		var ready = true;
      		var armed = false;
      		var go = ready
      			&& armed
      			&& !paused;
      		trace(go);
      	}
      }
      """;

    assertEquals("""
      class Main {
          static function main() {
              var ready = true;
              var armed = false;
              var go = ready
                       && armed
                       && !paused;
              trace(go);
          }
      }
      """, reformat(alignedOperands, source));

    assertEquals("""
      class Main {
          static function main() {
              var ready = true;
              var armed = false;
              var go = ready
                  && armed
                  && !paused;
              trace(go);
          }
      }
      """, reformat(steppedOperands, source));
  }

  /** ALWAYS must break EVERY implements clause; only fill mode leaves the first one inline. */
  @Test
  @DisplayName("extends list wrap always breaks every clause")
  public void testExtendsListWrapAlwaysBreaksEveryClause() {
    String source = """
      class Foo extends Base implements Drawable implements Resizable {
      	public function new() {}
      }
      """;

    assertEquals("""
      class Foo extends Base
              implements Drawable
              implements Resizable {
          public function new() {}
      }
      """, reformat(commonSettings(common -> common.EXTENDS_LIST_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS), source));
  }

  /**
   * Metadata sits beside its declaration in the PSI; a declaration opened
   * by same-line metadata still anchors its wrapped parts at its own indent:
   * the extends list two steps in, a member's parameters two steps from the
   * member, the next-line body brace at the member's level.
   */
  @Test
  @DisplayName("same line metadata keeps wrap anchors")
  public void testSameLineMetadataKeepsWrapAnchors() {
    Consumer<CommonCodeStyleSettings> configure = common -> {
      common.EXTENDS_LIST_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS;
      common.METHOD_PARAMETERS_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS;
      common.ALIGN_MULTILINE_PARAMETERS = false;
      common.BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE;
      common.METHOD_BRACE_STYLE = CommonCodeStyleSettings.NEXT_LINE;
    };
    String source = """
      @:keep class Foo extends Base implements Drawable implements Resizable {
      	@:keep public function new(first:Int, second:Int) {
      		trace(first);
      	}
      }
      """;

    assertEquals("""
      @:keep class Foo extends Base
              implements Drawable
              implements Resizable
      {
          @:keep public function new(first:Int,
                  second:Int)
          {
              trace(first);
          }
      }
      """, reformat(commonSettings(configure), source));
  }

  @Test
  @DisplayName("spaces within parentheses survive binary wrapping")
  public void testSpacesWithinParenthesesSurviveBinaryWrapping() {
    Consumer<CommonCodeStyleSettings> wrapAndSpace = common -> {
      common.BINARY_OPERATION_WRAP = CommonCodeStyleSettings.WRAP_AS_NEEDED;
      common.SPACE_WITHIN_PARENTHESES = true;
    };
    String source = """
      class Main {
      	static function main() {
      		var total = (1 + 2) * 3;
      	}
      }
      """;

    assertEquals("""
      class Main {
          static function main() {
              var total = ( 1 + 2 ) * 3;
          }
      }
      """, reformat(commonSettings(wrapAndSpace), source));
  }

  @Test
  @DisplayName("chain item count rule alone")
  public void testChainItemCountRuleAlone() {
    // every threshold stands on its own: with the line-length rules at 0
    // (never), the operand count alone splits a chain one per line, a
    // three-operand chain stays
    Consumer<HaxeCodeStyleSettings> countOnly = haxe -> {
      haxe.BOOL_CHAIN_SPLIT_ITEM_COUNT = 4;
      haxe.INDENT_WRAPPED_OPERATOR_CHAINS = true;
    };
    String source = """
      class Main {
      	static function main() {
      		var few = a > b && b > c;
      		var many = a > b && b > c && c > d && d > e;
      	}
      }
      """;

    String formatted = reformat(haxeSettings(countOnly), source);

    assertEquals("""
      class Main {
          static function main() {
              var few = a > b && b > c;
              var many = a > b
                  && b > c
                  && c > d
                  && d > e;
          }
      }
      """, formatted);
  }

  @Test
  @DisplayName("value if placement")
  public void testValueIfPlacement() {
    // KEEP (the plain default) re-breaks a value if exactly where the source
    // broke - a forced break, so even a pass that drops custom line breaks
    // never pulls the else up - and spaces a snug body; SAME_LINE joins the
    // whole value onto one line
    String source = """
      class Main {
      	static function main() {
      		var mode = if (true)
      			"debug"
      		else
      			"release";
      		var a = if (true)1 else 2;
      	}
      }
      """;

    String kept = reformat(commonSettings(common -> common.KEEP_LINE_BREAKS = false), source);
    assertEquals("""
      class Main {
          static function main() {
              var mode = if (true)
                  "debug"
              else
                  "release";
              var a = if (true) 1 else 2;
          }
      }
      """, kept);

    Consumer<HaxeCodeStyleSettings> sameLine = haxe -> haxe.VALUE_IF_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE;
    String joined = reformat(haxeSettings(sameLine), source);
    assertEquals("""
      class Main {
          static function main() {
              var mode = if (true) "debug" else "release";
              var a = if (true) 1 else 2;
          }
      }
      """, joined);
  }

  @Test
  @DisplayName("body placement per construct")
  public void testBodyPlacementPerConstruct() {
    // each construct's non-block body follows its own placement: SAME_LINE
    // joins a broken for-body onto the header, NEXT_LINE breaks an inline
    // catch-body off it
    Consumer<HaxeCodeStyleSettings> placements = haxe -> {
      haxe.FOR_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_SAME_LINE;
      haxe.CATCH_BODY_PLACEMENT = HaxeCodeStyleSettings.BODY_PLACEMENT_NEXT_LINE;
    };
    String source = """
      class Main {
      	static function main() {
      		for (i in 0...3)
      			trace(i);
      		try {
      			trace("x");
      		} catch (e:Dynamic) trace(e);
      	}
      }
      """;

    String formatted = reformat(haxeSettings(placements), source);

    assertEquals("""
      class Main {
          static function main() {
              for (i in 0...3) trace(i);
              try {
                  trace("x");
              } catch (e:Dynamic)
                  trace(e);
          }
      }
      """, formatted);
  }

  @Test
  @DisplayName("object chop item count rule alone")
  public void testObjectChopItemCountRuleAlone() {
    // the count threshold stands on its own: four fields go one per line
    // however short the line, three written on one line stay as written
    Consumer<HaxeCodeStyleSettings> countOnly = haxe -> haxe.OBJECT_CHOP_ITEM_COUNT = 4;
    String source = """
      class Main {
      	static function main() {
      		var three = {a: 1, b: 2, c: 3};
      		var four = {a: 1, b: 2, c: 3, d: 4};
      	}
      }
      """;

    String formatted = reformat(haxeSettings(countOnly), source);

    assertEquals("""
      class Main {
          static function main() {
              var three = {a: 1, b: 2, c: 3};
              var four = {
                  a: 1,
                  b: 2,
                  c: 3,
                  d: 4
              };
          }
      }
      """, formatted);
  }

  @Test
  @DisplayName("array chop item count rule alone")
  public void testArrayChopItemCountRuleAlone() {
    // the count threshold stands on its own: four items go one per line
    // however short the line, three stay as written
    Consumer<HaxeCodeStyleSettings> countOnly = haxe -> haxe.ARRAY_CHOP_ITEM_COUNT = 4;
    String source = """
      class Main {
      	static function main() {
      		var three = [1, 2, 3];
      		var four = [1, 2, 3, 4];
      	}
      }
      """;

    String formatted = reformat(haxeSettings(countOnly), source);

    assertEquals("""
      class Main {
          static function main() {
              var three = [1, 2, 3];
              var four = [
                  1,
                  2,
                  3,
                  4
              ];
          }
      }
      """, formatted);
  }
}
