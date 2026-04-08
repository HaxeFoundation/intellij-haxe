package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeEmptyContainerStub;
import com.intellij.psi.stubs.IStubElementType;

/**
 * Base class for stub-aware PSI elements that are container-only (no serialized data).
 * Extends HaxeStubBasedPsiElementBase with the generic HaxeContainerStub type.
 */
public class HaxeContainerStubPsiElementBase extends HaxeStubBasedPsiElementBase<HaxeEmptyContainerStub<?>> {

  public HaxeContainerStubPsiElementBase(ASTNode node) {
    super(node);
  }

  public HaxeContainerStubPsiElementBase(HaxeEmptyContainerStub<?> stub, IStubElementType<?, ?> type) {
    super(stub, type);
  }
}
