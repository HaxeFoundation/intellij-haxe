package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeReference extends HaxeExpression, PsiReference {
  @NotNull
  HaxeIdentifier getIdentifier();

  @Nullable
  HaxeClass getHaxeClass();
}
