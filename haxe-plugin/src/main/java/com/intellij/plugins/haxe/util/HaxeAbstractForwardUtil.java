/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAbstractClassModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Extensions for resolving and analyzing @:forward abstract meta
 */
public class HaxeAbstractForwardUtil {

  public static boolean isElementInForwardMeta(@Nullable PsiElement element) {
    if (element != null) {
      if (element instanceof HaxeCustomMeta) {
        return element.getText().contains("@:forward(");
      }
      return isElementInForwardMeta(element.getParent());
    }
    return false;
  }

  @Nullable
  public static List<HaxeNamedComponent> findAbstractForwardingNamedSubComponents(@Nullable HaxeClass clazz) {
    final List<String> forwardingFieldsNames = getAbstractForwardingFieldsNames(clazz);
    if (forwardingFieldsNames != null && clazz instanceof HaxeAbstractClassDeclaration) {
      final HaxeAbstractClassModel abstractClassModel = (HaxeAbstractClassModel)clazz.getModel();
      final HaxeClass underlyingClass = abstractClassModel.getUnderlyingClass();
      if (underlyingClass != null) {
        if (forwardingFieldsNames.isEmpty()) {
          return HaxeResolveUtil.findNamedSubComponents(underlyingClass);
        }
        List<HaxeNamedComponent> haxeNamedComponentList = new ArrayList<>();
        for (String fieldName : forwardingFieldsNames) {
          HaxeNamedComponent component = HaxeResolveUtil.findNamedSubComponent(underlyingClass, fieldName);
          if (component != null) {
            haxeNamedComponentList.add(component);
          }
        }
        return haxeNamedComponentList;
      }
    }
    return null;
  }

  @Nullable
  public static List<String> getAbstractForwardingFieldsNames(@Nullable HaxeClass clazz) {
    if (clazz == null) return null;
    List<String> forwardingFields = new LinkedList<>();
    HaxeMacroClass meta = clazz.getMeta("@:forward");
    if (meta != null) {
      HaxeCustomMeta customMeta = meta.getCustomMeta();
      if (customMeta != null) {
        HaxeExpressionList expressions = customMeta.getExpressionList();
        if (expressions != null) {
          for (HaxeExpression expr : expressions.getExpressionList()) {
            final String name = expr.getText();
            if (name != null && !name.isEmpty() && !forwardingFields.contains(name)) {
              forwardingFields.add(name);
            }
          }
        }
        return forwardingFields;
      }
    }
    return null;
  }
}
