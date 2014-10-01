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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxePsiField extends PsiFieldImpl implements PsiField {

  private HaxeNamedComponent mHaxeNamedComponent;

  public HaxePsiField(@NotNull HaxeNamedComponent haxeNamedComponent) {
    super(haxeNamedComponent.getNode());
    mHaxeNamedComponent = haxeNamedComponent;
  }

  @NotNull
  private HaxeNamedComponent getDelegate() {
    return mHaxeNamedComponent;
  }

  @Nullable
  public HaxeComponentName getComponentName() {
    return getDelegate().getComponentName();
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    PsiComment psiComment = HaxeResolveUtil.findDocumentation(getDelegate());
    return ((psiComment != null)? new HaxePsiDocComment(getDelegate(), psiComment) : null);
  }

  @NotNull
  @Override
  public HaxePsiModifierList getModifierList() {
    HaxePsiModifierList haxePsiModifierList = new HaxePsiModifierList(getDelegate());

    if (getDelegate().isStatic()) {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.STATIC, true);
    }

    if (getDelegate().isPublic()) {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.PUBLIC, true);
    }
    else {
      haxePsiModifierList.setModifierProperty(HaxePsiModifier.PRIVATE, true);
    }

    // XXX: make changes to bnf, and add code to detect any other missing annotations/modifiers
    // that can be applied to an identifier declaration... set appropriate elements as above.
    // E.g. see AbstractHaxeClassPsi

    return haxePsiModifierList;
  }

  @Override
  public boolean hasModifierProperty(@HaxePsiModifier.ModifierConstant @NonNls @NotNull String name) {
    return getModifierList().hasModifierProperty(name);
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Nullable
  @Override
  public PsiClass getContainingClass() {
    return PsiTreeUtil.getParentOfType(this, HaxeClass.class, true);
  }
}
