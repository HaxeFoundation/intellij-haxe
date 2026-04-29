package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeParameter;
import com.intellij.plugins.haxe.lang.psi.HaxeRestParameter;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeParameterStub;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * Shared IStubElementType for parameter-like declarations (parameter, restParameter, untypedParameter).
 */
public class HaxeParameterStubElementType extends IStubElementType<HaxeParameterStub, HaxeParameter> {

  private final BiFunction<HaxeParameterStub, HaxeParameterStubElementType, ? extends HaxeParameter> myPsiCreator;

  public HaxeParameterStubElementType(@NotNull String debugName,
                                       @NotNull BiFunction<HaxeParameterStub, HaxeParameterStubElementType, ? extends HaxeParameter> psiCreator) {
    super(debugName, HaxeLanguage.INSTANCE);
    myPsiCreator = psiCreator;
  }

  @NotNull
  @Override
  public String getExternalId() {
    //want to use getDebugName here instead of "this", but it's marked as internal;
    // however, toString returns the value from getDebugName so "+ this" gives us the same result.
    return "haxe.parameter." + this;
  }

  @Override
  public HaxeParameter createPsi(@NotNull HaxeParameterStub stub) {
    return myPsiCreator.apply(stub, this);
  }

  @NotNull
  @Override
  public HaxeParameterStub createStub(@NotNull HaxeParameter psi, StubElement<?> parentStub) {
    boolean isOptional = psi.getOptionalMark() != null;
    boolean hasInit = psi.getVarInit() != null;
    boolean isRest = psi instanceof HaxeRestParameter;
    return new HaxeParameterStub(parentStub, this, psi.getName(), isOptional, isRest, hasInit);
  }

  @Override
  public void serialize(@NotNull HaxeParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getFlags());
  }

  @NotNull
  @Override
  public HaxeParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef nameRef = dataStream.readName();
    int flags = dataStream.readVarInt();
    String name = nameRef != null ? nameRef.getString() : null;
    return new HaxeParameterStub(parentStub, this, name, flags);
  }

  @Override
  public void indexStub(@NotNull HaxeParameterStub stub, @NotNull IndexSink sink) {
    // NOOP — no stub indexes for parameter elements.
  }
}
