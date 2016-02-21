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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeAbstractEnumUtil {

  public static boolean isAbstractEnum(@Nullable PsiClass clazz) {
    if(clazz != null && clazz instanceof HaxeAbstractClassDeclaration) {
      final HaxeMacroClassList macroListDecl = ((HaxeAbstractClassDeclaration)clazz).getMacroClassList();
      final List<HaxeMacroClass> macroList = macroListDecl != null ? macroListDecl.getMacroClassList() : null;
      if(macroList != null) {
        for (HaxeMacroClass macroClass : macroList) {
          if (macroClass.getText().equals("@:enum")) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean isAbstractEnumField(@Nullable PsiElement element) {
    if(element != null && element instanceof HaxeVarDeclarationPart) {
      final HaxeVarDeclarationPart decl = (HaxeVarDeclarationPart)element;
      final PsiClass containingClass = decl.getContainingClass();
      if(containingClass instanceof HaxeAbstractClassDeclaration &&
         decl.getPropertyDeclaration() == null && !decl.isStatic()) {
        return true;
      }
    }
    return false;
  }

  //public static boolean checkFieldType(@Nullable PsiElement element) {
  //  if(element != null && element instanceof HaxeVarDeclarationPart) {
  //    final HaxeVarDeclarationPart decl = (HaxeVarDeclarationPart)element;
  //    final HaxeTypeTag tag = decl.getTypeTag();
  //    if (tag == null) {
  //      return true;
  //    }
  //    final String className = decl.getContainingClass().getNameIdentifier().getText();
  //    return className.equals(tag.getText());
  //    //if(containingClass instanceof HaxeAbstractClassDeclaration)
  //    //final SpecificHaxeClassReference classRef = HaxeTypeResolver.getTypeFromTypeTag(tag, element).getClassType();
  //    //return classRef != null && classRef.getHaxeClass() == containingClass;
  //  }
  //  return false;
  //}

  @Nullable
  public static HaxeAbstractClassDeclaration getFieldClass(@Nullable PsiElement element) {
    if (!HaxeAbstractEnumUtil.isAbstractEnumField(element)) {
      return null;
    }
    final HaxeAbstractClassDeclaration abstractEnumClass =
      PsiTreeUtil.getParentOfType(element, HaxeAbstractClassDeclaration.class);
    return isAbstractEnum(abstractEnumClass) ? abstractEnumClass : null;
  }

  @Nullable
  public static HaxeClassResolveResult resolveFieldType(@Nullable PsiElement element) {
    final HaxeClass cls = getFieldClass(element);
    return cls != null ? HaxeClassResolveResult.create(cls) : null;
  }

  @Nullable
  public static ResultHolder getFieldType(@Nullable PsiElement element) {
    final HaxeClass cls = getFieldClass(element);
    if(cls == null || element == null) {
      return null;
    }
    return new ResultHolder(SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(cls.getModel(), element)))
        .withConstantValue(element.getText());
  }

  //public static boolean checkExpressionIsConst(@Nullable PsiElement expression) {
  //  if(expression != null) {
  //    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(expression, HaxeReference.class);
  //    if (childReferences != null && childReferences.length == 2) {
  //      final HaxeClass leftClass = childReferences[0].resolveHaxeClass().getHaxeClass();
  //      if (isAbstractEnum(leftClass)) {
  //        final HaxeNamedComponent field = leftClass.findHaxeFieldByName(childReferences[1].getText());
  //        return HaxeAbstractEnumUtil.isAbstractEnumField(field);
  //      }
  //    }
  //  }
  //  return false;
  //}

  @Nullable
  public static ResultHolder getStaticMemberExpression(@Nullable PsiElement expression) {
    if(expression != null) {
      final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(expression, HaxeReference.class);
      if (childReferences != null && childReferences.length == 2) {
        final HaxeClass leftClass = childReferences[0].resolveHaxeClass().getHaxeClass();
        if (isAbstractEnum(leftClass)) {
          return getFieldType(leftClass.findHaxeFieldByName(childReferences[1].getText()));
        }
      }
    }
    return null;
  }
}
