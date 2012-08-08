package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public abstract class HaxeNamedElementImpl extends HaxePsiCompositeElementImpl implements HaxeComponentName {
  public HaxeNamedElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String newElementName) throws IncorrectOperationException {
    final HaxeIdentifier identifier = getIdentifier();
    final HaxeIdentifier identifierNew = HaxeElementGenerator.createIdentifierFromText(getProject(), newElementName);

    final String oldName = getName();
    if (identifierNew != null) {
      getNode().replaceChild(identifier.getNode(), identifierNew.getNode());
    }

    if (getParent() instanceof HaxeClass) {
      final HaxeFile haxeFile = (HaxeFile)getParent().getParent();
      if (oldName != null && oldName.equals(FileUtil.getNameWithoutExtension(haxeFile.getName()))) {
        haxeFile.setName(newElementName + "." + FileUtil.getExtension(haxeFile.getName()));
      }
    }
    return this;
  }

  @Override
  public String getName() {
    return getIdentifier().getText();
  }

  @Override
  public PsiElement getNameIdentifier() {
    return this;
  }

  @NotNull
  @Override
  public SearchScope getUseScope() {
    final HaxeComponentType type = HaxeComponentType.typeOf(getParent());
    final HaxeComponent component = PsiTreeUtil.getParentOfType(getParent(), HaxeComponent.class, true);
    if (type == null || component == null) {
      return super.getUseScope();
    }
    if (type == HaxeComponentType.FUNCTION || type == HaxeComponentType.PARAMETER || type == HaxeComponentType.VARIABLE) {
      return new LocalSearchScope(component);
    }
    return super.getUseScope();
  }

  @Nullable
  public ItemPresentation getPresentation() {
    if (getParent() instanceof NavigationItem) {
      return ((NavigationItem)getParent()).getPresentation();
    }
    return null;
  }

  @Override
  public Icon getIcon(int flags) {
    return getParent().getIcon(flags);
  }
}
