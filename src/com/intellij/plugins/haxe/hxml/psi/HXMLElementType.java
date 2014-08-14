package com.intellij.plugins.haxe.hxml.psi;

import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class HXMLElementType  extends IElementType {

  public HXMLElementType(@NotNull @NonNls String debugName) {
    super(debugName, HXMLLanguage.INSTANCE);
  }

}