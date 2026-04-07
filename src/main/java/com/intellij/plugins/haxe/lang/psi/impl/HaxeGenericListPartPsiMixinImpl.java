package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeGenericListPartStub;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub-aware mixin for genericListPart PSI elements.
 * Extends HaxeTypeParameterDeclarationPsiMixinImpl (which extends AbstractHaxePsiClass)
 * to inherit all HaxeClass method implementations.
 * HaxeGenericListPartStub extends HaxeClassStub so the stub constructor chain works cleanly.
 * getStub() is overridden with a covariant return type to satisfy the generated
 * HaxeGenericListPart interface which is typed StubBasedPsiElement&lt;HaxeGenericListPartStub&gt;.
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
