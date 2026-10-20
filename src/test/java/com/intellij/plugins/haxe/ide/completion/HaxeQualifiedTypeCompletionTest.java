package com.intellij.plugins.haxe.ide.completion;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text a type completion leaves behind. A type declared inside another
 * module is written with the module in its path ({@code pack.Module.Type}),
 * a module's main type without it ({@code pack.Module}).
 */
@DisplayName("Completion: qualified type insertion")
public class HaxeQualifiedTypeCompletionTest extends HaxeLightFixtureTestCase {

  private static final String TOOLS_HX_NAME = "pack/sub/Tools.hx";
  private static final String TOOLS_HX_SOURCE = """
    package pack.sub;

    class StringHelpers {}
    """;
  private static final String REFLECT_HX_NAME = "pack/sub/Reflect.hx";
  private static final String REFLECT_HX_SOURCE = """
    package pack.sub;

    class Reflect {}

    class Kind {
      public function new(value:Int) {}
    }
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a secondary type after its package qualifier gets its module, once")
  public void testASecondaryTypeAfterItsPackageQualifierGetsItsModuleOnce() {
    myFixture.addFileToProject(REFLECT_HX_NAME, REFLECT_HX_SOURCE);
    myFixture.configureByText("Main.hx", """
      class Main {
        static function main() {
          pack.sub.Kin<caret>;
        }
      }
      """);

    myFixture.completeBasic();

    myFixture.checkResult("""
      class Main {
        static function main() {
          pack.sub.Reflect.Kind<caret>;
        }
      }
      """);
  }

  @Test
  @DisplayName("a constructed secondary type keeps the caret inside its arguments")
  public void testAConstructedSecondaryTypeKeepsTheCaretInsideItsArguments() {
    myFixture.addFileToProject(REFLECT_HX_NAME, REFLECT_HX_SOURCE);
    myFixture.configureByText("Main.hx", """
      class Main {
        static function main() {
          new pack.sub.Kin<caret>;
        }
      }
      """);

    myFixture.completeBasic();

    myFixture.checkResult("""
      class Main {
        static function main() {
          new pack.sub.Reflect.Kind(<caret>);
        }
      }
      """);
  }

  @Test
  @DisplayName("a module main type after its package qualifier stays as typed")
  public void testAModuleMainTypeAfterItsPackageQualifierStaysAsTyped() {
    myFixture.addFileToProject(REFLECT_HX_NAME, REFLECT_HX_SOURCE);
    myFixture.configureByText("Main.hx", """
      class Main {
        static function main() {
          pack.sub.Refl<caret>;
        }
      }
      """);

    myFixture.completeBasic();

    myFixture.checkResult("""
      class Main {
        static function main() {
          pack.sub.Reflect<caret>;
        }
      }
      """);
  }

  @Test
  @DisplayName("a package lists a module only when no same named type stands for it")
  public void testAPackageListsAModuleOnlyWhenNoSameNamedTypeStandsForIt() {
    myFixture.addFileToProject(REFLECT_HX_NAME, REFLECT_HX_SOURCE);
    myFixture.addFileToProject(TOOLS_HX_NAME, TOOLS_HX_SOURCE);
    myFixture.configureByText("Main.hx", """
      class Main {
        static function main() {
          pack.sub.<caret>;
        }
      }
      """);

    myFixture.completeBasic();
    List<String> items = myFixture.getLookupElementStrings();

    assertNotNull(items);
    assertEquals(1, Collections.frequency(items, "Reflect"), "Reflect.hx's main type stands for the module: " + items);
    assertTrue(items.contains("Tools"), "Tools.hx has no type of its name, so the module itself is offered: " + items);
    assertTrue(items.contains("StringHelpers"), items.toString());
  }

  @Test
  @DisplayName("a secondary type its module import brings in stays short")
  public void testASecondaryTypeItsModuleImportBringsInStaysShort() {
    myFixture.addFileToProject(REFLECT_HX_NAME, REFLECT_HX_SOURCE);
    myFixture.configureByText("Main.hx", """
      import pack.sub.Reflect;

      class Main {
        static function main() {
          var k:Kin<caret>;
        }
      }
      """);

    myFixture.completeBasic();

    myFixture.checkResult("""
      import pack.sub.Reflect;

      class Main {
        static function main() {
          var k:Kind<caret>;
        }
      }
      """);
  }
}
