package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.helper.HaxeProcessDeclarationsHelper;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataListOwner;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.extapi.psi.StubBasedPsiElementBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Stub-aware base class mirroring functionality from HaxePsiCompositeElementImpl.
 * Used as the base for all stub-aware mixin classes.
 */
public abstract class HaxeStubBasedPsiElementBase<T extends StubElement<?>> extends StubBasedPsiElementBase<T>
  implements HaxePsiCompositeElement, HaxeModifierListOwner, HaxeMetadataListOwner {

  public HaxeStubBasedPsiElementBase(@NotNull ASTNode node) {
    super(node);
  }

  public HaxeStubBasedPsiElementBase(@NotNull T stub, @NotNull IStubElementType<?, ?> nodeType) {
    super(stub, nodeType);
  }

  public IElementType getTokenType() {
    return getNode().getElementType();
  }

  @Override
  public String toString() {
    return getElementTypeImpl().toString();

  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {
    return HaxeProcessDeclarationsHelper.processDeclarations(this, processor, state, lastParent, place);
  }

  // HaxeModifierListOwner implementations

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    HaxeModifierList list = getModifierList();
    return null != list && list.hasModifierProperty(name);
  }

  @Nullable
  @Override
  public HaxeModifierList getModifierList() {
    // This list is built in sub-classes, null is an expected value and
    // the sub-class will create a new HaxeModifierList if null is recieved.
    return null;
  }

  // HaxeMetadataListOwner implementations

  @Nullable
  @Override
  public HaxeMetadataList getMetadataList(@Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.getMetadataList(this, metadataType);
  }

  @Override
  public boolean hasMetadata(HaxeMetadataTypeName name, @Nullable Class<? extends HaxeMeta> metadataType) {
    return HaxeMetadataUtils.hasMeta(this, metadataType, name);
  }

  @Override
  public PsiElement @NotNull [] getChildren() {
    PsiElement psiChild = getFirstChild();
    if (psiChild == null) return PsiElement.EMPTY_ARRAY;

    List<PsiElement> result = null;
    while (psiChild != null) {
      if (psiChild.getNode() instanceof CompositeElement || psiChild.getNode() instanceof PsiComment) {
        if (result == null) {
          result = new ArrayList<>();
        }
        result.add(psiChild);
      }
      psiChild = psiChild.getNextSibling();
    }
    return result == null ? PsiElement.EMPTY_ARRAY : PsiUtilCore.toPsiElementArray(result);
  }
}

