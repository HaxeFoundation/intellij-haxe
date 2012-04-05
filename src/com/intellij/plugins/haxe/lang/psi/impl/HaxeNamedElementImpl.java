package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
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
    if (getParent() instanceof HaxeClass) {
      final HaxeFile haxeFile = (HaxeFile)getParent().getParent();
      final String currentName = getName();
      if (currentName != null && currentName.equals(FileUtil.getNameWithoutExtension(haxeFile.getName()))) {
        haxeFile.setName(newElementName + "." + FileUtil.getExtension(haxeFile.getName()));
      }
    }
    if (identifierNew != null) {
      getNode().replaceChild(identifier.getNode(), identifierNew.getNode());
    }
    return this;
  }

  @Override
  public String getName() {
    return getIdentifier().getText();
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
