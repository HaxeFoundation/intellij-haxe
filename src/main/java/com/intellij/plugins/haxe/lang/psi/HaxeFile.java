/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 TiVo Inc.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2018 Eric Bishton
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
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.stubs.*;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeModuleStub;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxePackageStub;
import com.intellij.plugins.haxe.model.HaxeFileModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.MODULE;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.PACKAGE_STATEMENT;

public class HaxeFile extends PsiFileBase
  implements HaxeModifierListOwner, PsiClassOwner {

  private HaxeFileModel haxeFileModel;

  public HaxeFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, HaxeLanguage.INSTANCE);
  }


  public HaxeModule getModule() {
    return withGreenStubOrAst(this::moduleWithStub, this::moduleWithAst);
  }

  private HaxeModule moduleWithStub(PsiFileStub<?> stub) {
    HaxeModuleStub moduleStub = (HaxeModuleStub)stub.findChildStubByElementType(MODULE);
    return moduleStub == null ? null : moduleStub.getPsi();
  }

  private HaxeModule moduleWithAst(FileElement element) {
   return PsiTreeUtil.findChildOfType(this, HaxeModule.class);
  }


  @NotNull
  @Override
  public FileType getFileType() {
    return HaxeFileType.INSTANCE;
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
  public PsiElement setName(@NotNull String newName) throws IncorrectOperationException {
   return super.setName(newName);
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
    return HaxeHierarchyUtils.getClassArray(this);
  }

  @NotNull
  public List<HaxeClass> getClassList() {
    return HaxeHierarchyUtils.getClassList(this);
  }

  public PsiPackageStatement getPackageStatement() {

    ASTNode node = calcTreeElement().findChildByType(PACKAGE_STATEMENT);
    return node != null ? (PsiPackageStatement)node.getPsi() : null;
  }

  @Override
  public String getPackageName() {
    return withGreenStubOrAst(this::PackageNameWithStub, this::PackageNameWithAst);
  }

  private String PackageNameWithStub(PsiFileStub<?> stub) {
    HaxePackageStub packageStatement =
      (HaxePackageStub)stub.findChildStubByElementType(PACKAGE_STATEMENT);
    if (packageStatement != null) {
      return packageStatement.getPackageName();
    }
    return "";
  }

  private String PackageNameWithAst(FileElement element) {
    PsiPackageStatement statement = getPackageStatement();
    return statement == null ? "" : statement.getPackageName();
  }

  @Override
  public void setPackageName(String packageName) throws IncorrectOperationException {
    // TODO: verify
    HaxePackageStatement packageStatementFromPath = HaxeElementGenerator.createPackageStatementFromPath(getProject(), packageName);

    HaxePackageStatement packageStatement = PsiTreeUtil.getStubChildOfType(this, HaxePackageStatement.class);
    if (packageStatement != null) {
      packageStatement.replace(packageStatementFromPath);
    }
    else {
      addBefore(packageStatementFromPath, getFirstChild());
    }
  }

  public List<HaxeImportStatement> getImportStatements() {
    return PsiTreeUtil.getStubChildrenOfTypeAsList(this, HaxeImportStatement.class);
  }
  public List<HaxeUsingStatement> getUsingStatements() {
    return PsiTreeUtil.getStubChildrenOfTypeAsList(this, HaxeUsingStatement.class);
  }


  @NotNull
  public HaxeFileModel getModel() {
    if(haxeFileModel != null && haxeFileModel.isValid()) return haxeFileModel;
    haxeFileModel =new HaxeFileModel(this);
    return haxeFileModel;
  }
}
