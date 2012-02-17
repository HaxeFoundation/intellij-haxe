package com.intellij.plugins.haxe.lang.psi;

/**
 * @author: Fedor.Korotkov
 */
public interface PsiIdentifiedElement extends HaxePsiCompositeElement {
  HaxeIdentifier getIdentifier();
}
