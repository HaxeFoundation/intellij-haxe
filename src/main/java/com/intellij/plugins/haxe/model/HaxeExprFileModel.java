package com.intellij.plugins.haxe.model;

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import org.jetbrains.annotations.NotNull;

public class HaxeExprFileModel extends HaxeFileModel {
  private static final Key<HaxeExprFileModel> HAXE_EXPR_FILE_MODEL_KEY = new Key<>("HAXE_STD_FILE_MODEL");

  private HaxeExprFileModel(@NotNull HaxeFile file) {
    super(file);
  }

  @Override
  protected boolean isReferencingCurrentFile(FullyQualifiedInfo info) {
    return (info.packagePath == null || info.packagePath.isEmpty()) && (info.fileName == null || info.fileName.isEmpty());
  }

  public static HaxeExprFileModel fromFile(@NotNull HaxeFile file) {
    HaxeExprFileModel model = file.getUserData(HAXE_EXPR_FILE_MODEL_KEY);
    if (model == null) {
      model = new HaxeExprFileModel(file);
      file.putUserData(HAXE_EXPR_FILE_MODEL_KEY, model);
    }
    return model;
  }
}
