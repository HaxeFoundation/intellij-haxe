package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeImportStub;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeImportStubSerializer implements StubSerializer<HaxeImportStub> {

  private final IElementType myElementType;

  public HaxeImportStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.import." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeImportStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getImportPath());
    dataStream.writeBoolean(stub.hasWildcard());
    dataStream.writeName(stub.getAlias());
  }

  @NotNull
  @Override
  public HaxeImportStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef pathRef = dataStream.readName();
    boolean hasWildcard = dataStream.readBoolean();
    StringRef aliasRef = dataStream.readName();
    String path = pathRef != null ? pathRef.getString() : null;
    String alias = aliasRef != null ? aliasRef.getString() : null;
    return new HaxeImportStub(parentStub, myElementType, path, hasWildcard, alias);
  }

  @Override
  public void indexStub(@NotNull HaxeImportStub stub, @NotNull IndexSink sink) {
    // NOOP
  }
}