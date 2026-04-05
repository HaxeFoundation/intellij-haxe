package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeAliasModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class HaxeImportAliasPsiMixinImpl extends HaxeStatementPsiMixinImpl implements HaxeImportAlias {

  private final AtomicReference<HaxeAliasModel> aliasModel = new AtomicReference<>();

  public HaxeImportAliasPsiMixinImpl(ASTNode node) {
    super(node);
  }


  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) {
      ((HaxeVisitor)visitor).visitImportAlias(this);
    }
    else {
      super.accept(visitor);
    }
  }

  public HaxeAliasModel getModel() {
    HaxeAliasModel model = aliasModel.get();
    if (model != null && model.isValid()) {
      return model;
    }
    HaxeAliasModel newValue = new HaxeAliasModel(this);
    if (aliasModel.compareAndSet(null, newValue)) {
      return newValue;
    } else {
      return aliasModel.get();
    }
  }

  @Override
  public @NotNull HaxeIdentifier getIdentifier() {
    return findChildByClass(HaxeIdentifier.class);
  }

  @Override
  public @NotNull HaxeResolveResult resolveHaxeClass() {
    if(getParent() instanceof  HaxeImportStatement importStatement) {
      HaxeReferenceExpression expression = importStatement.getReferenceExpression();
      if (expression != null) {
        ResultHolder evaluationResult = HaxeExpressionEvaluator.evaluate(expression, null).result;
        if(!evaluationResult.isUnknown()){
          if (evaluationResult.isFunctionType()) {
            return evaluationResult.getFunctionType().asResolveResult();
          }else {
            SpecificHaxeClassReference classReference = evaluationResult.getClassType();
            HaxeClass haxeClass = classReference.getHaxeClass();
            HaxeClassModel model = haxeClass.getModel();
            @NotNull ResultHolder[] specifics = classReference.getGenericResolver().getSpecificsFor(haxeClass);
            SpecificHaxeClassReference reference = SpecificHaxeClassReference.withGenerics(model.getReference(), specifics);
            if (reference.isClass() && specifics.length == 1) {
              // extract type from Class<T>
              return specifics[0].getClassType().asResolveResult();
            }
          }
        }
      }
    }
    return HaxeResolveResult.EMPTY;
  }

}
