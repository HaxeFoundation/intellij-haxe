/*
 * Copyright 2018-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageBaseFix;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeNewExpression;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class HaxeCreateConstructorFromCallFix extends CreateFromUsageBaseFix {

  private HaxeNewExpression myNewExpression;

  public HaxeCreateConstructorFromCallFix(@NotNull HaxeNewExpression newExpression) {
    super();
    myNewExpression = newExpression;
  }

  @Override
  protected boolean isAvailableImpl(int offset) {
    HaxeClass haxeClass = getElement(myNewExpression);
    if (haxeClass == null) return false;

    PsiFile targetFile = haxeClass.getContainingFile();
    if (targetFile != null && !targetFile.getManager().isInProject(targetFile)) {
      return false;
    }

    if (shouldShowTag(offset, haxeClass, myNewExpression)) {
      setText(QuickFixBundle.message("create.constructor.from.new.text"));
      return true;
    }

    return false;
  }

  protected List<PsiClass> filterTargetClasses(PsiElement element, Project project) {
    return Collections.singletonList(getElement((HaxeNewExpression)element));
  }

  private boolean shouldShowTag(int offset, PsiElement namedElement, PsiElement element) {
    if (namedElement == null) return false;
    TextRange range = namedElement.getTextRange();
    if (range.getLength() == 0) return false;
    boolean isInNamedElement = range.contains(offset);
    return isInNamedElement || element.getTextRange().contains(offset - 1);
  }

  @Override
  protected void invokeImpl(PsiClass targetClass) {
    if (targetClass instanceof HaxeClass) {
      // TODO Need to generate proper constructor based on expressions passed to newExpression.
      // But before we should implement variables suggestions based on expressions types.
      // Check JavaCodeStyleManagerImpl as reference
      ((HaxeClass)targetClass).getModel().addMethod("new");
    }
  }

  @Override
  protected boolean isValidElement(PsiElement element) {
    HaxeNewExpression constructorCall = (HaxeNewExpression)element;
    ResultHolder typeHolder = HaxeTypeResolver.getTypeFromType(constructorCall.getType());
    if (typeHolder.getType() instanceof SpecificHaxeClassReference) {
      final HaxeClassModel clazz = ((SpecificHaxeClassReference)typeHolder.getType()).getHaxeClassModel();
      if (clazz == null) return false;
      if (clazz.isEnum() && !clazz.isAbstract()) return false;

      return clazz.getConstructor() != null;
    }

    return false;
  }

  @Nullable
  @Override
  protected PsiElement getElement() {
    if (!myNewExpression.isValid() || !myNewExpression.getManager().isInProject(myNewExpression)) return null;

    PsiElement referenceElement = getReferenceElement(myNewExpression);
    if (referenceElement == null) return null;
    if (referenceElement instanceof HaxeIdentifier) return myNewExpression;

    return null;
  }

  private static PsiElement getReferenceElement(HaxeNewExpression constructorCall) {
    HaxeClass haxeClass = getElement(constructorCall);
    if (haxeClass != null && haxeClass.getComponentName() != null) {
      return haxeClass.getComponentName().getIdentifier();
    }
    return null;
  }

  private static HaxeClass getElement(HaxeNewExpression constructorCall) {
    SpecificHaxeClassReference classReference =
      HaxeTypeResolver.getTypeFromType(constructorCall.getType()).getClassType();

    if (classReference != null) {
      return classReference.getHaxeClass();
    }

    return null;
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return QuickFixBundle.message("create.constructor.from.new.family");
  }
}
