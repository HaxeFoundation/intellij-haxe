package com.intellij.plugins.haxe.lang.psi.stubs.type;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFileStub;
import com.intellij.plugins.haxe.lang.psi.stubs.HaxeStubVersions;
import com.intellij.psi.PsiFile;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IStubFileElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class HaxeFileElementType extends IStubFileElementType<HaxeFileStub> {

  public HaxeFileElementType() {
    super("HAXEFILE", HaxeLanguage.INSTANCE);
  }

  @Override
  public int getStubVersion() {
    return HaxeStubVersions.STUB_VERSION;
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "haxe.file";
  }

  @Override
  public void serialize(@NotNull HaxeFileStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    // Package name is now stored in the child HaxePackageStatementStub — nothing to serialize here.
  }

  @NotNull
  @Override
  public HaxeFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new HaxeFileStub(null);
  }

  @Override
  public StubBuilder getBuilder() {
    return new DefaultStubBuilder() {
      @NotNull
      @Override
      protected PsiFileStub<?> createStubForFile(@NotNull PsiFile file) {
        if (file instanceof HaxeFile haxeFile) {
          return new HaxeFileStub(haxeFile);
        }
        return new HaxeFileStub(null);
      }
    };
  }
}

