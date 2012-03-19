// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeExternFunctionDeclarationImpl extends AbstractHaxeNamedComponent implements HaxeExternFunctionDeclaration {

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

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitExternFunctionDeclaration(this);
    else super.accept(visitor);
  }

}
