/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.Usage;
import com.intellij.usages.rules.ImportFilteringRule;
import com.intellij.usages.rules.PsiElementUsage;
import org.jetbrains.annotations.NotNull;

public class HaxeImportFilteringRule extends ImportFilteringRule {
  @Override
  public boolean isVisible(@NotNull Usage usage) {
    if (usage instanceof PsiElementUsage) {
      final PsiElement psiElement = ((PsiElementUsage)usage).getElement();
      final PsiFile containingFile = psiElement.getContainingFile();

      if (containingFile != null && containingFile.getFileType() == HaxeFileType.HAXE_FILE_TYPE) {
        return PsiTreeUtil.getParentOfType(psiElement, HaxeImportStatement.class) == null;
      }
    }
    return true;
  }
}
