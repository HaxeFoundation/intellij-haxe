package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * in haxe Expr and ExprOf can be used without declaring imports.
 * this class and HaxeExprFileModel helps the resolver resolve these
 */

public class HaxeLogPackageModel extends HaxePackageModel {
  private static final String LOG_TYPES = "haxe/Log";



  HaxeLogPackageModel(@NotNull HaxeSourceRootModel root) {
    super(root, "", null);
  }

  private HaxeFileModel getLogFileModel() {
    // TODO: This is called by the resolver a LOT.  Cache the result and create a listener to invalidate.
    final HaxeFile file =  getFile(LOG_TYPES);
    if (file != null) {
      return HaxeExprFileModel.fromFile(file);
    }
    return null;
  }

  @Nullable
  @Override
  public HaxeClassModel getClassModel(@NotNull String className) {
    HaxeClassModel result = super.getClassModel(className);

    HaxeFileModel logModel = getLogFileModel();
    if (result == null && logModel != null) {
      result = logModel.getClassModel(className);
    }

    return result;
  }

  @Nullable
  public HaxeModel resolveTrace() {


    HaxeFileModel exprModel = getLogFileModel();
    if (exprModel != null) {
      HaxeClassModel classModel = exprModel.getClassModel("Log");
      if (classModel != null) {
       return classModel.getMember("trace", null);
      }
    }
    return null;
  }

}
