package com.intellij.plugins.haxe.v2.compiler;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Container resolution behind {@link HaxeLanguageLevelUtil#getLanguageLevel}:
 * a library/SDK file (outside every content root) follows the ACTIVE build
 * container's level — the same source the define context derives haxe_ver
 * from — so level-gated checks agree with which {@code #if haxe_ver} block
 * is active in library sources.
 */
@DisplayName("Compiler: language level container resolution")
public class HaxeLanguageLevelContainerResolutionTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/annotation.languagelevel/";
  }

  @Test
  @DisplayName("library file follows the active build containers level")
  public void testLibraryFileFollowsTheActiveBuildContainersLevel() throws Exception {
    VirtualFile buildFile = myFixture.addFileToProject("build.hxml", "-cp src\n--main Main\n").getVirtualFile();
    HaxeBuildFilesStore.getInstance(getProject()).addFile(getModule().getName(), buildFile.getPath());
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(buildFile.getPath());
    HaxeCompilerSettings.getInstance(getProject()).setContainerLanguageLevelOverride(getModule().getName(), HaxeLanguageLevel.HAXE_4_0);

    PsiFile libraryFile = libraryPsiFile();

    assertEquals(HaxeLanguageLevel.HAXE_4_0, HaxeLanguageLevelUtil.getLanguageLevel(libraryFile),
                 "a file outside every content root must resolve the active container's override, not the project default");
  }

  @Test
  @DisplayName("library file uses the project default without an active build file")
  public void testLibraryFileUsesTheProjectDefaultWithoutAnActiveBuildFile() throws Exception {
    HaxeCompilerSettings.getInstance(getProject()).setDefaultLanguageLevel(HaxeLanguageLevel.HAXE_4_2);

    PsiFile libraryFile = libraryPsiFile();

    assertEquals(HaxeLanguageLevel.HAXE_4_2, HaxeLanguageLevelUtil.getLanguageLevel(libraryFile));
  }

  /** A haxe file OUTSIDE every content root, standing in for a haxelib/std source. */
  @NotNull
  private PsiFile libraryPsiFile() throws Exception {
    File libraryDir = FileUtil.createTempDirectory("haxelib", null);
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), libraryDir.getAbsolutePath());
    File libraryType = new File(libraryDir, "LibraryType.hx");
    Files.writeString(libraryType.toPath(), "class LibraryType {}\n");

    VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(libraryType.toPath());
    assertNotNull(file, "library fixture file on disk");
    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    assertNotNull(psiFile, "psi for the library fixture file");
    return psiFile;
  }
}
