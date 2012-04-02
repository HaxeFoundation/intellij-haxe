package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeReference extends HaxeExpression, PsiReference {
  @NotNull
  HaxeClassResolveResult resolveHaxeClass();
}
