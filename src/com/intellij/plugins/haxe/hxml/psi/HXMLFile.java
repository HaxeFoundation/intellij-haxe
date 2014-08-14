package com.intellij.plugins.haxe.hxml.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.plugins.haxe.hxml.HXMLFileType;
import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HXMLFile extends PsiFileBase {
  public HXMLFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, HXMLLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return HXMLFileType.INSTANCE;
  }

  @Override
  public String toString() {
    return "HXML File";
  }

  @Override
  public Icon getIcon(int flags) {
    return super.getIcon(flags);
  }
}