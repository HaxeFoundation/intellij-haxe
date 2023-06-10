package com.intellij.plugins.haxe.model;

import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;

public class HaxeExprFileModel extends HaxeFileModel {

  private HaxeExprFileModel(@NotNull HaxeFile file) {
    super(file);
  }

  @Override
  protected boolean isReferencingCurrentFile(FullyQualifiedInfo info) {
    return (info.packagePath == null || info.packagePath.isEmpty()) && (info.fileName == null || info.fileName.isEmpty());
  }

  public static HaxeExprFileModel fromFile(@NotNull HaxeFile file) {
    return CachedValuesManager.getProjectPsiDependentCache(file, HaxeExprFileModel::cacheValueProvider).getValue();
  }

  private static CachedValueProvider.Result<HaxeExprFileModel> cacheValueProvider(@NotNull HaxeFile file) {
    return CachedValueProvider.Result.create(new HaxeExprFileModel(file),
                                             ModificationTracker.EVER_CHANGED,
                                             ProjectRootModificationTracker.getInstance(file.getProject()));
  }
}
