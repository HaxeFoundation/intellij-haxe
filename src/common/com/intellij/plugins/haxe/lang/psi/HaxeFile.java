/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeFile extends PsiFileBase
  implements HaxeModifierListOwner, PsiClassOwner {

  public HaxeFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, HaxeLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return HaxeFileType.HAXE_FILE_TYPE;
  }

  @Override
  public String toString() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      // Unit tests expect the fixed string.  Maybe we should fix the test goldens, then?
      return "Haxe File";
    }
    return getName();
  }

  @Override
  public Icon getIcon(int flags) {
    return super.getIcon(flags);
  }

  @Override
  public PsiReference findReferenceAt(int offset) {
    return super.findReferenceAt(offset);
  }

  @Override
  public PsiElement setName(@NotNull String newName) throws IncorrectOperationException {
    final String oldName = FileUtil.getNameWithoutExtension(getName());
    final PsiElement result = super.setName(newName);
    final HaxeClass haxeClass = HaxeResolveUtil.findComponentDeclaration(this, oldName);
    if (haxeClass != null) {
      haxeClass.setName(FileUtil.getNameWithoutExtension(newName));
    }
    return result;
  }

  @Nullable
  @Override
  public HaxeModifierList getModifierList() {
    // usually files don't have annotations or modifiers associated with them
    return null;
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    // usually files don't have annotations or modifiers associated with them
    return false;
  }

  @NotNull
  @Override
  public PsiClass[] getClasses() {
    return HaxeHierarchyUtils.getClassList(this);
  }

  public PsiPackageStatement getPackageStatement() {
    ASTNode node = calcTreeElement().findChildByType(HaxeTokenTypes.PACKAGE_STATEMENT);
    return node != null ? (PsiPackageStatement)node.getPsi() : null;
  }

  @Override
  public String getPackageName() {
    PsiPackageStatement statement = getPackageStatement();
    return statement == null ? "" : statement.getPackageName();
  }

  @Override
  public void setPackageName(String packageName) throws IncorrectOperationException {
    // TODO: verify
    HaxePackageStatement packageStatementFromPath = HaxeElementGenerator.createPackageStatementFromPath(getProject(), packageName);

    HaxePackageStatement packageStatement = PsiTreeUtil.getChildOfType(this, HaxePackageStatement.class);
    if (packageStatement != null) {
      packageStatement.replace(packageStatementFromPath);
    }
    else {
      addBefore(packageStatementFromPath, getFirstChild());
    }
  }
}
