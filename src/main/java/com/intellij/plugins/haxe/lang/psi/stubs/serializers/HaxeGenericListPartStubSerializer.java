package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeGenericListPartStub;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeGenericListPartStubSerializer implements StubSerializer<HaxeGenericListPartStub> {

  private final IElementType myElementType;

  public HaxeGenericListPartStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.generic." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeGenericListPartStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
  }

  @NotNull
  @Override
  public HaxeGenericListPartStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeGenericListPartStub(parentStub, myElementType, name);
  }

  @Override
  public void indexStub(@NotNull HaxeGenericListPartStub stub, @NotNull IndexSink sink) {
    // NOOP — no stub indexes for type parameter elements.
  }
}