package com.intellij.plugins.haxe.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class HaxeFile extends PsiFileBase {
  public HaxeFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return HaxeFileType.HAXE_FILE_TYPE;
  }

  @Override
  public String toString() {
    return "haXe File";
  }
}
