package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.indexes.utils.HaxeIndexUtil;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.fqn.HaxeFullyQualifiedParameterNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeParameterStub;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeParameterStubSerializer implements StubSerializer<HaxeParameterStub> {

  private final IElementType myElementType;

  public HaxeParameterStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.parameter." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxeParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getFlags());
  }

  @NotNull
  @Override
  public HaxeParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    int flags = dataStream.readVarInt();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeParameterStub(parentStub, myElementType, name, flags);
  }

  @Override
  public void indexStub(@NotNull HaxeParameterStub stub, @NotNull IndexSink sink) {
    if (HaxeIndexUtil.fileBelongToPlatformSpecificStd(stub)) {
      return;
    }

    HaxeModel model = stub.getPsi().getModel();
    if(model != null) {
      FullyQualifiedInfo qualifiedInfo = model.getQualifiedInfo();
      sink.occurrence(HaxeFullyQualifiedParameterNameStubIndex.KEY, qualifiedInfo.getQualifiedName(true));
    }
  }
}