package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.Nullable;

public class HaxeFileStub extends PsiFileStubImpl<HaxeFile> {

  public HaxeFileStub(HaxeFile file) {
    super(file);
  }

  /**
   * Returns the package name by finding the child {@link HaxePackageStub}.
   * This avoids storing a redundant copy of the package name on the file stub itself.
   */
  @Nullable
  public String getPackageName() {
    for (StubElement<?> child : getChildrenStubs()) {
      if (child instanceof HaxePackageStub packageStub) {
        return packageStub.getPackageName();
      }
    }
    return null;
  }
}

