package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.impl.HaxeIterator;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;

/**
 * MLO: comment:
 * Made iterator into a stub just so we don't need to have and maintain 2 different AbstractHaxeNamedComponent variants
 * (one for stubbed PSI and one for none stubbed)
 */
public class HaxeIteratorStub extends StubBase<HaxeIterator> {

  public HaxeIteratorStub(StubElement parent, @NotNull IStubElementType elementType) {
    super(parent, elementType);
  }
}

