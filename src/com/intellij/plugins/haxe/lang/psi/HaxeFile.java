package com.intellij.plugins.haxe.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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

  @Override
  public Icon getIcon(int flags) {
    return super.getIcon(flags);
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return super.findReferenceAt(offset);
  }
  @Override
  public PsiElement setName(@NotNull String newName) throws IncorrectOperationException {
    final String oldName = FileUtil.getNameWithoutExtension(getName());
    final PsiElement result = super.setName(newName);
    final HaxeClass haxeClass = HaxeResolveUtil.findComponentDeclaration(this, oldName);
    if(haxeClass != null){
      haxeClass.setName(FileUtil.getNameWithoutExtension(newName));
    }
    return result;
  }
}
