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

import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeFindUsagesUtil {

  /**
   * Selected element is usually a COMPONENT_NAME, not a named element that we can get
   * typing, etc, from.  And it's no longer always the parent.  So go find the appropriate
   * element that can be searched for.
   *
   * @param psiElement - lowest level element found at a cursor position -- usually a COMPONENT_NAME.
   * @return a higher level PsiElement that has context and typing information.
   */
  @Nullable
  public static PsiElement getTargetElement(@NotNull final PsiElement psiElement) {
    PsiElement parent = PsiTreeUtil.getParentOfType(psiElement, HaxeNamedComponent.class, false);
    return parent;
  }
}
