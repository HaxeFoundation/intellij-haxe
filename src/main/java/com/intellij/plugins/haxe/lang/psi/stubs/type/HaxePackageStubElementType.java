package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePackageStatementImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxePackageStub;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxePackageStubElementType extends IStubElementType<HaxePackageStub, HaxePackageStatement> {

  public HaxePackageStubElementType() {
    super("PACKAGE_STATEMENT", HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.statement.package";
  }

  @Override
  public HaxePackageStatement createPsi(@NotNull HaxePackageStub stub) {
    return new HaxePackageStatementImpl(stub, this);
  }

  @NotNull
  @Override
  public HaxePackageStub createStub(@NotNull HaxePackageStatement psi, StubElement parentStub) {
    String packageName = psi.getPackageName();
    return new HaxePackageStub(parentStub, this, packageName);
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
    return new HaxePackageStub(parentStub, this, packageName);
  }

  @Override
  public void indexStub(@NotNull HaxePackageStub stub, @NotNull IndexSink sink) {
      // TODO mlo: consider index for package completion ?
  }
}


