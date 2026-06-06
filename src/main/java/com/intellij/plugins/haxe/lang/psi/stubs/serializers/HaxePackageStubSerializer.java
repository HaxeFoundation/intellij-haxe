package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxePackageStub;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxePackageStubSerializer implements StubSerializer<HaxePackageStub> {

  private final IElementType myElementType;

  public HaxePackageStubSerializer(@NotNull IElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public @NotNull String getExternalId() {
    return "haxe.package." + myElementType;
  }

  @Override
  public void serialize(@NotNull HaxePackageStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getPackageName());
  }

  @NotNull
  @Override
  public HaxePackageStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef packageNameRef = dataStream.readName();
    String packageName = packageNameRef != null ? packageNameRef.getString() : null;
    return new HaxePackageStub(parentStub, myElementType, packageName);
  }

  @Override
  public void indexStub(@NotNull HaxePackageStub stub, @NotNull IndexSink sink) {
    // TODO mlo: consider index for package completion ?
  }
}