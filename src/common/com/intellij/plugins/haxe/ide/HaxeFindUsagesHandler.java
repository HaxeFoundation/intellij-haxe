/*
 * Copyright 2017 Eric Bishton
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

import com.intellij.find.findUsages.AbstractFindUsagesDialog;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeFindUsagesHandler extends FindUsagesHandler {

  public static final PsiElement[] NO_ELEMENTS = {};

  private final PsiElement[] extraElementsToSearch;

  protected HaxeFindUsagesHandler(@NotNull PsiElement psiElement) {
    this(psiElement, NO_ELEMENTS);
  }

  protected HaxeFindUsagesHandler(@NotNull PsiElement psiElement, @Nullable PsiElement[] others) {
    super(psiElement);
    extraElementsToSearch = others != null ? others : NO_ELEMENTS;
  }

  @NotNull
  @Override
  public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
    return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab);
  }

  @NotNull
  @Override
  public PsiElement[] getPrimaryElements() {
    // Return the element.  In Java, this checks for the named parameter in overriding methods (and lamdas) and adds them to to search scope.
    // See @{JavaFindUsagesProvider#getPrimaryElements}

    // We check for the presence of overriding methods, et cetera, before we create this handler.

    return ArrayUtil.mergeArrays(super.getPrimaryElements(), extraElementsToSearch);
  }

  @NotNull
  @Override
  public PsiElement[] getSecondaryElements() {
    // In Java, if the element is a property, return getters and setters that reference it.
    return super.getSecondaryElements();
  }

}
