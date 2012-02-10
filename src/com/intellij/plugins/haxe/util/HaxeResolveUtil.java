package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiElement;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeResolveUtil {
  public static PsiElement getTopReferenceParent(final PsiElement parent) {
    PsiElement currentParent = parent;
    while (currentParent instanceof HaxeReferenceExpression) {
      currentParent = currentParent.getParent();
    }
    return currentParent;
  }
}
