package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;

public class HaxeExprFileModel extends HaxeFileModel {

  private HaxeExprFileModel(@NotNull HaxeFile file) {
    super(file);
  }

  @Override
  protected boolean isReferencingCurrentFile(FullyQualifiedInfo info) {
    return (info.packageName == null || info.packageName.isEmpty()) && (info.moduleName == null || info.moduleName.isEmpty());
  }

  public static HaxeExprFileModel fromFile(@NotNull HaxeFile file) {
    return CachedValuesManager.getProjectPsiDependentCache(file, HaxeExprFileModel::cacheValueProvider);
  }

  private static HaxeExprFileModel cacheValueProvider(@NotNull HaxeFile file) {
    return new HaxeExprFileModel(file);
  }
}
