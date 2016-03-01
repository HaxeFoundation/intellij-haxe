/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
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
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.lang.psi.impl.HaxeNamedElementImpl");

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

    if (oldName != null &&
        getParent() instanceof HaxeClass &&
        getParent().getParent() instanceof HaxeFile)
    {
      final HaxeFile haxeFile = (HaxeFile)getParent().getParent();
      if (oldName.equals(FileUtil.getNameWithoutExtension(haxeFile.getName()))) {
        haxeFile.setName(newElementName + "." + FileUtilRt.getExtension(haxeFile.getName()));
      }
    }
    return this;
  }

  @Override
  public String getName() {
    try {
      return getIdentifier().getText();
    }
    catch (Throwable t) {
      // log & swallow
      LOG.error(t.getMessage());
      return "";
    }
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
      return new LocalSearchScope(component.getParent());
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
