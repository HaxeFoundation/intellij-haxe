package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the PSI file behind a COMPILE-RELATIVE source path from a test
 * reporter's position info ({@code test/CalculatorTest.hx}) — the compiler
 * prints paths as it saw them, relative to the compile's working directory,
 * so the project file is matched by path suffix.
 */
public final class HaxeTestFileLocation {

  private HaxeTestFileLocation() {
  }

  @Nullable
  public static PsiFile find(@NotNull Project project,
                             @NotNull GlobalSearchScope scope,
                             @NotNull String compileRelativePath) {
    String normalized = compileRelativePath.replace('\\', '/');
    String simpleName = normalized.substring(normalized.lastIndexOf('/') + 1);
    for (VirtualFile candidate : FilenameIndex.getVirtualFilesByName(simpleName, scope)) {
      if (candidate.getPath().endsWith("/" + normalized) || candidate.getPath().equals(normalized)) {
        return PsiManager.getInstance(project).findFile(candidate);
      }
    }
    return null;
  }
}
