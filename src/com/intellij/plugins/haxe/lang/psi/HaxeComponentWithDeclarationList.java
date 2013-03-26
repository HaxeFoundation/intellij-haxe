package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeComponentWithDeclarationList extends HaxeComponent {
  @Nullable
  List<HaxeDeclarationAttribute> getDeclarationAttributeList();
}
