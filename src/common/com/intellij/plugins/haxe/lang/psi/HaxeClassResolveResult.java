/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassResolveResult implements Cloneable {

  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();

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

  private static int debugNestCountForCreate = 0;
  @NotNull
  public static HaxeClassResolveResult create(@Nullable HaxeClass aClass, HaxeGenericSpecialization specialization) {
    if (aClass == null) {
      return new HaxeClassResolveResult(null);
    }
    debugNestCountForCreate += 1;
    LOG.debug(debugNestCountForCreate + "Resolving class " + aClass.getName() + " using specialization " + (specialization == null ? "<none>" : specialization.debugDump("  ")));
    HaxeClassResolveResult resolveResult = HaxeClassResolveCache.getInstance(aClass.getProject()).get(aClass);

    if (resolveResult == null) {
      resolveResult = new HaxeClassResolveResult(aClass);
      HaxeClassResolveCache.getInstance(aClass.getProject()).put(aClass, resolveResult);

      final HaxeGenericParam genericParam = aClass.getGenericParam();
      List<HaxeGenericListPart> genericListPartList = genericParam != null ?
                                                      genericParam.getGenericListPartList() :
                                                      Collections.<HaxeGenericListPart>emptyList();
      for (HaxeGenericListPart genericListPart : genericListPartList) {
        final HaxeComponentName componentName = genericListPart.getComponentName();
        final HaxeTypeListPart typeListPart = genericListPart.getTypeListPart();
        final List<HaxeTypeOrAnonymous> typeOrAnonymousList = ((typeListPart != null) ? typeListPart.getTypeOrAnonymousList() : null);
        final HaxeTypeOrAnonymous typeOrAnonymous = ((typeOrAnonymousList != null) ? typeOrAnonymousList.get(0) : null);
        final HaxeType specializedType = ((typeOrAnonymous != null) ? typeOrAnonymous.getType() : null);
        if (specializedType != null) {
          HaxeClassResolveResult specializedTypeResult = HaxeResolveUtil.getHaxeClassResolveResult(specializedType, specialization);
          LOG.debug(debugNestCountForCreate + "  Adding specialization for " + aClass.getName() + "<" + componentName.getName() + "> -> " + specializedTypeResult.debugDump("    "));
          resolveResult.specialization.put(aClass,
                                           componentName.getName(),
                                           specializedTypeResult);
        } else {
          LOG.debug(debugNestCountForCreate + "  Not adding specialization for " + aClass.getName() + "<" + componentName.getName() + ">. No specialized type found.");
        }
      }

      for (HaxeType haxeType : aClass.getHaxeExtendsList()) {
        final HaxeClassResolveResult result = create(HaxeResolveUtil.tryResolveClassByQName(haxeType));
        result.specializeByParameters(haxeType.getTypeParam());
        LOG.debug(debugNestCountForCreate + "  Adding (extends) specialization for " + aClass.getName() + "<" + haxeType.getName() + "> -> " + result.getSpecialization().debugDump("    "));
        resolveResult.merge(result.getSpecialization());
      }
      for (HaxeType haxeType : aClass.getHaxeImplementsList()) {
        final HaxeClassResolveResult result = create(HaxeResolveUtil.tryResolveClassByQName(haxeType));
        result.specializeByParameters(haxeType.getTypeParam());
        LOG.debug(debugNestCountForCreate + "  Adding (implements) specialization for " + aClass.getName() + "<" + haxeType.getName() + "> -> " + result.getSpecialization().debugDump("    "));
        resolveResult.merge(result.getSpecialization());
      }
    }

    final HaxeClassResolveResult clone = resolveResult.clone();
    clone.softMerge(specialization);
    debugNestCountForCreate -= 1;
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
      final HaxeGenericListPart genericListPart = genericParam.getGenericListPartList().get(i);
      final HaxeTypeListPart typeListPart = typeList.getTypeListPartList().get(i);
      final List<HaxeTypeOrAnonymous> typeOrAnonymousList = ((typeListPart != null) ? typeListPart.getTypeOrAnonymousList() : null);
      final HaxeTypeOrAnonymous typeOrAnonymous = (((typeOrAnonymousList != null) && (typeOrAnonymousList.size() > 0)) ? typeOrAnonymousList.get(0) : null);
      final HaxeType specializedType = ((typeOrAnonymous != null) ? typeOrAnonymous.getType() : null);
      if (genericListPart.getText() == null || specializedType == null) continue;
      final HaxeClassResolveResult specializedTypeResult = HaxeResolveUtil.getHaxeClassResolveResult(specializedType, specialization);
      specialization.put(haxeClass, genericListPart.getText(), specializedTypeResult);
    }
    LOG.debug(specialization.debugDump());
  }

  public boolean isFunctionType() {
    return !functionTypes.isEmpty();
  }


  public String debugDump() {
    return debugDump(null);
  }

  public String debugDump( String linePrefix ) {
    StringBuilder builder = new StringBuilder();

    if (linePrefix == null) {
      linePrefix="";
    }
    builder.append(linePrefix);
    builder.append(null == haxeClass ? "<null haxeClass>" : haxeClass.getName());
    builder.append(":\n");
    String prefix = linePrefix + "  ";
    builder.append(null == specialization ? "<null specialization>" : specialization.debugDump(prefix));
    for(HaxeClassResolveResult result : functionTypes) {
      result.debugDump(prefix + "  ");
    }
    return builder.toString();
  }


  public JavaResolveResult toJavaResolveResult() {
    return new JavaResult(this);
  }

  private class JavaResult implements JavaResolveResult {
    private HaxeClassResolveResult originalResult = null;
    public JavaResult(HaxeClassResolveResult result) { originalResult = result; }
    @Override public PsiElement getElement() { return (originalResult != null ? originalResult.getHaxeClass() : null); }
    @NotNull
    @Override public PsiSubstitutor getSubstitutor() { return PsiSubstitutor.EMPTY; }
    @Override public boolean isValidResult() { return null != this.getElement(); }
    @Override public boolean isAccessible() { return true; }
    @Override public boolean isStaticsScopeCorrect() { return true; } // TODO: How to check scope?
    @Override public PsiElement getCurrentFileResolveScope() { return (this.getElement() != null ? this.getElement().getOriginalElement() : null); } // TODO: Verify
    @Override public boolean isPackagePrefixPackageReference() { return false; }  // TODO: No idea what to do with this.
  }

}
