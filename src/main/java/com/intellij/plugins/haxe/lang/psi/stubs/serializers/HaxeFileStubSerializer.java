package com.intellij.plugins.haxe.lang.psi.stubs.serializers;

import com.intellij.plugins.haxe.lang.psi.stubs.index.specialized.HaxeImportHxStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFileStub;
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

public class HaxeFileStubSerializer implements StubSerializer<HaxeFileStub> {

  @Override
  public @NotNull String getExternalId() {
    return "haxe.file";
  }

  @Override
  public void serialize(@NotNull HaxeFileStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getFileName());
  }

  @NotNull
  @Override
  public HaxeFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef stringRef = dataStream.readName();
    return new HaxeFileStub(null, stringRef.getString());
  }

  @Override
  public void indexStub(@NonNull HaxeFileStub stub, @NotNull IndexSink sink) {
    if("import.hx".equals(stub.getFileName())) {
      sink.occurrence(HaxeImportHxStubIndex.KEY, stub.getPackageName());
    }
  }
}