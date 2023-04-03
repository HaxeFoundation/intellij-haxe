/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.plugins.haxe.lang.psi.HaxeAbstractClassDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.HaxeAbstractClassModel;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataContent;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extensions for resolving and analyzing @:forward abstract meta
 */
public class HaxeAbstractForwardUtil {

  public static boolean isElementInForwardMeta(@Nullable PsiElement element) {
    HaxeMeta meta = HaxeMetadataUtils.getEnclosingMeta(element);
    return null != meta && meta.isCompileTimeMeta() && meta.isType(HaxeMeta.FORWARD);
  }

  @Nullable
  public static List<HaxeNamedComponent> findAbstractForwardingNamedSubComponents(@Nullable HaxeClass clazz, @Nullable HaxeGenericResolver resolver) {
    final List<String> forwardingFieldsNames = getAbstractForwardingFieldsNames(clazz);
    if (forwardingFieldsNames != null && clazz instanceof HaxeAbstractClassDeclaration) {
      final HaxeAbstractClassModel abstractClassModel = (HaxeAbstractClassModel)clazz.getModel();
      final HaxeClass underlyingClass = abstractClassModel.getUnderlyingClass(resolver);
      if (underlyingClass != null) {
        if (forwardingFieldsNames.isEmpty()) {
          HaxeGenericResolver forwardResolver = resolver != null ? resolver : new HaxeGenericResolver();
          forwardResolver = HaxeGenericResolverUtil.getResolverSkipAbstractNullScope(abstractClassModel, forwardResolver);
          return HaxeResolveUtil.findNamedSubComponents(forwardResolver, underlyingClass);
        }
        List<HaxeNamedComponent> haxeNamedComponentList = new ArrayList<>();
        for (String fieldName : forwardingFieldsNames) {
          HaxeNamedComponent component = HaxeResolveUtil.findNamedSubComponent(underlyingClass, fieldName, resolver);
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
    HaxeMetadataList forwardMetas = clazz.getCompileTimeMeta(HaxeMeta.FORWARD);
    if (forwardMetas.isEmpty()) {
      return null;
    }

    // We need to return an empty list if the meta exists, but has no names.
    // This because an empty list is interpreted as "all" of the names. (In Haxe and in plugin code.)
    List<String> forwardingFields = new ArrayList<>();
    for (HaxeMeta meta: forwardMetas) {
      HaxeMetadataContent content = meta.getContent();
      if (null != content) {
        forwardingFields.addAll(parseForwardingFieldsNames(content));
      }
    }
    return forwardingFields;
  }

  @NotNull
  private static Set<String> parseForwardingFieldsNames(@NotNull HaxeMetadataContent content) {
    Set<String> names = new HashSet<String>();
    List<HaxeExpression> expressions = HaxeMetadataUtils.getCompileTimeExpressions(content);
    for (HaxeExpression expression : expressions) {
      addIfUnique(names, expression.getText());
    }
    return names;
  }

  private static void addIfUnique(Set<String> set, String name) {
    if (null != name && !name.isEmpty() && !set.contains(name)) {
      set.add(name);
    }
  }


}
