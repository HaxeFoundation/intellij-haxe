package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeGenericListPartStub;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * IStubElementType for genericListPart (type parameter declaration).
 * Uses HaxeClass as the PSI type because HaxeGenericListPartStub extends HaxeClassStub
 * which is typed as StubBase&lt;HaxeClass&gt;. At runtime the PSI is HaxeGenericListPartImpl
 * which is a HaxeClass.
 */
public class HaxeGenericListPartStubElementType extends IStubElementType<HaxeGenericListPartStub, HaxeClass> {

  private final BiFunction<HaxeGenericListPartStub, HaxeGenericListPartStubElementType, ? extends HaxeClass> myPsiCreator;

  public HaxeGenericListPartStubElementType(
    @NotNull BiFunction<HaxeGenericListPartStub, HaxeGenericListPartStubElementType, ? extends HaxeClass> psiCreator) {
    super("GENERIC_LIST_PART", HaxeLanguage.INSTANCE);
    myPsiCreator = psiCreator;
  }

  @NotNull
  @Override
  public String getExternalId() {
    //want to use getDebugName here instead of "this", but it's marked as internal;
    // however, toString returns the value from getDebugName so "+ this" gives us the same result.
    return "haxe.type." + this;
  }

  @Override
  public HaxeClass createPsi(@NotNull HaxeGenericListPartStub stub) {
    return myPsiCreator.apply(stub, this);
  }

  @NotNull
  @Override
  public HaxeGenericListPartStub createStub(@NotNull HaxeClass psi, StubElement<?> parentStub) {
    return new HaxeGenericListPartStub(parentStub, this, psi.getName());
  }

  @Override
  public void serialize(@NotNull HaxeGenericListPartStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
  }

  @NotNull
  @Override
  public HaxeGenericListPartStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeGenericListPartStub(parentStub, this, name);
  }

  @Override
  public void indexStub(@NotNull HaxeGenericListPartStub stub, @NotNull IndexSink sink) {
    // NOOP — no stub indexes for type parameter elements.
  }
}
