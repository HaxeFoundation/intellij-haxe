package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HaxePsiCompositeElementImpl extends ASTWrapperPsiElement implements HaxePsiCompositeElement {
  public HaxePsiCompositeElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public IElementType getTokenType() {
    return getNode().getElementType();
  }

  public String toString() {
    return getTokenType().toString();
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {
    for (PsiElement element : getDeclarationElementToProcess(lastParent)) {
      if (!processor.execute(element, state)) {
        return false;
      }
    }
    return super.processDeclarations(processor, state, lastParent, place);
  }

  private List<PsiElement> getDeclarationElementToProcess(PsiElement lastParent) {
    final List<PsiElement> result = new ArrayList<PsiElement>();
    addVarDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeVarDeclarationImpl.class));
    addLocalVarDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeLocalVarDeclarationImpl.class));

    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeFunctionDeclarationWithAttributesImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeLocalFunctionDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeClassDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeEnumDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeInterfaceDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeTypedefDeclarationImpl.class));

    if (this instanceof HaxeFunctionDeclarationWithAttributes) {
      final HaxeParameterList parameterList = ((HaxeFunctionDeclarationWithAttributes)this).getParameterList();
      if (parameterList != null) {
        result.addAll(parameterList.getParameterList());
      }
    }

    if (this instanceof HaxeLocalFunctionDeclaration) {
      final HaxeParameterList parameterList = ((HaxeLocalFunctionDeclaration)this).getParameterList();
      if (parameterList != null) {
        result.addAll(parameterList.getParameterList());
      }
    }

    if (this instanceof HaxeForStatement) {
      result.add(this);
    }
    return result;
  }

  private static void addLocalVarDeclarations(@NotNull List<PsiElement> result, @Nullable HaxeLocalVarDeclarationImpl[] items) {
    if (items == null) {
      return;
    }
    for (HaxeLocalVarDeclarationImpl varDeclaration : items) {
      result.addAll(varDeclaration.getLocalVarDeclarationPartList());
    }
  }

  private static void addVarDeclarations(@NotNull List<PsiElement> result, @Nullable HaxeVarDeclarationImpl[] items) {
    if (items == null) {
      return;
    }
    for (HaxeVarDeclarationImpl varDeclaration : items) {
      result.addAll(varDeclaration.getVarDeclarationPartList());
    }
  }

  private static void addDeclarations(@NotNull List<PsiElement> result, @Nullable PsiElement[] items) {
    if (items != null) {
      result.addAll(Arrays.asList(items));
    }
  }
}
