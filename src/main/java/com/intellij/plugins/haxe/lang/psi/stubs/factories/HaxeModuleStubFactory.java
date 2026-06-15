package com.intellij.plugins.haxe.lang.psi.stubs.factories;

import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeModuleImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeModuleStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import org.jetbrains.annotations.NotNull;

public class HaxeModuleStubFactory implements StubElementFactory<HaxeModuleStub, HaxeModule> {

  private final HaxeElementType myElementType;

  public HaxeModuleStubFactory(@NotNull HaxeElementType elementType) {
    myElementType = elementType;
  }

  @Override
  public HaxeModule createPsi(@NotNull HaxeModuleStub stub) {
    return new HaxeModuleImpl(stub, myElementType);
  }

  @NotNull
  @Override
  public HaxeModuleStub createStub(@NotNull HaxeModule psi, StubElement parentStub) {
    return new HaxeModuleStub(parentStub, myElementType);
  }
}