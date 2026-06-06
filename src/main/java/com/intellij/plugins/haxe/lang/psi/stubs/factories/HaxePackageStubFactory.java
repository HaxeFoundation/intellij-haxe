package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePackageStatementImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxePackageStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import org.jetbrains.annotations.NotNull;

public class HaxePackageStubFactory implements StubElementFactory<HaxePackageStub, HaxePackageStatement> {

  private final HaxeElementType myElementType;

  public HaxePackageStubFactory(@NotNull HaxeElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public HaxePackageStatement createPsi(@NotNull HaxePackageStub stub) {
    return new HaxePackageStatementImpl(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxePackageStub createStub(@NotNull HaxePackageStatement psi, StubElement parentStub) {
    String packageName = psi.getPackageName();
    return new HaxePackageStub(parentStub, myElementType, packageName);
  }
}