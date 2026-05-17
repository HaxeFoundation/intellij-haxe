package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Stub for HaxeComponentName — stores the identifier name text. */
public class HaxeComponentNameStub extends StubBase<HaxeComponentName> {

  private final String name;

  public HaxeComponentNameStub(StubElement<?> parent, @NotNull IElementType elementType,
                               @Nullable String name) {
    super(parent, elementType);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
