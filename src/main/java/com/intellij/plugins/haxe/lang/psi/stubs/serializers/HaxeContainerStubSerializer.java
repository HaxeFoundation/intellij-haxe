package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeEmptyContainerStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.EmptyStubSerializer;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class HaxeContainerStubSerializer<P extends PsiElement> implements EmptyStubSerializer<HaxeEmptyContainerStub<P>> {

  private final IElementType myElementType;

  public HaxeContainerStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.container." + myElementType;
  }

  @Override
  public @NonNull HaxeEmptyContainerStub<P> instantiate(StubElement<?> element) {
    return new HaxeEmptyContainerStub<>(element, myElementType);
  }

  @Override
  public void indexStub(@NotNull HaxeEmptyContainerStub<P> stub, @NotNull IndexSink sink) {
    // NOOP — no stub indexes for type sub-tree elements.
  }
}