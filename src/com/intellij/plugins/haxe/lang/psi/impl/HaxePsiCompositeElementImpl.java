package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
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
    addVarDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeVarDeclaration.class));
    addLocalVarDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeLocalVarDeclaration.class));

    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeFunctionDeclarationWithAttributes.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeLocalFunctionDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeClassDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeExternClassDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeEnumDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeInterfaceDeclaration.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeTypedefDeclaration.class));

    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(this, HaxeParameterList.class);
    if (parameterList != null) {
      result.addAll(parameterList.getParameterList());
    }

    if (this instanceof HaxeForStatement) {
      result.add(this);
    }
    return result;
  }

  private static void addLocalVarDeclarations(@NotNull List<PsiElement> result, @Nullable HaxeLocalVarDeclaration[] items) {
    if (items == null) {
      return;
    }
    for (HaxeLocalVarDeclaration varDeclaration : items) {
      result.addAll(varDeclaration.getLocalVarDeclarationPartList());
    }
  }

  private static void addVarDeclarations(@NotNull List<PsiElement> result, @Nullable HaxeVarDeclaration[] items) {
    if (items == null) {
      return;
    }
    for (HaxeVarDeclaration varDeclaration : items) {
      result.addAll(varDeclaration.getVarDeclarationPartList());
    }
  }

  private static void addDeclarations(@NotNull List<PsiElement> result, @Nullable PsiElement[] items) {
    if (items != null) {
      result.addAll(Arrays.asList(items));
    }
  }
}
