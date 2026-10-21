package com.intellij.plugins.haxe.buildsystem.hxml.psi.mixin;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.hxml.psi.HXMLMain;
import com.intellij.plugins.haxe.hxml.psi.HXMLQualifiedName;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A type path in an hxml file; after {@code -main} it refers to the class the build starts from. */
public abstract class HxmlQualifiedNameMixin extends ASTWrapperPsiElement {

  public HxmlQualifiedNameMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiReference getReference() {
    if (getParent() instanceof HXMLMain && this instanceof HXMLQualifiedName typePath) {
      return new HxmlMainClassReference(typePath);
    }
    return null;
  }
}
