package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeReference extends HaxeExpression, PsiReference {
  @Nullable
  HaxeIdentifier getIdentifier();

  @Nullable
  HaxeClass getHaxeClass();
}
