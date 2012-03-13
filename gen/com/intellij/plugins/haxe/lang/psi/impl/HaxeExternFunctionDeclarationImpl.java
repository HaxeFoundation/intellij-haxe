// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeExternFunctionDeclarationImpl extends HaxePsiCompositeElementImpl implements HaxeExternFunctionDeclaration {

  public HaxeExternFunctionDeclarationImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeBlockStatement getBlockStatement() {
    return findChildByClass(HaxeBlockStatement.class);
  }

  @Override
  @Nullable
  public HaxeComponentName getComponentName() {
    return findChildByClass(HaxeComponentName.class);
  }

  @Override
  @Nullable
  public HaxeDeclarationAttributeList getDeclarationAttributeList() {
    return findChildByClass(HaxeDeclarationAttributeList.class);
  }

  @Override
  @Nullable
  public HaxeParameterList getParameterList() {
    return findChildByClass(HaxeParameterList.class);
  }

  @Override
  @NotNull
  public List<HaxeRequireMeta> getRequireMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeRequireMeta.class);
  }

  @Override
  @Nullable
  public HaxeReturnStatementWithoutSemicolon getReturnStatementWithoutSemicolon() {
    return findChildByClass(HaxeReturnStatementWithoutSemicolon.class);
  }

  @Override
  @Nullable
  public HaxeTypeParam getTypeParam() {
    return findChildByClass(HaxeTypeParam.class);
  }

  @Override
  @Nullable
  public HaxeTypeTag getTypeTag() {
    return findChildByClass(HaxeTypeTag.class);
  }
}
