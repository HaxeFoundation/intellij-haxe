package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.lineMarker.RunLineMarkerContributor.Info;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeMainClasses;
import com.intellij.plugins.haxe.v2.runconfig.HaxeMainClassLaunches.MainLaunch;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The class a build starts from carries that build's Build &amp; run launches in the gutter. */
@DisplayName("Run configurations: main class launches")
public class HaxeMainClassLaunchesTest extends HaxeCodeInsightFixtureTestCase {
  private static final String BUILD_HXML_SOURCE = "-cp src\n-main Main\n-hl out/main.hl\n";
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

  @Test
  @DisplayName("an hxml build launches from its main class")
  public void testAnHxmlBuildLaunchesFromItsMainClass() {
    myFixture.addFileToProject("build.hxml", BUILD_HXML_SOURCE);
    PsiFile main = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);
    PsiFile helper = myFixture.addFileToProject("src/Helper.hx", "class Helper {}\n");

    List<MainLaunch> mainLaunches = HaxeMainClassLaunches.launchesOf(classIn(main));
    List<MainLaunch> helperLaunches = HaxeMainClassLaunches.launchesOf(classIn(helper));

    assertEquals(List.of(HaxeLaunchKind.HASHLINK), kindsOf(mainLaunches));
    assertEquals("build.hxml", mainLaunches.getFirst().buildFile().file().getName());
    assertTrue(helperLaunches.isEmpty(), "a class no build starts from gets no launch");
  }

  @Test
  @DisplayName("a tests build gets no launch")
  public void testATestsBuildGetsNoLaunch() {
    myFixture.addFileToProject("test.hxml", "-cp test\n-lib utest\n-main TestMain\n-hl out/test.hl\n");
    PsiFile testMain = myFixture.addFileToProject("test/TestMain.hx", "class TestMain {\n  static function main() {}\n}\n");

    List<MainLaunch> launches = HaxeMainClassLaunches.launchesOf(classIn(testMain));

    assertTrue(launches.isEmpty(), "its main runs a test runner, which the test gutter covers");
  }

  @Test
  @DisplayName("the gutter marks the static main with run and debug")
  public void testTheGutterMarksTheStaticMainWithRunAndDebug() {
    myFixture.addFileToProject("build.hxml", BUILD_HXML_SOURCE);
    PsiFile main = myFixture.addFileToProject("src/Main.hx", MAIN_HX_SOURCE);

    Info onMain = infoAt(main, "main()");
    Info onClass = infoAt(main, "Main {");

    assertNotNull(onMain, "the static main carries the mark");
    assertEquals(2, onMain.actions.length, "Run and Debug for the HashLink build");
    assertNull(onClass, "a class with a static main is marked on the main, not on its name");
  }

  @Test
  @DisplayName("a lime project starts from its app main")
  public void testALimeProjectStartsFromItsAppMain() {
    PsiFile projectXml = myFixture.addFileToProject("project.xml", """
      <project>
        <app main="Game" file="Game"/>
        <source path="src"/>
      </project>
      """);
    PsiFile game = myFixture.addFileToProject("src/Game.hx", "class Game {}\n");
    HaxeBuildFile buildFile = new HaxeBuildFile(projectXml.getVirtualFile(), HaxeBuildFileType.LIME);

    HaxeClass mainClass = HaxeMainClasses.mainClassOf(getProject(), buildFile);

    assertSame(classIn(game), mainClass);
  }

  private static HaxeClass classIn(PsiFile file) {
    HaxeClass haxeClass = PsiTreeUtil.findChildOfType(file, HaxeClass.class);
    assertNotNull(haxeClass);
    return haxeClass;
  }

  private static List<HaxeLaunchKind> kindsOf(List<MainLaunch> launches) {
    return launches.stream()
      .map(MainLaunch::kind)
      .toList();
  }

  /** The gutter info of the leaf the text starts at. */
  private static Info infoAt(PsiFile file, String text) {
    int offset = file.getText().indexOf(text);
    assertTrue(offset >= 0, text);
    return new HaxeMainClassRunLineMarkerContributor().getInfo(file.findElementAt(offset));
  }
}
