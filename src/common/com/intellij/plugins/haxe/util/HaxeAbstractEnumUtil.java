/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeClassReference;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Extensions for resolving and analyzing Haxe @:enum abstract type
 */
public class HaxeAbstractEnumUtil {

  @Contract("null -> false")
  public static boolean isAbstractEnum(@Nullable PsiClass clazz) {
    return HaxeAbstractUtil.hasMeta(clazz, "@:enum");
  }

  /**
   * If this element suitable for processing by `@:enum abstract` logic
   * IMPORTANT: This method doesn't check if this field inside `@:enum abstract`!
   */
  @Contract("null -> false")
  public static boolean couldBeAbstractEnumField(@Nullable PsiElement element) {
    if(element != null && element instanceof HaxeVarDeclarationPart) {
      final HaxeVarDeclarationPart decl = (HaxeVarDeclarationPart)element;
       if(decl.getPropertyDeclaration() == null && !decl.isStatic()) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static HaxeClassResolveResult resolveFieldType(@Nullable PsiElement element) {
    final HaxeClass cls = getFieldClass(element);
    return cls != null ? HaxeClassResolveResult.create(cls) : null;
  }

  @Nullable
  public static ResultHolder getFieldType(@Nullable PsiElement element) {
    final HaxeClass cls = getFieldClass(element);
    if(cls != null && element != null) {
      ResultHolder result = new ResultHolder(SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(cls.getModel(), element)));
      if(element instanceof HaxeVarDeclarationPart) {
        final HaxeVarInit init = ((HaxeVarDeclarationPart)element).getVarInit();
        if(init != null && init.getExpression() != null) {
          result = result.withConstantValue(init.getExpression().getText());
        }
      }
      return result;
    }
    return null;
  }

  @Nullable
  @Contract("null -> null")
  public static ResultHolder getStaticMemberExpression(@Nullable PsiElement expression) {
    if(expression != null) {
      final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(expression, HaxeReference.class);
      if (childReferences != null && childReferences.length == 2) {
        final HaxeClass leftClass = childReferences[0].resolveHaxeClass().getHaxeClass();
        if (isAbstractEnum(leftClass)) {
          final HaxeNamedComponent enumField = leftClass.findHaxeFieldByName(childReferences[1].getText());
          if(enumField != null) {
            ResultHolder result = getFieldType(enumField);
            if(result != null) {
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
  private static HaxeClass getFieldClass(@Nullable PsiElement element) {
    final HaxeVarDeclarationPart varDecl = element != null && (element instanceof HaxeVarDeclarationPart) ?
                                           (HaxeVarDeclarationPart)element : null;
    if (couldBeAbstractEnumField(varDecl)) {
      final HaxeAbstractClassDeclaration abstractEnumClass =
        PsiTreeUtil.getParentOfType(varDecl, HaxeAbstractClassDeclaration.class);
      if (isAbstractEnum(abstractEnumClass)) {
        if (varDecl.getTypeTag() == null) {
          return abstractEnumClass;
        }
      }
      HaxeClassResolveResult result = HaxeResolveUtil.tryResolveClassByTypeTag(varDecl, new HaxeGenericSpecialization());
      if(result.getHaxeClass() != null) {
        return result.getHaxeClass();
      }
    }
    return null;
  }
}
