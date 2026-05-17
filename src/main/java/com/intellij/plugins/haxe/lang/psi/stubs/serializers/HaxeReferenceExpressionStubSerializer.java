package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeReferenceExpressionStub;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeReferenceExpressionStubSerializer implements StubSerializer<HaxeReferenceExpressionStub> {

  private final IElementType myElementType;

  public HaxeReferenceExpressionStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.reference." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeReferenceExpressionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getText());
  }

  @NotNull
  @Override
  public HaxeReferenceExpressionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef text = dataStream.readName();
    return new HaxeReferenceExpressionStub(parentStub, myElementType, text.getString());
  }

  @Override
  public void indexStub(@NotNull HaxeReferenceExpressionStub stub, @NotNull IndexSink sink) {
  }
}