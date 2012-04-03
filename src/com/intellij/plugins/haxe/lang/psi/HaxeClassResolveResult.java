package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassResolveResult {
  public static final HaxeClassResolveResult EMPTY = new HaxeClassResolveResult(null);
  @Nullable
  private final HaxeClass haxeClass;
  private final HaxeGenericSpecialization specializations;

  public HaxeClassResolveResult(@Nullable HaxeClass aClass) {
    this(aClass, new HaxeGenericSpecialization());
  }

  public HaxeClassResolveResult(@Nullable HaxeClass aClass, HaxeGenericSpecialization specialization) {
    haxeClass = aClass;
    this.specializations = specialization;
    if (haxeClass == null) {
      return;
    }
    for (HaxeType haxeType : haxeClass.getExtendsList()) {
      final HaxeClassResolveResult result = new HaxeClassResolveResult(HaxeResolveUtil.resolveClass(haxeType));
      result.specializeByParameters(haxeType.getTypeParam());
      merge(result.getSpecializations());
    }
    for (HaxeType haxeType : haxeClass.getImplementsList()) {
      final HaxeClassResolveResult result = new HaxeClassResolveResult(HaxeResolveUtil.resolveClass(haxeType));
      result.specializeByParameters(haxeType.getTypeParam());
      merge(result.getSpecializations());
    }
  }

  private void merge(HaxeGenericSpecialization otherSpecializations) {
    for (String key : otherSpecializations.map.keySet()){
      specializations.map.put(key, otherSpecializations.map.get(key));
    }
  }

  @Nullable
  public HaxeClass getHaxeClass() {
    return haxeClass;
  }

  public HaxeGenericSpecialization getSpecializations() {
    return specializations;
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
    for (int i = 0, size = genericParam.getGenericListPartList().size(); i < size; i++) {
      HaxeGenericListPart haxeGenericListPart = genericParam.getGenericListPartList().get(i);
      final HaxeType specializedType = typeList.getTypeListPartList().get(i).getType();
      if (haxeGenericListPart.getText() == null || specializedType == null) continue;
      specializations.put(haxeClass, haxeGenericListPart.getText(), HaxeResolveUtil.getHaxeClass(specializedType, specializations));
    }
  }
}
