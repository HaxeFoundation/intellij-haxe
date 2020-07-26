/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2019-2020 Eric Bishton
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeClassReference;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Extensions for resolving and analyzing Haxe @:enum abstract type
 */
public class HaxeAbstractEnumUtil {

  @Contract("null -> false")
  public static boolean isAbstractEnum(@Nullable PsiClass clazz) {
    return clazz instanceof HaxeClass && ((HaxeClass)clazz).isAbstract() && clazz.isEnum();
  }

  /**
   * If this element suitable for processing by `@:enum abstract` logic
   * IMPORTANT: This method doesn't check if this field inside `@:enum abstract`!
   */
  @Contract("null -> false")
  public static boolean couldBeAbstractEnumField(@Nullable PsiElement element) {
    if (element != null && element instanceof HaxeFieldDeclaration) {
      final HaxeFieldDeclaration decl = (HaxeFieldDeclaration)element;
      if (decl.getPropertyDeclaration() == null && !decl.isStatic()) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static HaxeClassResolveResult resolveFieldType(@Nullable PsiElement element, HaxeGenericSpecialization specialization) {
    final HaxeGenericResolver resolver = null == specialization ? null : specialization.toGenericResolver(element);
    final SpecificHaxeClassReference cls = getFieldClass(element, resolver);
    return cls != null ? cls.asResolveResult() : null;
  }

  @Nullable
  public static ResultHolder getFieldType(@Nullable PsiElement element, @Nullable HaxeGenericResolver resolver) {
    final SpecificHaxeClassReference cls = getFieldClass(element, resolver); // returns null if element is not an abstract field.
    if (cls != null && element != null) {
      ResultHolder result = new ResultHolder(cls);

      if (element instanceof HaxeFieldDeclaration) {
        final HaxeVarInit init = ((HaxeFieldDeclaration)element).getVarInit();
        if (init != null && init.getExpression() != null) {
          result = result.withConstantValue(init.getExpression().getText());
        }
      }
      return result;
    }
    return null;
  }

  @Nullable
  @Contract("null,null -> null")
  public static ResultHolder getStaticMemberExpression(@Nullable PsiElement expression, HaxeGenericResolver resolver) {
    if (expression != null) {
      final PsiElement containerElement = expression.getFirstChild();
      final PsiElement memberElement = expression.getLastChild();

      if (containerElement instanceof HaxeReference && memberElement instanceof HaxeIdentifier) {
        final HaxeClass leftClass = ((HaxeReference)containerElement).resolveHaxeClass().getHaxeClass();
        if (isAbstractEnum(leftClass)) {
          final HaxeNamedComponent enumField = leftClass.findHaxeFieldByName(memberElement.getText(), resolver);
          if (enumField != null) {
            ResultHolder result = getFieldType(enumField, resolver);
            if (result != null) {
              return result;
            }
          }
        }
      }
    }
    return null;
  }

  /*** HELPERS ***/

  @Nullable
  private static SpecificHaxeClassReference getFieldClass(@Nullable PsiElement element, @Nullable HaxeGenericResolver resolver) {
    final HaxeFieldDeclaration varDecl = (element instanceof HaxeFieldDeclaration) ?
                                       (HaxeFieldDeclaration)element : null;
    if (couldBeAbstractEnumField(varDecl)) {
      final HaxeAbstractClassDeclaration abstractEnumClass =
        PsiTreeUtil.getParentOfType(varDecl, HaxeAbstractClassDeclaration.class);
      SpecificHaxeClassReference specificRef;
      if (isAbstractEnum(abstractEnumClass)) {
        if (varDecl.getTypeTag() == null) {
          HaxeClassReference enumReference = new HaxeClassReference(abstractEnumClass.getModel(), element);
          ResultHolder[] specifics = resolver != null ? resolver.getSpecificsFor(enumReference) : ResultHolder.EMPTY;
          specificRef = SpecificHaxeClassReference.withGenerics(enumReference, specifics);
          return SpecificHaxeClassReference.propagateGenericsToType(specificRef, resolver);
        }
        HaxeGenericSpecialization specialization = resolver != null ? resolver.getSpecialization(element) : HaxeGenericSpecialization.EMPTY;
        HaxeClassResolveResult result = HaxeResolveUtil.tryResolveClassByTypeTag(varDecl, specialization);
        return result.getSpecificClassReference(element, resolver);  // Propagates generics internally.
      }
    }
    return null;
  }
}
