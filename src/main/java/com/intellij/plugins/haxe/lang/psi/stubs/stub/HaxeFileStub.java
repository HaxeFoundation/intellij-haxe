package com.intellij.plugins.haxe.lang.psi.stubs.stub;

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class HaxeFileStub extends PsiFileStubImpl<HaxeFile> {


  @Getter
  private final String fileName; // used to easly find `import.hx` files


  public HaxeFileStub(HaxeFile file, String fileName) {
    super(file);
    this.fileName = fileName;
  }

  @Override
  public @NotNull IElementType getFileElementType() {
    return HaxeTokenTypeSets.HAXE_FILE;
  }

  /**
   * Returns the package name by finding the child {@link HaxePackageStub}.
   * This avoids storing a redundant copy of the package name on the file stub itself.
   */
  @NotNull
  public String getPackageName() {
    for (StubElement<?> child : getChildrenStubs()) {
      if (child instanceof HaxePackageStub packageStub) {
        return packageStub.getPackageName();
      }
    }
    return ""; // no package statment defaults to root package
  }
}

