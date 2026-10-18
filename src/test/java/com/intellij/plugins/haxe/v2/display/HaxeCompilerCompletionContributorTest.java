package com.intellij.plugins.haxe.v2.display;

import com.intellij.plugins.haxe.display.protocol.CompletionItem;
import com.intellij.plugins.haxe.display.protocol.ImportStatus;
import com.intellij.plugins.haxe.display.protocol.Position;
import com.intellij.plugins.haxe.display.protocol.Range;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerCompletionContributor.TypeInsertion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Completion: compiler completion contributor")
public class HaxeCompilerCompletionContributorTest {

  private static final String DOTTED_SOURCE = """
    import go.reflect.Reflect;
    class Main {
      static function main() {
        go.math.Ma
      }
    }
    """;
  private static final int CARET_LINE = 3;
  private static final int CARET_OFFSET = DOTTED_SOURCE.indexOf("go.math.Ma") + "go.math.Ma".length();

  @Test
  @DisplayName("a dotted replace range is the prefix")
  public void testADottedReplaceRangeIsThePrefix() {
    Range range = new Range(new Position(CARET_LINE, 4), new Position(CARET_LINE, 14));

    assertEquals("go.math.Ma", HaxeCompilerCompletionContributor.replacedText(DOTTED_SOURCE, range, CARET_OFFSET));
  }

  @Test
  @DisplayName("a range at another line is no prefix")
  public void testARangeAtAnotherLineIsNoPrefix() {
    // the compiler's range at the import's "go", lines above the caret
    Range importRange = new Range(new Position(0, 7), new Position(0, 10));

    assertNull(HaxeCompilerCompletionContributor.replacedText(DOTTED_SOURCE, importRange, CARET_OFFSET));
    assertNull(HaxeCompilerCompletionContributor.replacedText(DOTTED_SOURCE, null, CARET_OFFSET));
  }

  @Test
  @DisplayName("a dotted prefix matches types packages and modules by their dot path")
  public void testADottedPrefixMatchesTypesPackagesAndModulesByTheirDotPath() {
    CompletionItem type = new CompletionItem("Type", "Math", "go.math.Math", "class", null, null, 0, ImportStatus.UNIMPORTED);
    CompletionItem local = new CompletionItem("Local", "field", null, null, null, null, 1, null);

    assertEquals("go.math.Math", HaxeCompilerCompletionContributor.lookupString(type, true));
    assertEquals("Math", HaxeCompilerCompletionContributor.lookupString(type, false));
    assertEquals("field", HaxeCompilerCompletionContributor.lookupString(local, true));
  }

  /** (item kind, import status, whether the position qualifies the type, how the item is inserted). */
  static final List<Arguments> TYPE_INSERTIONS = List.of(
    arguments("Type", ImportStatus.UNIMPORTED, false, TypeInsertion.NAME_WITH_IMPORT),
    arguments("Type", ImportStatus.SHADOWED, false, TypeInsertion.QUALIFIED_PATH),
    arguments("Type", ImportStatus.IMPORTED, false, TypeInsertion.NAME),
    // after a package, or in an import path, the written text already qualifies the type
    arguments("Type", ImportStatus.UNIMPORTED, true, TypeInsertion.NAME),
    arguments("Type", ImportStatus.SHADOWED, true, TypeInsertion.NAME),
    arguments("Type", null, false, TypeInsertion.NAME),
    arguments("Local", null, false, TypeInsertion.NAME));

  @ParameterizedTest(name = "{0} {1}, qualified position {2}")
  @FieldSource("TYPE_INSERTIONS")
  @DisplayName("type insertion")
  public void testTypeInsertion(String kind, ImportStatus importStatus, boolean qualifiedPosition, TypeInsertion expected) {
    CompletionItem item = new CompletionItem(kind, "Thread", "sys.thread.Thread", null, null, null, 0, importStatus);

    assertEquals(expected, HaxeCompilerCompletionContributor.typeInsertion(item, qualifiedPosition));
  }
}
