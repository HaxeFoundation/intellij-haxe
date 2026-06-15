package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeUsingStub;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeUsingStubSerializer implements StubSerializer<HaxeUsingStub> {

  private final IElementType myElementType;

  public HaxeUsingStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.using." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeUsingStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getUsingPath());
  }

  @NotNull
  @Override
  public HaxeUsingStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef pathRef = dataStream.readName();
    String path = pathRef != null ? pathRef.getString() : null;
    return new HaxeUsingStub(parentStub, myElementType, path);
  }

  @Override
  public void indexStub(@NotNull HaxeUsingStub stub, @NotNull IndexSink sink) {
  }
}