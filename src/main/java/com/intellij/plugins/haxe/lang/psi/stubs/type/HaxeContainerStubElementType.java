package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeContainerStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * Generic IStubElementType for container PSI elements that hold no stub data.
 *
 * @param <P> the concrete PSI element interface
 */
public class HaxeContainerStubElementType<P extends PsiElement> extends IStubElementType<HaxeContainerStub<P>, P> {

  private final BiFunction<HaxeContainerStub<P>, HaxeContainerStubElementType<P>, P> psiFactory;

  public HaxeContainerStubElementType(@NotNull String debugName,
                                       @NotNull BiFunction<HaxeContainerStub<P>, HaxeContainerStubElementType<P>, P> psiFactory) {
    super(debugName, HaxeLanguage.INSTANCE);
    this.psiFactory = psiFactory;
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.container." + getDebugName();
  }

  @Override
  public P createPsi(@NotNull HaxeContainerStub<P> stub) {
    return psiFactory.apply(stub, this);
  }

  @NotNull
  @Override
  public HaxeContainerStub<P> createStub(@NotNull P psi, StubElement<?> parentStub) {
    return new HaxeContainerStub<>(parentStub, this);
  }

  @Override
  public void serialize(@NotNull HaxeContainerStub<P> stub, @NotNull StubOutputStream dataStream) throws IOException {
    // Container stubs hold no data — nothing to serialize.
  }

  @NotNull
  @Override
  @SuppressWarnings("rawtypes")
  public HaxeContainerStub<P> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new HaxeContainerStub<>(parentStub, this);
  }

  @Override
  public void indexStub(@NotNull HaxeContainerStub<P> stub, @NotNull IndexSink sink) {
    // NOOP — no stub indexes for type sub-tree elements.
  }
}
