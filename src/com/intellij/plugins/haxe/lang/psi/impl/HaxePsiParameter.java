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

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Srikanth.Ganapavarapu
 */
public class HaxePsiParameter extends HaxeParameterImpl implements HaxeParameter, PsiParameter {

  public HaxePsiParameter(ASTNode node) {
    super(node);
  }

  public HaxePsiParameter(PsiParameter parameter) {
    super(parameter.getNode());
  }

  public HaxePsiParameter(HaxeParameter parameter) {
    super(parameter.getNode());
  }

  @NotNull
  @Override
  public PsiElement getDeclarationScope() {
    // TODO: [TiVo]: implement?
    return getParent();
  }

  @Override
  public boolean isVarArgs() {
    // TODO: [TiVo]: implement?
    return false;
  }

  @Nullable
  @Override
  public PsiTypeElement getTypeElement() {
    // TODO: [TiVo]: implement?
    return null;
  }

  @NotNull
  @Override
  public PsiType getType() {
    // TODO: [TiVo]: implement?
    return null;
  }

  @Nullable
  @Override
  public PsiExpression getInitializer() {
    // TODO: [TiVo]: implement?
    return null;
  }

  @Override
  public boolean hasInitializer() {
    // TODO: [TiVo]: implement?
    return false;
  }

  @Override
  public void normalizeDeclaration() throws IncorrectOperationException {
    // TODO: [TiVo]: implement?
  }

  @Nullable
  @Override
  public Object computeConstantValue() {
    // TODO: [TiVo]: implement?
    return null;
  }

  @Nullable
  @Override
  public PsiIdentifier getNameIdentifier() {
    // TODO: [TiVo]: implement?
    return null;
  }

  @Nullable
  @Override
  public PsiModifierList getModifierList() {
    // TODO: [TiVo]: implement?
    return null;
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    // TODO: [TiVo]: implement?
    return false;
  }
}
