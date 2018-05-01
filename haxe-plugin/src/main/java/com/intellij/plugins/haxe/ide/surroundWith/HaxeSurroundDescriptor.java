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
package com.intellij.plugins.haxe.ide.surroundWith;

import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.plugins.haxe.lang.psi.HaxeBlockStatement;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSurroundDescriptor implements SurroundDescriptor {
  @NotNull
  @Override
  public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
    final List<PsiElement> path = UsefulPsiTreeUtil.getPathToParentOfType(file.findElementAt(startOffset), HaxeBlockStatement.class);
    if (path == null || path.size() < 2) {
      return PsiElement.EMPTY_ARRAY;
    }
    final List<PsiElement> result = new ArrayList<PsiElement>();
    PsiElement child = path.get(path.size() - 2);
    while (child != null && child.getTextOffset() < endOffset) {
      result.add(child);
      child = child.getNextSibling();
    }
    return result.toArray(new PsiElement[result.size()]);
  }

  @NotNull
  @Override
  public Surrounder[] getSurrounders() {
    return new Surrounder[]{
      new HaxeIfSurrounder(),
      new HaxeIfElseSurrounder(),
      new HaxeWhileSurrounder(),
      new HaxeDoWhileSurrounder(),
      new HaxeTryCatchSurrounder()
    };
  }

  @Override
  public boolean isExclusive() {
    return false;
  }
}
