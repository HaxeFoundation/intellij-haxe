/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassResolveResult implements Cloneable {
  public static final HaxeClassResolveResult EMPTY = new HaxeClassResolveResult(null);
  @Nullable
  private final HaxeClass haxeClass;
  private final HaxeGenericSpecialization specialization;
  private final List<HaxeClassResolveResult> functionTypes = new ArrayList<HaxeClassResolveResult>();

  private HaxeClassResolveResult(@Nullable HaxeClass aClass) {
    this(aClass, new HaxeGenericSpecialization());
  }

  private HaxeClassResolveResult(@Nullable HaxeClass aClass, HaxeGenericSpecialization specialization) {
    haxeClass = aClass;
    this.specialization = specialization;
  }

  @Override
  protected HaxeClassResolveResult clone() {
    return new HaxeClassResolveResult(haxeClass, specialization.clone());
  }

  @NotNull
  public static HaxeClassResolveResult create(@Nullable HaxeClass aClass) {
    return create(aClass, new HaxeGenericSpecialization());
  }

  @NotNull
  public static HaxeClassResolveResult create(@Nullable HaxeClass aClass, HaxeGenericSpecialization specialization) {
    if (aClass == null) {
      return new HaxeClassResolveResult(null);
    }
    HaxeClassResolveResult resolveResult = HaxeClassResolveCache.getInstance(aClass.getProject()).get(aClass);

    if (resolveResult == null) {
      resolveResult = new HaxeClassResolveResult(aClass);
      HaxeClassResolveCache.getInstance(aClass.getProject()).put(aClass, resolveResult);

      HaxeGenericParam genericParam = aClass.getGenericParam();
      List<HaxeGenericListPart> genericListPartList = genericParam != null ?
                                                      genericParam.getGenericListPartList() :
                                                      Collections.<HaxeGenericListPart>emptyList();
      for (HaxeGenericListPart genericListPart : genericListPartList) {
        HaxeComponentName componentName = genericListPart.getComponentName();
        HaxeTypeListPart typeListPart = genericListPart.getTypeListPart();
        HaxeTypeOrAnonymous typeOrAnonymous = typeListPart != null ? typeListPart.getTypeOrAnonymous() : null;
        HaxeType specializedType = typeOrAnonymous != null ? typeOrAnonymous.getType() : null;
        if (specializedType != null) {
          resolveResult.specialization.put(aClass,
                                           componentName.getName(),
                                           HaxeResolveUtil.getHaxeClassResolveResult(specializedType, specialization));
        }
      }

      for (HaxeType haxeType : aClass.getExtendsList()) {
        final HaxeClassResolveResult result = create(HaxeResolveUtil.tryResolveClassByQName(haxeType));
        result.specializeByParameters(haxeType.getTypeParam());
        resolveResult.merge(result.getSpecialization());
      }
      for (HaxeType haxeType : aClass.getImplementsList()) {
        final HaxeClassResolveResult result = create(HaxeResolveUtil.tryResolveClassByQName(haxeType));
        result.specializeByParameters(haxeType.getTypeParam());
        resolveResult.merge(result.getSpecialization());
      }
    }

    final HaxeClassResolveResult clone = resolveResult.clone();
    clone.softMerge(specialization);
    return clone;
  }

  public List<HaxeClassResolveResult> getFunctionTypes() {
    return functionTypes;
  }

  private void merge(HaxeGenericSpecialization otherSpecializations) {
    for (String key : otherSpecializations.map.keySet()) {
      specialization.map.put(key, otherSpecializations.map.get(key));
    }
  }

  private void softMerge(HaxeGenericSpecialization otherSpecializations) {
    for (String key : otherSpecializations.map.keySet()) {
      if (!specialization.map.containsKey(key)) {
        specialization.map.put(key, otherSpecializations.map.get(key));
      }
    }
  }

  @Nullable
  public HaxeClass getHaxeClass() {
    return haxeClass;
  }

  public HaxeGenericSpecialization getSpecialization() {
    return specialization;
  }

  public void specialize(@Nullable PsiElement element) {
    if (element == null || haxeClass == null || !haxeClass.isGeneric()) {
      return;
    }
    if (element instanceof HaxeNewExpression) {
      specializeByParameters(((HaxeNewExpression)element).getType().getTypeParam());
    }
  }

  public void specializeByParameters(@Nullable HaxeTypeParam param) {
    if (param == null || haxeClass == null || !haxeClass.isGeneric()) {
      return;
    }
    final HaxeGenericParam genericParam = haxeClass.getGenericParam();
    assert genericParam != null;
    final HaxeTypeList typeList = param.getTypeList();
    int size = Math.min(genericParam.getGenericListPartList().size(), typeList.getTypeListPartList().size());
    for (int i = 0; i < size; i++) {
      HaxeGenericListPart haxeGenericListPart = genericParam.getGenericListPartList().get(i);
      final HaxeTypeOrAnonymous typeOrAnonymous = typeList.getTypeListPartList().get(i).getTypeOrAnonymous();
      final HaxeType specializedType = typeOrAnonymous == null ? null : typeOrAnonymous.getType();
      if (haxeGenericListPart.getText() == null || specializedType == null) continue;
      specialization
        .put(haxeClass, haxeGenericListPart.getText(), HaxeResolveUtil.getHaxeClassResolveResult(specializedType, specialization));
    }
  }

  public boolean isFunctionType() {
    return !functionTypes.isEmpty();
  }
}
