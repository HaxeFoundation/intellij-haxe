package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeComponentWithDeclarationList extends HaxeComponent {
  @Nullable
  HaxeDeclarationAttributeList getDeclarationAttributeList();
}
