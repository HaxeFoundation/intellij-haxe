package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stub for parameter declarations (parameter, restParameter, untypedParameter).
 */
public class HaxeParameterStub extends StubBase<HaxeParameter> implements StubWithName {
  private static final int IS_OPTIONAL = 0b01;
  private static final int IS_REST     = 0b10;

  private final String name;
  private final int flags;

  public HaxeParameterStub(StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                            @Nullable String name, boolean isOptional, boolean isRest) {
    super(parent, elementType);
    this.name = name;
    this.flags = (isOptional ? IS_OPTIONAL : 0) | (isRest ? IS_REST : 0);
  }

  public HaxeParameterStub(StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                            @Nullable String name, int flags) {
    super(parent, elementType);
    this.name = name;
    this.flags = flags;
  }

  @Override
  @Nullable
  public String getName() {
    return name;
  }

  public int getFlags() {
    return flags;
  }

  public boolean isOptional() {
    return (flags & IS_OPTIONAL) != 0;
  }

  public boolean isRest() {
    return (flags & IS_REST) != 0;
  }
}
