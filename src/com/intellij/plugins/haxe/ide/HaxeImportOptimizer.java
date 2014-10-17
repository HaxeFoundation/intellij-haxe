/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementRegular;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementWithInSupport;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementWithWildcard;
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
    for (HaxeImportStatementRegular unusedImportStatement : HaxeImportUtil.findUnusedImports(file)) {
      unusedImportStatement.delete();
    }

    for (HaxeImportStatementWithInSupport unusedImportStatement : HaxeImportUtil.findUnusedInImports(file)) {
      unusedImportStatement.delete();
    }

    for (HaxeImportStatementWithWildcard unusedImportStatement : HaxeImportUtil.findUnusedInImportsWithWildcards(file)) {
      unusedImportStatement.delete();
    }

    // todo: rearrange
  }
}
