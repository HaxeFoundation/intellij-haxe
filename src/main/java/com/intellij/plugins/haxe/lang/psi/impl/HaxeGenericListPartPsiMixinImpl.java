package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeGenericListPartStub;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Note: this is a Stub mixin as GenericListPart is treated as a class/Type
 * (HaxeTypeParameterDeclarationPsiMixinImpl -> AbstractHaxePsiClass -> HaxeStubBasedNamedComponent)
 */
public abstract class HaxeGenericListPartPsiMixinImpl extends HaxeTypeParameterDeclarationPsiMixinImpl {

  public HaxeGenericListPartPsiMixinImpl(@NotNull ASTNode node) {
    super(node);
  }

  public HaxeGenericListPartPsiMixinImpl(@NotNull HaxeClassStub stub, @NotNull IStubElementType<?, ?> type) {
    super(stub, type);
  }

  @Override
  @Nullable
  public HaxeGenericListPartStub getStub() {
    HaxeClassStub stub = super.getStub();
    return stub instanceof HaxeGenericListPartStub ? (HaxeGenericListPartStub) stub : null;
  }
}
