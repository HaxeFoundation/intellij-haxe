package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeUsingModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeImportAliasPsiMixinImpl extends HaxeStatementPsiMixinImpl implements HaxeImportAlias {
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

  @Override
  public @NotNull HaxeIdentifier getIdentifier() {
    return findChildByClass(HaxeIdentifier.class);
  }

  @Override
  public @NotNull HaxeResolveResult resolveHaxeClass() {
    if(getParent() instanceof  HaxeImportStatement importStatement) {
      HaxeReferenceExpression expression = importStatement.getReferenceExpression();
      if (expression != null) {
        return expression.resolveHaxeClass();
      }
    }
    return HaxeResolveResult.EMPTY;
  }

}
