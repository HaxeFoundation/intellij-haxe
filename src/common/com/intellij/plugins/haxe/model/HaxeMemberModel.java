/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.*;

abstract public class HaxeMemberModel extends HaxeBaseMemberModel {

  public HaxeMemberModel(PsiMember basePsi) {
    super(basePsi);
  }

  public static HaxeMemberModel fromPsi(PsiElement psiElement) {
    return ObjectUtils.tryCast(HaxeBaseMemberModel.fromPsi(psiElement), HaxeMemberModel.class);
  }

  public PsiMember getMemberPsi() {
    return (PsiMember)basePsi;
  }

  public boolean isPublic() {
    HaxeClassModel declaringClass = getDeclaringClass();

    return hasModifier(PUBLIC)
           // Fields and methods of externs and interfaces are public by default, private modifier for them should be defined explicitly
           || ((declaringClass.isInterface() || declaringClass.isExtern()) && !hasModifier(PRIVATE))
           || isOverriddenPublicMethod()
           || getDeclaringClass().hasMeta("@:publicFields");
  }

  public boolean isFinal() {
    return hasModifier(FINAL);
  }

  private boolean isOverriddenPublicMethod() {
    if (hasModifier(OVERRIDE)) {
      final HaxeMemberModel parentMember = getParentMember();
      return parentMember != null && parentMember.isPublic();
    }

    return false;
  }

  @Override
  public HaxeClassModel getDeclaringClass() {
    HaxeClassModel model = getBasePsi().getUserData(DECLARING_CLASS_MODEL_KEY);
    if (model == null) {
      PsiClass containingClass = getMemberPsi().getContainingClass();
      if (containingClass instanceof HaxeClass) {
        model = ((HaxeClass)containingClass).getModel();
        getBasePsi().putUserData(DECLARING_CLASS_MODEL_KEY, model);
      }
    }

    return model;
  }

  public boolean hasModifier(@HaxePsiModifier.ModifierConstant String modifier) {
    return this.getModifiers().hasModifier(modifier);
  }

  public boolean isStatic() {
    return hasModifier(HaxePsiModifier.STATIC);
  }

  @NotNull
  public PsiElement getNameOrBasePsi() {
    PsiElement element = getNamePsi();
    if (element == null) element = getBasePsi();
    return element;
  }

  private HaxeModifiersModel _modifiers;

  @NotNull
  public HaxeModifiersModel getModifiers() {
    if (_modifiers == null) _modifiers = new HaxeModifiersModel(basePsi);
    return _modifiers;
  }

  public HaxeMemberModel getParentMember() {
    final HaxeClassModel aClass = getDeclaringClass().getParentClass();
    return (aClass != null) ? aClass.getMember(this.getName()) : null;
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    if (getDeclaringClass() != null && isStatic() && isPublic()) {
      FullyQualifiedInfo containerInfo = getDeclaringClass().getQualifiedInfo();
      if (containerInfo != null) {
        return new FullyQualifiedInfo(containerInfo.packagePath, containerInfo.fileName, containerInfo.className, getName());
      }
    }
    return null;
  }
}
