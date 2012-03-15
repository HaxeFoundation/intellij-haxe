package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeReference extends HaxeExpression {
  @NotNull
  HaxeIdentifier getIdentifier();

  @Nullable
  HaxeClass getHaxeClass();
}
