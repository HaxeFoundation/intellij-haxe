package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.hash.HashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassResolveResult {
  public static final HaxeClassResolveResult EMPTY = new HaxeClassResolveResult(null);
  @Nullable
  private final HaxeClass haxeClass;
  private final Map<String, HaxeClassResolveResult> specializations = new HashMap<String, HaxeClassResolveResult>();

  public HaxeClassResolveResult(@Nullable HaxeClass aClass) {
    haxeClass = aClass;
  }

  @Nullable
  public HaxeClass getHaxeClass() {
    return haxeClass;
  }

  public Map<String, HaxeClassResolveResult> getSpecializations() {
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
      specializations.put(haxeGenericListPart.getText(), HaxeResolveUtil.getHaxeClass(specializedType));
    }
  }
}
