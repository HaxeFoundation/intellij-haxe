package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Resolve: qname resolve util")
public class HaxeQnameResolveUtilTest extends HaxeLightFixtureTestCase {

  /** The test-data root the fixture requires; these tests create their files in the project instead. */
  @Override
  protected String getBasePath() {
    return "/resolve/";
  }

  /** A debugger frame named pkg.Cls.method: both copies of pkg.Cls qualify, the package-less Cls does not. */
  @Test
  @DisplayName("finds every copy of a packaged class by its runtime name")
  public void testFindsEveryCopyOfAPackagedClassByItsRuntimeName() {
    myFixture.addFileToProject("app/src/pkg/Cls.hx", "package pkg;\nclass Cls {\n  public function method() {}\n}\n");
    myFixture.addFileToProject("other/src/pkg/Cls.hx", "package pkg;\nclass Cls {}\n");
    myFixture.addFileToProject("Cls.hx", "class Cls {}\n");

    List<HaxeClass> classes = ReadAction.compute(() -> HaxeQnameResolveUtil.findClassesByRuntimeName("pkg.Cls.method", getProject()));

    List<String> paths = classes.stream().map(HaxeQnameResolveUtilTest::pathOf).sorted().toList();
    assertEquals(2, paths.size(), "both packaged copies, got: " + paths);
    assertTrue(paths.stream().allMatch(path -> path.endsWith("/src/pkg/Cls.hx")), paths.toString());
  }

  @Test
  @DisplayName("a name that qualifies no class denotes none")
  public void testANameThatQualifiesNoClassDenotesNone() {
    myFixture.addFileToProject("Cls.hx", "class Cls {}\n");

    List<HaxeClass> standalone = ReadAction.compute(() -> HaxeQnameResolveUtil.findClassesByRuntimeName("fn@12", getProject()));
    List<HaxeClass> unknown = ReadAction.compute(() -> HaxeQnameResolveUtil.findClassesByRuntimeName("?", getProject()));

    assertTrue(standalone.isEmpty());
    assertTrue(unknown.isEmpty());
  }

  private static String pathOf(HaxeClass haxeClass) {
    return haxeClass.getContainingFile().getVirtualFile().getPath();
  }
}
