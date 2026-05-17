package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeEmptyContainerStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;

/**
 * Base class for stub-aware PSI elements that are container-only (no serialized data).
 */
public class HaxeContainerStubPsiElementBase extends HaxeStubBasedPsiElementBase<HaxeEmptyContainerStub<?>> {

  public HaxeContainerStubPsiElementBase(ASTNode node) {
    super(node);
  }

  public HaxeContainerStubPsiElementBase(HaxeEmptyContainerStub<?> stub, IElementType type) {
    super(stub, type);
  }
}
