package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeLocalValueElementModel extends HaxeBaseMemberModel {

  public HaxeLocalValueElementModel(PsiElement basePsi) {
    super(basePsi);
  }

  public ResultHolder getVariableType() {
    return HaxeExpressionEvaluator.evaluate(basePsi).result;
  }

  @Override
  public @Nullable HaxeClassModel getDeclaringClass() {
    return null;
  }

  @Override
  public @Nullable HaxeModuleModel getDeclaringModule() {
    return null;
  }

  @Override
  public @Nullable FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }

  @Override
  public @Nullable HaxeExposableModel getExhibitor() {
    return null;
  }
}
