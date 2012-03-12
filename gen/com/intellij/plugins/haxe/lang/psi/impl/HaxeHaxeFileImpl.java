// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeHaxeFileImpl extends HaxePsiCompositeElementImpl implements HaxeHaxeFile {

  public HaxeHaxeFileImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeClassDeclaration> getClassDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeClassDeclaration.class);
  }

  @Override
  @NotNull
  public List<HaxeEnumDeclaration> getEnumDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeEnumDeclaration.class);
  }

  @Override
  @NotNull
  public List<HaxeExternClassDeclaration> getExternClassDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeExternClassDeclaration.class);
  }

  @Override
  @NotNull
  public List<HaxeImportStatement> getImportStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeImportStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeInterfaceDeclaration> getInterfaceDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeInterfaceDeclaration.class);
  }

  @Override
  @Nullable
  public HaxePackageStatement getPackageStatement() {
    return findChildByClass(HaxePackageStatement.class);
  }

  @Override
  @NotNull
  public List<HaxePp> getPpList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxePp.class);
  }

  @Override
  @NotNull
  public List<HaxeTypedefDeclaration> getTypedefDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeTypedefDeclaration.class);
  }

}
