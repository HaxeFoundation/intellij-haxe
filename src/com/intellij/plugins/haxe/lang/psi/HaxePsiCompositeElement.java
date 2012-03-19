package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author fedor.korotkov
 */
public interface HaxePsiCompositeElement extends NavigatablePsiElement {
  IElementType getTokenType();
}
