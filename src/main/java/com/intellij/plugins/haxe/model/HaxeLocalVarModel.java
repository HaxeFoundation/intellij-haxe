/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2020 Eric Bishton
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.FINAL;

public class HaxeLocalVarModel extends HaxeMemberModel {

  private HaxeLocalVarDeclaration element;

  public HaxeLocalVarModel(HaxeLocalVarDeclaration element) {
    super(element);
    this.element = element;
  }

  @Override
  public ResultHolder getResultType() {
    final HaxeTypeTag typeTag = element.getTypeTag();
    final HaxeTypeOrAnonymous type = typeTag != null ? typeTag.getTypeOrAnonymous() : null;
    return type != null ? HaxeTypeResolver.getTypeFromTypeOrAnonymous(type) : null;

  }
  @Override
  public String getPresentableText(HaxeMethodContext context) {
    final ResultHolder type = getResultType();
    return type == null ? this.getName() : this.getName() + ":" + type;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return null;
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }

  public boolean hasInitializer() {
    return getInitializerPsi() != null;
  }

  @Nullable
  public HaxeVarInit getInitializerPsi() {
    final PsiElement basePsi = getBasePsi();
    return basePsi instanceof HaxeLocalVarDeclaration ? ((HaxeLocalVarDeclaration)basePsi).getVarInit() : null;
  }

  public boolean hasTypeTag() {
    return getTypeTagPsi() != null;
  }

  public HaxeTypeTag getTypeTagPsi() {
    final PsiElement basePsi = getBasePsi();
    if (basePsi instanceof HaxeLocalVarDeclaration) {
      return ((HaxeLocalVarDeclaration)basePsi).getTypeTag();
    }
    return null;
  }

  private HaxeMutabilityModifier getMutabilityModifier() {
    HaxeLocalVarDeclarationList declParent = UsefulPsiTreeUtil.getParentOfType(element, HaxeLocalVarDeclarationList.class);
    return null != declParent ? declParent.getMutabilityModifier() : null;
  }

  @Override
  public boolean isFinal() {
    return FINAL.equals(getMutabilityModifier().getText()) || super.isFinal();
  }
}
