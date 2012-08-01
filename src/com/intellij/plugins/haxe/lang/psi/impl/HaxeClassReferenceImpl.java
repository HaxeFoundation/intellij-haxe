package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassReferenceImpl extends HaxeExpressionImpl implements HaxeReference {
  public HaxeClassReferenceImpl(ASTNode node) {
    super(node);
  }

  @Override
  public PsiElement getElement() {
    return this;
  }

  @Override
  public PsiReference getReference() {
    return this;
  }

  @Override
  public TextRange getRangeInElement() {
    final TextRange textRange = getTextRange();
    return new TextRange(0, textRange.getEndOffset() - textRange.getStartOffset());
  }

  @Override
  public PsiElement resolve() {
    return null;
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return getText();
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    return this;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    return this;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    return false;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @NotNull
  @Override
  public HaxeClassResolveResult resolveHaxeClass() {
    final HaxeFunctionType functionType = PsiTreeUtil.getChildOfType(this, HaxeFunctionType.class);
    HaxeTypeOrAnonymous typeOrAnonymous = PsiTreeUtil.getChildOfType(this, HaxeTypeOrAnonymous.class);
    if (functionType != null && !functionType.getTypeOrAnonymousList().isEmpty()) {
      typeOrAnonymous = functionType.getTypeOrAnonymousList().iterator().next();
    }
    final HaxeType type = typeOrAnonymous != null ? typeOrAnonymous.getType() : PsiTreeUtil.getChildOfType(this, HaxeType.class);
    return HaxeClassResolveResult.create(HaxeResolveUtil.tryResolveClassByQName(type));
  }
}
