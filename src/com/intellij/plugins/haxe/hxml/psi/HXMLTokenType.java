package com.intellij.plugins.haxe.hxml.psi;

import com.intellij.plugins.haxe.hxml.HXMLLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class HXMLTokenType extends IElementType {

  public HXMLTokenType(@NotNull @NonNls String debugName) {
    super(debugName, HXMLLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "HXMLTokenType." + super.toString();
  }
}
