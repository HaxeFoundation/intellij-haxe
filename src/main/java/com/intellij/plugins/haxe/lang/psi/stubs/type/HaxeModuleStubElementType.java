package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeModuleStub;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public class HaxeModuleStubElementType
  extends IStubElementType<HaxeModuleStub, HaxeModule>
  implements EmptyStubSerializer<HaxeModuleStub> {

  public HaxeModuleStubElementType() {
    super("MODULE", com.intellij.plugins.haxe.HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.module";
  }

  @Override
  public HaxeModule createPsi(@NotNull HaxeModuleStub stub) {
    return new com.intellij.plugins.haxe.lang.psi.impl.HaxeModuleImpl(stub, this);
  }

  @NotNull
  @Override
  public HaxeModuleStub createStub(@NotNull HaxeModule psi, StubElement parentStub) {
    return new HaxeModuleStub(parentStub, this);
  }

  @Override
  public @NonNull HaxeModuleStub instantiate(StubElement<?> element) {
    return new HaxeModuleStub(element, this);
  }

  @Override
  public void indexStub(@NotNull HaxeModuleStub stub, @NotNull IndexSink sink) {
    // Structural only — nothing to index
  }
}

