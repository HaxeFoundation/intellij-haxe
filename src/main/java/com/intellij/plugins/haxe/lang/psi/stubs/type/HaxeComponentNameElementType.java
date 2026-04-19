package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeComponentNameStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.function.BiFunction;

public class HaxeComponentNameElementType extends IStubElementType<HaxeComponentNameStub, HaxeComponentName> {

  private final BiFunction<HaxeComponentNameStub, HaxeComponentNameElementType, ? extends HaxeComponentName> myPsiCreator;

  public HaxeComponentNameElementType(@NotNull String debugName,
                                      @NotNull BiFunction<HaxeComponentNameStub, HaxeComponentNameElementType, ? extends HaxeComponentName> psiCreator) {
    super(debugName, HaxeLanguage.INSTANCE);
    myPsiCreator = psiCreator;
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.componentName." + getDebugName();
  }

  @Override
  public HaxeComponentName createPsi(@NotNull HaxeComponentNameStub stub) {
    return myPsiCreator.apply(stub, this);
  }

  @Override
  public @NonNull HaxeComponentNameStub createStub(@NonNull HaxeComponentName psi,
                                                   StubElement<? extends PsiElement> parentStub) {
    return new HaxeComponentNameStub(parentStub, this, psi.getName());
  }

  @Override
  public void serialize(@NotNull HaxeComponentNameStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
  }

  @NotNull
  @Override
  public HaxeComponentNameStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    StringRef name = dataStream.readName();
    return new HaxeComponentNameStub(parentStub, this, name != null ? name.getString() : null);
  }

  @Override
  public void indexStub(@NotNull HaxeComponentNameStub stub, @NotNull IndexSink sink) {
  }
}
