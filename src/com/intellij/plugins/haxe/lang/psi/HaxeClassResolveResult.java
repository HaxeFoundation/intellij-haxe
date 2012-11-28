package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
