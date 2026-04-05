package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for using statements.
 * Stores the qualified path text of the using declaration.
 */
public class HaxeUsingStub extends StubBase<HaxeUsingStatement> {

  private final String usingPath;

  public HaxeUsingStub(StubElement parent,
                        @NotNull IStubElementType elementType,
                        @Nullable String usingPath) {
    super(parent, elementType);
    this.usingPath = usingPath;
  }

  @Nullable
  public String getUsingPath() {
    return usingPath;
  }
}

