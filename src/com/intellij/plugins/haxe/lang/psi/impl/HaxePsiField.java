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

import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxePsiField /*extends AbstractHaxeNamedComponent */ extends PsiFieldImpl implements PsiField {

  private HaxeNamedComponent mHaxeNamedComponent;
  private AbstractHaxePsiClass mContainingClass;

  public HaxePsiField(@NotNull AbstractHaxePsiClass inContainingClass,
                       @NotNull HaxeNamedComponent inHaxeNamedComponent) {
    super(inHaxeNamedComponent.getNode());
    mContainingClass = inContainingClass;
    mHaxeNamedComponent = inHaxeNamedComponent;
  }

  @Nullable
  public HaxeComponentName getComponentName() {
    return mHaxeNamedComponent.getComponentName();
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    return new HaxePsiDocComment(mHaxeNamedComponent);
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Nullable
  @Override
  public PsiClass getContainingClass() {
    return mContainingClass;
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
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

  /*

  @NotNull
  @Override
  public PsiIdentifier getNameIdentifier() {
     // TODO: [TiVo]: Implement
    return null;
  }

  @NotNull
  @Override
  public PsiType getType() {
     // TODO: [TiVo]: Implement

    return null;
  }

  @Nullable
  @Override
  public PsiTypeElement getTypeElement() {
     // TODO: [TiVo]: Implement
    return null;
  }

  @Nullable
  @Override
  public Object computeConstantValue() {
     // TODO: [TiVo]: Implement
    return null;
  }

  @Nullable
  @Override
  public PsiExpression getInitializer() {
    // TODO: [TiVo]: is this even needed? can we get away without implementing?
    return null;
  }

  @Override
  public boolean hasInitializer() {
    // TODO: [TiVo]: is this even needed? can we get away without implementing?
    return false;
  }

  @Override
  public void setInitializer(@Nullable PsiExpression initializer) throws IncorrectOperationException {
    // TODO: [TiVo]: is this even needed? can we get away without implementing?
    // none
  }

  @Override
  public void normalizeDeclaration() throws IncorrectOperationException {
    // TODO: [TiVo]: is this even needed? can we get away without implementing?
  }

  @Nullable
  @Override
  public PsiModifierList getModifierList() {
    // TODO: [TiVo]: is this even needed? can we get away without implementing?
    return null;
  }

  */
}
