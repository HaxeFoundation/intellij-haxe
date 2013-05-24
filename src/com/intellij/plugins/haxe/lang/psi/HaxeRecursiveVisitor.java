package com.intellij.plugins.haxe.lang.psi;

import com.intellij.psi.PsiElement;

/**
 * Created by fedorkorotkov.
 */
public class HaxeRecursiveVisitor extends HaxeVisitor {
  @Override
  public void visitElement(PsiElement element) {
    super.visitElement(element);
    element.acceptChildren(this);
  }
}
