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
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Extensions for working with abstracts
 */
public class HaxeAbstractUtil {

  public static boolean hasMeta(@Nullable PsiClass clazz, @Nullable String metaName) {
    return getMetaByName(clazz, metaName) != null;
  }

  @Nullable
  public static HaxeMacroClass getMetaByName(@Nullable PsiClass clazz, @Nullable String metaName) {
    if(clazz != null && metaName != null && clazz instanceof HaxeAbstractClassDeclaration) {
      final HaxeMacroClassList metaList = ((HaxeAbstractClassDeclaration)clazz).getMacroClassList();
      final List<HaxeMacroClass> metas = metaList != null ? metaList.getMacroClassList() : null;
      if(metas != null) {
        for (HaxeMacroClass meta : metas) {
          if (meta.getText().equals(metaName)|| meta.getText().contains(metaName + "(")) {
            return meta;
          }
        }
      }
    }
    return null;
  }

  @Nullable
  public static HaxeClass getAbstractUnderlyingClass(@Nullable PsiClass clazz) {
    if (clazz != null && clazz instanceof HaxeAbstractClassDeclaration) {
      final HaxeType underlyingType = ((HaxeAbstractClassDeclaration)clazz).getTypeOrAnonymous().getType();
      if (underlyingType != null) {
        final HaxeClassResolveResult result = underlyingType.getReferenceExpression().resolveHaxeClass();
        if (result != null) {
          return result.getHaxeClass();
        }
      }
    }
    return null;
  }

}
