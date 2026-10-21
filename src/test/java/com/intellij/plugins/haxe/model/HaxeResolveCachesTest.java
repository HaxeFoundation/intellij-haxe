package com.intellij.plugins.haxe.model;

import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Resolve caches: roots change")
public class HaxeResolveCachesTest extends HaxeCodeInsightFixtureTestCase {
  private static final String LIBRARY_NAME = "haxelib: fake";

  @TempDir
  Path libraryDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The library sync a project open starts would replace the haxelib entries the tests attach. */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    awaitLibrarySync();
  }

  @Test
  @DisplayName("a type stops resolving once its library is removed")
  public void testATypeStopsResolvingOnceItsLibraryIsRemoved() throws IOException {
    addLibrary("LibType.hx", "class LibType {}");
    VirtualFile main = myFixture.addFileToProject("src/Main.hx", "class Main { var field:LibType; }").getVirtualFile();
    assertNotNull(resolveLibType(main), "the library type resolves while the library is attached");

    removeLibrary();

    assertNull(resolveLibType(main), "the reference must not answer from a cache that outlived the library");
  }

  private void addLibrary(String fileName, String source) throws IOException {
    Path root = Files.createDirectories(libraryDirectory.resolve("lib"));
    Files.writeString(root.resolve(fileName), source);
    VirtualFile rootFile = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root));
    ModuleRootModificationUtil.addModuleLibrary(myFixture.getModule(), LIBRARY_NAME, List.of(rootFile.getUrl()), List.of());
  }

  private void removeLibrary() {
    ModuleRootModificationUtil.updateModel(myFixture.getModule(), model -> {
      LibraryTable table = model.getModuleLibraryTable();
      table.removeLibrary(Objects.requireNonNull(table.getLibraryByName(LIBRARY_NAME)));
    });
  }

  @Nullable
  private PsiElement resolveLibType(VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    assertNotNull(psiFile);
    PsiReference reference = psiFile.findReferenceAt(psiFile.getText().indexOf("LibType"));
    assertNotNull(reference);
    return reference.resolve();
  }
}
