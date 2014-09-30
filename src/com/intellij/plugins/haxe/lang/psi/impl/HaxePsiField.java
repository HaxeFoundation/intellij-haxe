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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
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

  public HaxePsiField(@NotNull HaxeNamedComponent inHaxeNamedComponent) {
    super(inHaxeNamedComponent.getNode());
    mHaxeNamedComponent = inHaxeNamedComponent;
  }

  @Nullable
  public HaxeComponentName getComponentName() {
    return mHaxeNamedComponent.getComponentName();
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    PsiComment psiComment = HaxeResolveUtil.findDocumentation(mHaxeNamedComponent);
    return ((psiComment != null)? new HaxePsiDocComment(mHaxeNamedComponent, psiComment) : null);
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

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    //
    // TODO: [TiVo]: Verify + Fix, based on learning from fixing hasModifierProperty/getModifierList in AbstractHaxePsiClass
    //
    if (PsiModifier.PUBLIC.equals(name)) {
      return mHaxeNamedComponent.isPublic();
    }
    else if (PsiModifier.PRIVATE.equals(name)) {
      return (! mHaxeNamedComponent.isPublic());
    }
    else if (PsiModifier.STATIC.equals(name)) {
      return mHaxeNamedComponent.isStatic();
    }
    return getModifierList().hasModifierProperty(name);
  }
}
