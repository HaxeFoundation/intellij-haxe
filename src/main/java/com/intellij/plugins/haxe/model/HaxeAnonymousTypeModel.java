package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousType;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousTypeBody;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeAnonymousTypeModel extends HaxeClassModel {

  public final HaxeAnonymousType anonymousType;

  public HaxeAnonymousTypeModel(@NotNull HaxeAnonymousType anonymousType) {
    super(anonymousType);
    this.anonymousType = anonymousType;
  }

  public List<ResultHolder> getCompositeTypes() {
    List<HaxeType> typeList  = CachedValuesManager.getProjectPsiDependentCache(anonymousType, HaxeAnonymousTypeModel::_getCompositeTypes);
    return typeList.stream().map(HaxeTypeResolver::getTypeFromType).toList();
  }

  private static List<HaxeType> _getCompositeTypes(HaxeAnonymousType anonymousType) {
    return anonymousType.getTypeList();
  }

  public List<HaxeAnonymousTypeBody> getAnonymousTypeBodyList() {
    return CachedValuesManager.getProjectPsiDependentCache(anonymousType, HaxeAnonymousTypeModel::_getAnonymousTypeBodyList);
  }

  private static @NotNull List<HaxeAnonymousTypeBody> _getAnonymousTypeBodyList(HaxeAnonymousType anonymousType) {
    return anonymousType.getAnonymousTypeBodyList();
  }

}
