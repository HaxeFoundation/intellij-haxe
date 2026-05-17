package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeComponentNameStub;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeComponentNameStubSerializer implements StubSerializer<HaxeComponentNameStub> {

  private final IElementType myElementType;

  public HaxeComponentNameStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.component." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeComponentNameStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
  }

  @NotNull
  @Override
  public HaxeComponentNameStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef name = dataStream.readName();
    return new HaxeComponentNameStub(parentStub, myElementType, name != null ? name.getString() : null);
  }

  @Override
  public void indexStub(@NotNull HaxeComponentNameStub stub, @NotNull IndexSink sink) {
  }
}