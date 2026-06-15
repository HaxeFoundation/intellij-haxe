package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/** Stub for container PSI elements that hold no data of their own. */
public class HaxeEmptyContainerStub<T extends PsiElement> extends StubBase<T> {
  public HaxeEmptyContainerStub(StubElement<?> parent, @NotNull IElementType elementType) {
    super(parent, elementType);
  }
}
