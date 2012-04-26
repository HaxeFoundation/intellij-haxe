package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassResolveResult {
  public static final HaxeClassResolveResult EMPTY = new HaxeClassResolveResult(null);
  @Nullable
  private final HaxeClass haxeClass;
  private final HaxeGenericSpecialization specialization;
  private final List<HaxeClassResolveResult> functionTypes = new ArrayList<HaxeClassResolveResult>();

  public HaxeClassResolveResult(@Nullable HaxeClass aClass) {
    this(aClass, new HaxeGenericSpecialization());
  }

  public HaxeClassResolveResult(@Nullable HaxeClass aClass, HaxeGenericSpecialization specialization) {
    haxeClass = aClass;
    this.specialization = specialization;
    if (haxeClass == null) {
      return;
    }
    for (HaxeType haxeType : haxeClass.getExtendsList()) {
      final HaxeClassResolveResult result = new HaxeClassResolveResult(HaxeResolveUtil.tryResolveClassByQName(haxeType));
      result.specializeByParameters(haxeType.getTypeParam());
      merge(result.getSpecialization());
    }
    for (HaxeType haxeType : haxeClass.getImplementsList()) {
      final HaxeClassResolveResult result = new HaxeClassResolveResult(HaxeResolveUtil.tryResolveClassByQName(haxeType));
      result.specializeByParameters(haxeType.getTypeParam());
      merge(result.getSpecialization());
    }
  }

  public List<HaxeClassResolveResult> getFunctionTypes() {
    return functionTypes;
  }

  private void merge(HaxeGenericSpecialization otherSpecializations) {
    for (String key : otherSpecializations.map.keySet()) {
      specialization.map.put(key, otherSpecializations.map.get(key));
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
    for (int i = 0, size = genericParam.getGenericListPartList().size(); i < size; i++) {
      HaxeGenericListPart haxeGenericListPart = genericParam.getGenericListPartList().get(i);
      final HaxeType specializedType = typeList.getTypeListPartList().get(i).getTypeOrAnonymous().getType();
      if (haxeGenericListPart.getText() == null || specializedType == null) continue;
      specialization.put(haxeClass, haxeGenericListPart.getText(), HaxeResolveUtil.getHaxeClassResolveResult(specializedType,
                                                                                                             specialization));
    }
  }

  public boolean isFunctionType() {
    return !functionTypes.isEmpty();
  }
}
