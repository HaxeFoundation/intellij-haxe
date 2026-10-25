package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: main classes")
public class HaxeMainClassesTest extends HaxeCodeInsightFixtureTestCase {
  private static final String MAIN_HX_SOURCE = """
    class Main {
      static function main() {}
    }
    """;

  @Override
  protected String getBasePath() {
    // every fixture is created in-project by the test itself
    return "";
  }

  /** (what the build does, its hxml content, where Main.hx lives, whether Main is an entry class). */
  static final List<Arguments> ENTRY_CLASS_BUILDS = List.of(
    arguments("-main names it", "-cp src\n-main Main\n--interp\n", "src/Main.hx", true),
    arguments("a later section's -main names it", "-cp src\n-main Other\n--interp\n--next\n-cp src\n-main Main\n-js out.js\n", "src/Main.hx", true),
    arguments("--run starts it", "-cp src\n--run Main first second\n", "src/Main.hx", true),
    arguments("a tests build starts it", "-cp src\n-lib utest\n-main Main\n--interp\n", "src/Main.hx", true),
    arguments("-cp . holds it beside the build file", "-cp .\n-main Main\n--interp\n", "Main.hx", true),
    arguments("the work directory holds it without -cp", "-main Main\n--interp\n", "Main.hx", true),
    arguments("the build starts another class", "-cp src\n-main Other\n--interp\n", "src/Main.hx", false));

  @ParameterizedTest(name = "{0}")
  @FieldSource("ENTRY_CLASS_BUILDS")
  @DisplayName("entry class")
  public void testEntryClass(String description, String buildHxmlSource, String mainHxPath, boolean entryClass) {
    myFixture.addFileToProject("build.hxml", buildHxmlSource);
    PsiFile main = myFixture.addFileToProject(mainHxPath, MAIN_HX_SOURCE);

    HaxeClass mainClass = PsiTreeUtil.findChildOfType(main, HaxeClass.class);
    assertNotNull(mainClass);

    assertEquals(entryClass, HaxeMainClasses.isEntryClass(mainClass), description);
  }
}
