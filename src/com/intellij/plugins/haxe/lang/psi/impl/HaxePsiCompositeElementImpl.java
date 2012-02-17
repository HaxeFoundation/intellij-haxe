package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxePsiCompositeElement;
import com.intellij.plugins.haxe.lang.psi.PsiIdentifiedElement;
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
    addFunctionDeclarations(result, lastParent, PsiTreeUtil.getChildrenOfType(this, HaxeFunctionDeclarationWithAttributesImpl.class));
    addLocalFunctionDeclarations(result, lastParent, PsiTreeUtil.getChildrenOfType(this, HaxeLocalFunctionDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeClassDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeEnumDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeInterfaceDeclarationImpl.class));
    addDeclarations(result, PsiTreeUtil.getChildrenOfType(this, HaxeTypedefDeclarationImpl.class));
    return result;
  }

  private static void addLocalFunctionDeclarations(@NotNull List<PsiElement> result,
                                                   PsiElement lastParent,
                                                   @Nullable HaxeLocalFunctionDeclarationImpl[] items) {
    addDeclarations(result, items);
    if (items == null) {
      return;
    }
    for (HaxeLocalFunctionDeclarationImpl localFunctionDeclaration : items) {
      if (localFunctionDeclaration.getParameterList() != null && PsiTreeUtil.isAncestor(localFunctionDeclaration, lastParent, false)) {
        result.addAll(UsefulPsiTreeUtil.getSubnodesOfType(localFunctionDeclaration, PsiIdentifiedElement.class));
      }
    }
  }

  private static void addFunctionDeclarations(@NotNull List<PsiElement> result,
                                              PsiElement lastParent,
                                              @Nullable HaxeFunctionDeclarationWithAttributesImpl[] items) {
    addDeclarations(result, items);
    if (items == null) {
      return;
    }
    for (HaxeFunctionDeclarationWithAttributesImpl functionDeclarationWithAttributes : items) {
      if (functionDeclarationWithAttributes.getParameterList() != null && PsiTreeUtil.isAncestor(functionDeclarationWithAttributes, lastParent, false)) {
        result.addAll(UsefulPsiTreeUtil.getSubnodesOfType(functionDeclarationWithAttributes, PsiIdentifiedElement.class));
      }
    }
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
