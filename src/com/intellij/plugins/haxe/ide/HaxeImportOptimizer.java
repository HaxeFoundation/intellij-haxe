package com.intellij.plugins.haxe.ide;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.util.HaxeImportUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Created by fedorkorotkov.
 */
public class HaxeImportOptimizer implements ImportOptimizer {
  @Override
  public boolean supports(PsiFile file) {
    return file instanceof HaxeFile;
  }

  @NotNull
  @Override
  public Runnable processFile(final PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile instanceof VirtualFileWindow) vFile = ((VirtualFileWindow)vFile).getDelegate();
    if (vFile == null || !ProjectRootManager.getInstance(file.getProject()).getFileIndex().isInSourceContent(vFile)) {
      return EmptyRunnable.INSTANCE;
    }

    return new Runnable() {
      @Override
      public void run() {
        optimizeImports(file);
      }
    };
  }

  private static void optimizeImports(PsiFile file) {
    for (HaxeImportStatement unusedImportStatement : HaxeImportUtil.findUnusedImports(file)) {
      unusedImportStatement.delete();
    }
    // todo: rearrange
  }
}
