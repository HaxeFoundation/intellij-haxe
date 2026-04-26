package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeReferenceExpressionStub;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeEnumExtractedValueElementModel;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeEnumExtractedValueMixin extends HaxeReferenceImpl implements HaxeNamedComponent, HaxeModelTarget {

  private HaxeBaseMemberModel _model = null;

  public HaxeEnumExtractedValueMixin(ASTNode node) {
    super(node);
  }

  public HaxeEnumExtractedValueMixin(HaxeReferenceExpressionStub stub, IStubElementType stubType) {
    super(stub, stubType);
  }

  @Override
  public HaxeModel getModel() {
    if (_model == null || !_model.isValid()) {
      _model = new HaxeEnumExtractedValueElementModel(this);
    }
    return _model;
  }


  @Override
  public @Nullable HaxeNamedComponent getTypeComponent() {
    return this;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isOverride() {
    return false;
  }

  @Override
  public boolean isOverload() {
    return false;
  }

  @Override
  public boolean isInline() {
    return false;
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  @Override
  public String filterName() {
    return _model.getName();
  }

  @Override
  public PsiElement getModiferPsi(IElementType tokenType) {
    return null;
  }

  @Override
  public HaxeComponentType getComponentType() {
    return HaxeComponentType.VARIABLE;
  }
}
