package com.intellij.plugins.haxe.v2.testing;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.psi.PsiFile;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The gutter markers end to end through real highlighting: a conventional
 * (never explicitly marked) test.hxml owning the file produces class and
 * method run markers, files outside its classpaths get none. Exercises the
 * whole chain - ownership resolution, framework detection and the
 * contributor's PSI shape.
 */
@DisplayName("Test runner: run line marker contributor")
public class HaxeTestRunLineMarkerContributorTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/";
  }

  private PsiFile configureOwnedTestClass() {
    myFixture.addFileToProject("proj/utest/ITest.hx", "package utest;\ninterface ITest {}\n");
    myFixture.addFileToProject("proj/utest/Test.hx", "package utest;\nclass Test implements ITest {}\n");
    myFixture.addFileToProject("proj/test.hxml", "-cp .\n-lib utest\n--main CalcTest\n--interp\n");
    return myFixture.addFileToProject("proj/CalcTest.hx", """
      class CalcTest extends utest.Test {
      	public function testAdd() {}
      }
      """);
  }

  @Test
  @DisplayName("class and method markers appear for a conventionally owned file")
  public void testClassAndMethodMarkersAppearForAConventionallyOwnedFile() {
    PsiFile caseFile = configureOwnedTestClass();
    myFixture.configureFromExistingVirtualFile(caseFile.getVirtualFile());

    List<GutterMark> gutters = myFixture.findAllGutters();
    boolean classMarker = gutters.stream()
      .anyMatch(gutter -> "Run 'CalcTest'".equals(gutter.getTooltipText()));
    boolean methodMarker = gutters.stream()
      .anyMatch(gutter -> "Run 'CalcTest.testAdd'".equals(gutter.getTooltipText()));
    assertTrue(classMarker, "class marker expected, got: " + tooltips(gutters));
    assertTrue(methodMarker, "method marker expected, got: " + tooltips(gutters));
  }

  @Test
  @DisplayName("files outside the tests builds classpaths get no markers")
  public void testFilesOutsideTheTestsBuildsClasspathsGetNoMarkers() {
    configureOwnedTestClass();
    PsiFile outside = myFixture.addFileToProject("elsewhere/OtherTest.hx", """
      class OtherTest extends utest.Test {
      	public function testAdd() {}
      }
      """);
    myFixture.configureFromExistingVirtualFile(outside.getVirtualFile());

    boolean anyRunMarker = myFixture.findAllGutters().stream().anyMatch(HaxeTestRunLineMarkerContributorTest::isRunMarker);
    assertFalse(anyRunMarker, "no owning tests build - no run markers");
  }

  private static boolean isRunMarker(GutterMark gutter) {
    String tooltip = gutter.getTooltipText();
    return tooltip != null && tooltip.startsWith("Run '");
  }

  private static String tooltips(List<GutterMark> gutters) {
    return gutters.stream().map(GutterMark::getTooltipText).toList().toString();
  }
}
