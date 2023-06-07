package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

/**
 * in haxe Expr and ExprOf can be used without declaring imports.
 * this class and HaxeExprFileModel helps the resolver resolve these
 */

public class HaxeExprPackageModel extends HaxePackageModel {
  private static final String EXPR_TYPES = "haxe.macro/Expr";

  final protected static HashMap<String, FullyQualifiedInfo[]> implicitSubpackageTypes = new HashMap<>();

  static {
    implicitSubpackageTypes.put("Expr", new FullyQualifiedInfo[]{new FullyQualifiedInfo("haxe.macro.Expr")});
    implicitSubpackageTypes.put("ExprOf", new FullyQualifiedInfo[]{new FullyQualifiedInfo("haxe.macro.ExprOf")});

  }


  HaxeExprPackageModel(@NotNull HaxeSourceRootModel root) {
    super(root, "", null);
  }

  private HaxeFileModel getExprFileModel() {
    // TODO: This is called by the resolver a LOT.  Cache the result and create a listener to invalidate.
    final HaxeFile file = getFile(EXPR_TYPES);
    if (file != null) {
      return HaxeExprFileModel.fromFile(file);
    }
    return null;
  }

  @Nullable
  @Override
  public HaxeClassModel getClassModel(@NotNull String className) {
    HaxeClassModel result = super.getClassModel(className);

    HaxeFileModel exprModel = getExprFileModel();
    if (result == null && exprModel != null) {
      result = exprModel.getClassModel(className);
    }

    return result;
  }

  @Override
  public HaxeModel resolve(FullyQualifiedInfo info) {
    HaxeModel result = super.resolve(info);

    HaxeFileModel exprModel = getExprFileModel();
    if (result == null && exprModel != null && (info.packagePath == null || info.packagePath.isEmpty()) && this.path.isEmpty()) {
      result = exprModel.resolve(new FullyQualifiedInfo("", null, info.fileName, info.memberName));
    }
    if (result == null) {
      resolveGlobalSubpackage(info, implicitSubpackageTypes);
    }

    return result;
  }

  @Nullable
  private HaxeModel resolveGlobalSubpackage(FullyQualifiedInfo info, HashMap<String, FullyQualifiedInfo[]> types) {
    HaxeModel result = null;
      FullyQualifiedInfo[] subpackages = types.get(info.memberName);
    if (null != subpackages) {
      for (FullyQualifiedInfo subpkg : subpackages) {
        result = super.resolve(subpkg);
        if (null != result) {
          break;
        }
      }
    }
    return result;
  }
}
