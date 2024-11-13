package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeTypeParameterDeclarationPsiMixinImpl extends AbstractHaxePsiClass implements HaxeTypeParameterDeclaration {
  private HaxeTypeParameterScope scope;

  public HaxeTypeParameterDeclarationPsiMixinImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable HaxeGenericParam getGenericParam() {
    return null;
  }

  @Override
  public boolean isTypeParameter() {
    return true;
  }

  @Override
  public HaxeNamedComponent getOwner() {
    HaxeMethod methodDeclaration = PsiTreeUtil.getParentOfType(this, HaxeMethodDeclaration.class);
    if (methodDeclaration != null) return methodDeclaration;
    return PsiTreeUtil.getParentOfType(this, HaxeClass.class);
  }


  public @NotNull HaxeGenericParamModel getModel() {
    return (HaxeGenericParamModel)super.getModel();
  }

  @Override
  public HaxeTypeParameterScope getTypeParameterScope() {
    if(scope != null) return scope;
    if(getOwner() instanceof HaxeMethod) {
      scope =  HaxeTypeParameterScope.METHOD;
    }else {
      scope = HaxeTypeParameterScope.CLASS;
    }
    return scope;
  }

  @Override
  public String getQualifiedName() {
    HaxeNamedComponent owner = getOwner();
    if (owner instanceof HaxeClass haxeClass) {
      return haxeClass.getQualifiedName() + ":" + getName();
    }
    else if (owner instanceof HaxeMethod method) {
      if(method.getContainingClass() instanceof  HaxeClass haxeClass) {
        return haxeClass.getQualifiedName() + "#" + method.getName() + ":" + getName();
      }
    }
    return HaxeComponentType.getPresentableName(this);
  }
}
