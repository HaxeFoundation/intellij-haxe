package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Lightweight structural stub for the module rule.
 * Exists only so that child class/method/field stubs have a proper parent in the stub tree.
 */
public class HaxeModuleStub extends StubBase<HaxeModule> {

  public HaxeModuleStub(StubElement parent, @NotNull IElementType elementType) {
    super(parent, elementType);
  }

}

