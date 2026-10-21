package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.Location;
import com.intellij.plugins.haxe.v2.testing.HaxeTestNameLocation;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The locator's {@code ?build=} tie-break, kept on the HEAVY fixture: the
 * classpath containment resolves the build file through the local filesystem
 * and compares real paths against the candidates' files, so the whole
 * arrangement must live on disk - the shared light project of
 * {@link HaxeTestLocatorTest} (every other locator check) cannot host it.
 */
@DisplayName("Test runner: locator build tie break")
public class HaxeTestLocatorBuildTieBreakTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/";
  }

  private PsiElement locate(String name) {
    List<Location> locations = HaxeTestLocator.INSTANCE.getLocation(
      HaxeTestNameLocation.PROTOCOL, name, getProject(), GlobalSearchScope.allScope(getProject()));
    return locations.isEmpty() ? null : locations.get(0).getPsiElement();
  }

  @Test
  @DisplayName("build suffix breaks same name ties between sibling projects")
  public void testBuildSuffixBreaksSameNameTiesBetweenSiblingProjects() {
    // one module can hold several sub-projects declaring the SAME
    // default-package class - the run's tests build file picks its own
    myFixture.addFileToProject("alpha/test/CalculatorTest.hx",
                               "class CalculatorTest {\n  public function testAdd():Void {}\n}\n");
    myFixture.addFileToProject("beta/test/CalculatorTest.hx",
                               "class CalculatorTest {\n  public function testAdd():Void {}\n}\n");
    String alphaBuild = myFixture.addFileToProject("alpha/test.hxml", "-cp test\n--main TestMain\n--interp\n")
      .getVirtualFile().getPath();
    String betaBuild = myFixture.addFileToProject("beta/test.hxml", "-cp test\n--main TestMain\n--interp\n")
      .getVirtualFile().getPath();

    PsiElement alpha = locate("CalculatorTest.testAdd?build=" + alphaBuild);
    assertInstanceOf(HaxeMethod.class, alpha);
    String alphaPath = alpha.getContainingFile().getVirtualFile().getPath();
    assertTrue(alphaPath.contains("/alpha/"), "the alpha build resolves its own class, got: " + alphaPath);

    PsiElement beta = locate("CalculatorTest.testAdd?build=" + betaBuild);
    assertInstanceOf(HaxeMethod.class, beta);
    String betaPath = beta.getContainingFile().getVirtualFile().getPath();
    assertTrue(betaPath.contains("/beta/"), "the beta build resolves its own class, got: " + betaPath);
  }
}
