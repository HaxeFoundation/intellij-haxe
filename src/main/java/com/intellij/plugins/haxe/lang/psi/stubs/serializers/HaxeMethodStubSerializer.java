package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeModuleMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeStaticMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedMemberNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeConstructorStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeMethodStub;
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

public class HaxeMethodStubSerializer implements StubSerializer<HaxeMethodStub> {


  private final IElementType myElementType;
  public HaxeMethodStubSerializer(IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return  "haxe.method." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeMethodStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getKeywordFlags());
    dataStream.writeVarInt(stub.getMetaFlags());
    dataStream.writeVarInt(stub.getPropertyFlags());
  }

  @NotNull
  @Override
  public HaxeMethodStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    int keywordFlags = dataStream.readVarInt();
    int metaFlags = dataStream.readVarInt();
    int propertyFlags = dataStream.readVarInt();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeMethodStub(parentStub, myElementType, name, keywordFlags, metaFlags, propertyFlags);
  }

  @Override
  public void indexStub(@NotNull HaxeMethodStub stub, @NotNull IndexSink sink) {
    if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(stub)) {
      return;
    }

    String name = stub.getName();
    if(stub.isConstructor()) {
      if( stub.getParentStub() instanceof HaxeClassStub classStub) {
        sink.occurrence(HaxeConstructorStubIndex.KEY, classStub.getQualifiedName(false));
        sink.occurrence(HaxeConstructorStubIndex.KEY, classStub.getQualifiedName(true));
      }
    }else if(stub.isStatic()) {
      sink.occurrence(HaxeStaticMethodNameStubIndex.KEY, name);
      addToFqn(sink, stub);
    }else if (stub.getParentStub() instanceof HaxeClassStub) {
      sink.occurrence(HaxeClassMethodNameStubIndex.KEY, name);
      addToFqn(sink, stub);
    }else if (stub.getParentStub() instanceof HaxeModuleStub) {
      sink.occurrence(HaxeModuleMethodNameStubIndex.KEY, name);
      addToFqn(sink, stub);
    }
  }

  private static void addToFqn(@NotNull IndexSink sink, @NonNull HaxeMethodStub stub) {
    HaxeBaseMemberModel model = stub.getPsi().getModel();
    if(model != null) {
      FullyQualifiedInfo qualifiedInfo = model.getQualifiedInfo();
      sink.occurrence(HaxeFullyQualifiedMemberNameStubIndex.KEY, qualifiedInfo.getQualifiedName(true));
    }
  }

}