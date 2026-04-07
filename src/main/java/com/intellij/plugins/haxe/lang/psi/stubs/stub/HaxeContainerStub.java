package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;

/** Stub for container PSI elements that hold no data of their own. */
public class HaxeContainerStub<T extends PsiElement> extends StubBase<T> {
  public HaxeContainerStub(StubElement<?> parent, IStubElementType<?, ?> elementType) {
    super(parent, elementType);
  }
}
