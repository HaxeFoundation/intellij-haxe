package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeModuleStub;
import com.intellij.psi.stubs.EmptyStubSerializer;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class HaxeModuleStubSerializer implements EmptyStubSerializer<HaxeModuleStub> {

  private final IElementType myElementType;

  public HaxeModuleStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.module." + myElementType;
  }

  @Override
  public @NonNull HaxeModuleStub instantiate(StubElement<?> element) {
    return new HaxeModuleStub(element, myElementType);
  }

  @Override
  public void indexStub(@NotNull HaxeModuleStub stub, @NotNull IndexSink sink) {
    // Structural only — nothing to index
  }
}