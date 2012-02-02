package com.intellij.plugins.haxe.lang.psi;

import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.psi.tree.IFileElementType;

public class HaxeFileElementType extends IFileElementType {
  public HaxeFileElementType() {
    super(HaxeFileType.HAXE_LANGUAGE);
  }
}
