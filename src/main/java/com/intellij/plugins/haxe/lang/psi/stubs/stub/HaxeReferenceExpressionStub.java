package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Stub for container PSI elements that hold no data of their own. */
public class HaxeReferenceExpressionStub extends StubBase<HaxeReferenceExpression> {

  private final String text;

  public HaxeReferenceExpressionStub(StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                           @Nullable String text) {
    super(parent, elementType);
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
