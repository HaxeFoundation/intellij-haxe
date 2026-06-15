package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.index.*;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedMemberNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeModuleStub;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

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

    if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(stub)) {
     return;
    }

    String name = stub.getName();
    if(stub.isStatic()) {
      sink.occurrence(HaxeStaticFieldNameStubIndex.KEY, name);
      addToFqn(sink, stub);
    }else if (stub.getParentStub() instanceof HaxeClassStub) {
      sink.occurrence(HaxeClassFieldNameStubIndex.KEY, name);
      addToFqn(sink, stub);
    }else if (stub.getParentStub() instanceof HaxeModuleStub) {
      sink.occurrence(HaxeModuleFieldNameStubIndex.KEY, name);
      addToFqn(sink, stub);
    }
  }

  private static void addToFqn(@NotNull IndexSink sink, @NonNull HaxeFieldStub stub) {
    HaxeBaseMemberModel model = stub.getPsi().getModel();
    if(model != null) {
      FullyQualifiedInfo qualifiedInfo = model.getQualifiedInfo();
      sink.occurrence(HaxeFullyQualifiedMemberNameStubIndex.KEY, qualifiedInfo.getQualifiedName(true));
    }
  }
}