package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTestsBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeWorkDirectoryStore;
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
  @DisplayName("the most specific classpath owns a file when several marked builds contain it")
  public void testTheMostSpecificClasspathOwnsAFileWhenSeveralMarkedBuildsContainIt() {
    VirtualFile rootBuild = myFixture.copyFileToProject("utest/test.hxml", "test.hxml");
    VirtualFile rootSource = myFixture.copyFileToProject("src/TestMain.hx", "src/TestMain.hx");
    VirtualFile tinkBuild = myFixture.copyFileToProject("tink/test.hxml", "tink/test.hxml");
    VirtualFile tinkSource = myFixture.copyFileToProject("tink/src/TinkCase.hx", "tink/src/TinkCase.hx");
    HaxeTestsBuildFileStore store = HaxeTestsBuildFileStore.getInstance(getProject());
    store.markTestsFile("container", rootBuild.getPath());
    store.markTestsFile("container", tinkBuild.getPath());

    HaxeTestContext tinkContext = HaxeTestContext.forFile(psiFile(tinkSource));
    assertNotNull(tinkContext);
    assertEquals(tinkBuild.getPath(), tinkContext.testsBuildPath(),
                 "the root build's own folder contains the file too, but the nested build's classpath is more specific");
    assertInstanceOf(TinkFramework.class, tinkContext.framework());

    HaxeTestContext rootContext = HaxeTestContext.forFile(psiFile(rootSource));
    assertNotNull(rootContext);
    assertEquals(rootBuild.getPath(), rootContext.testsBuildPath(), "a file only the root build contains stays with it");
  }

  @Test
  @DisplayName("the work directory is an implicit classpath, the build files own folder is not")
  public void testTheWorkDirectoryIsAnImplicitClasspathTheBuildFilesOwnFolderIsNot() {
    // the build runs from src/ (override), so haxe's implicit classpath is
    // src/, and conf/ beside the hxml is no classpath at all
    VirtualFile buildFile = myFixture.copyFileToProject("utest/test.hxml", "conf/test.hxml");
    VirtualFile inWorkDirectory = myFixture.copyFileToProject("src/TestMain.hx", "src/TestMain.hx");
    VirtualFile besideBuildFile = myFixture.copyFileToProject("src/TestMain.hx", "conf/ConfCase.hx");
    HaxeWorkDirectoryStore workDirectories = HaxeWorkDirectoryStore.getInstance(getProject());
    workDirectories.setWorkDirectory(buildFile.getPath(), inWorkDirectory.getParent().getPath());
    HaxeTestsBuildFileStore.getInstance(getProject()).markTestsFile("container", buildFile.getPath());

    HaxeTestContext context = HaxeTestContext.forFile(psiFile(inWorkDirectory));
    assertNotNull(context, "a file in the work directory compiles through the implicit classpath");
    assertEquals(buildFile.getPath(), context.testsBuildPath());
    assertNull(HaxeTestContext.forFile(psiFile(besideBuildFile)),
               "haxe does not look beside the hxml, only in the directory it runs in");

    workDirectories.setWorkDirectory(buildFile.getPath(), null);
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
