package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTestsBuildFileStore;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ownership resolution behind the gutter markers and context-menu runs, over an explicitly MARKED
 * build with a deliberately NON-conventional name (checks.hxml) so the mark
 * itself is load-bearing: the context appears with the mark, follows the
 * classpaths, and drops on unmark (the store's modification tracker
 * invalidates the cache). The conventional-name path is covered by the
 * line-marker test.
 */
@DisplayName("Test runner: test context")
public class HaxeTestContextTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("file under the marked builds classpaths gets its framework")
  public void testFileUnderTheMarkedBuildsClasspathsGetsItsFramework() {
    VirtualFile buildFile = myFixture.copyFileToProject("utest/test.hxml", "sub/checks.hxml");
    VirtualFile source = myFixture.copyFileToProject("src/TestMain.hx", "sub/src/TestMain.hx");
    HaxeTestsBuildFileStore.getInstance(getProject()).markTestsFile("container", buildFile.getPath());

    HaxeTestContext context = HaxeTestContext.forFile(psiFile(source));
    assertNotNull(context, "the marked build's classpaths contain the file");
    assertEquals("utest", context.framework().libraryName(), "no -lib in the build falls to the utest default");
    assertEquals(buildFile.getPath(), context.testsBuildPath());
  }

  @Test
  @DisplayName("file outside every marked build has no context")
  public void testFileOutsideEveryMarkedBuildHasNoContext() {
    VirtualFile buildFile = myFixture.copyFileToProject("utest/test.hxml", "sub/checks.hxml");
    VirtualFile outside = myFixture.copyFileToProject("src/TestMain.hx", "elsewhere/TestMain.hx");
    HaxeTestsBuildFileStore.getInstance(getProject()).markTestsFile("container", buildFile.getPath());

    assertNull(HaxeTestContext.forFile(psiFile(outside)),
               "a file no marked tests build claims gets no runs");
  }

  @Test
  @DisplayName("unmarking the build drops the context")
  public void testUnmarkingTheBuildDropsTheContext() {
    VirtualFile buildFile = myFixture.copyFileToProject("utest/test.hxml", "sub/checks.hxml");
    VirtualFile source = myFixture.copyFileToProject("src/TestMain.hx", "sub/src/TestMain.hx");
    HaxeTestsBuildFileStore store = HaxeTestsBuildFileStore.getInstance(getProject());

    store.markTestsFile("container", buildFile.getPath());
    assertNotNull(HaxeTestContext.forFile(psiFile(source)));

    store.unmarkTestsFile("container", buildFile.getPath());
    assertNull(HaxeTestContext.forFile(psiFile(source)),
               "the store change must invalidate the cached context");
  }

  @Test
  @DisplayName("lime tests build claims files by its declared sources")
  public void testLimeTestsBuildClaimsFilesByItsDeclaredSources() {
    VirtualFile buildFile = myFixture.copyFileToProject("targets/lime-project.xml", "limeproj/project.xml");
    VirtualFile source = myFixture.copyFileToProject("src/TestMain.hx", "limeproj/src/TestMain.hx");
    HaxeTestsBuildFileStore.getInstance(getProject()).markTestsFile("container", buildFile.getPath());

    HaxeTestContext context = HaxeTestContext.forFile(psiFile(source));
    assertNotNull(context, "the lime build's declared sources contain the file");
    assertEquals("utest", context.framework().libraryName(), "the declared utest haxelib drives the framework");
    assertEquals(buildFile.getPath(), context.testsBuildPath());
  }

  @Test
  @DisplayName("conventional lime project under a tests directory needs no mark")
  public void testConventionalLimeProjectUnderATestsDirectoryNeedsNoMark() {
    VirtualFile buildFile = myFixture.copyFileToProject("targets/lime-project.xml", "limeproj/tests/project.xml");
    VirtualFile source = myFixture.copyFileToProject("src/TestMain.hx", "limeproj/tests/src/TestMain.hx");

    HaxeTestContext context = HaxeTestContext.forFile(psiFile(source));
    assertNotNull(context, "a lime project under a tests directory is a conventional candidate");
    assertEquals(buildFile.getPath(), context.testsBuildPath());
  }

  private PsiFile psiFile(VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    assertNotNull(psiFile);
    return psiFile;
  }
}
