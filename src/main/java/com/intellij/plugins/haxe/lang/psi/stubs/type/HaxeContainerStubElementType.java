package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeEmptyContainerStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * Generic IStubElementType for container PSI elements that hold no stub data.
 *
 * @param <P> the concrete PSI element interface
 */
public class HaxeContainerStubElementType<P extends PsiElement>
  extends IStubElementType<HaxeEmptyContainerStub<P>, P>
  implements EmptyStubSerializer<HaxeEmptyContainerStub<P>>
{

  private final BiFunction<HaxeEmptyContainerStub<P>, HaxeContainerStubElementType<P>, P> psiFactory;

  public HaxeContainerStubElementType(@NotNull String debugName,
                                       @NotNull BiFunction<HaxeEmptyContainerStub<P>, HaxeContainerStubElementType<P>, P> psiFactory) {
    super(debugName, HaxeLanguage.INSTANCE);
    this.psiFactory = psiFactory;
  }

  @NotNull
  @Override
  public String getExternalId() {
    //want to use getDebugName here instead of "this", but it's marked as internal;
    // however, toString returns the value from getDebugName so "+ this" gives us the same result.
    return "haxe.container." + this;
  }

  @Override
  public P createPsi(@NotNull HaxeEmptyContainerStub<P> stub) {
    return psiFactory.apply(stub, this);
  }

  @NotNull
  @Override
  public HaxeEmptyContainerStub<P> createStub(@NotNull P psi, StubElement<?> parentStub) {
    return new HaxeEmptyContainerStub<>(parentStub, this);
  }

  @Override
  public @NonNull HaxeEmptyContainerStub<P> instantiate(StubElement<?> element) {
    return new HaxeEmptyContainerStub<>(element, this);
  }

  @Override
  public void indexStub(@NotNull HaxeEmptyContainerStub<P> stub, @NotNull IndexSink sink) {
    // NOOP — no stub indexes for type sub-tree elements.
  }
}
