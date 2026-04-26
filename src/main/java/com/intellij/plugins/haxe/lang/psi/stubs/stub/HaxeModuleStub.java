package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;

/**
 * Lightweight structural stub for the module rule.
 * Exists only so that child class/method/field stubs have a proper parent in the stub tree.
 */
public class HaxeModuleStub extends StubBase<HaxeModule> {

  public HaxeModuleStub(StubElement parent, @NotNull IStubElementType elementType) {
    super(parent, elementType);
  }

}

