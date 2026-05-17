package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeFieldStubSerializer implements StubSerializer<HaxeFieldStub> {

  private final IElementType myElementType;

  public HaxeFieldStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.field." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeFieldStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getKeywordFlags());
    dataStream.writeVarInt(stub.getMetaFlags());
    dataStream.writeName(stub.getGetter());
    dataStream.writeName(stub.getSetter());
  }

  @NotNull
  @Override
  public HaxeFieldStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef   = dataStream.readName();
    int flags           = dataStream.readVarInt();
    int metaFlags       = dataStream.readVarInt();
    StringRef getterRef = dataStream.readName();
    StringRef setterRef = dataStream.readName();
    String name   = nameRef   != null ? nameRef.getString()   : null;
    String getter = getterRef != null ? getterRef.getString() : null;
    String setter = setterRef != null ? setterRef.getString() : null;
    return new HaxeFieldStub(parentStub, myElementType, name, flags, metaFlags, getter, setter);
  }

  @Override
  public void indexStub(@NotNull HaxeFieldStub stub, @NotNull IndexSink sink) {
    String name = stub.getName();
    if(stub.isStatic()) {
      sink.occurrence(HaxeStaticFieldNameStubIndex.KEY, name);
    }
    if (name != null) {
      sink.occurrence(HaxeFieldNameStubIndex.KEY, name);
    }
  }
}