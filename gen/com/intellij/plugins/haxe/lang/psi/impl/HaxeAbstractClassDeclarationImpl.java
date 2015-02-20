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

public class HaxeAbstractClassDeclarationImpl extends AbstractHaxePsiClass implements HaxeAbstractClassDeclaration {

  public HaxeAbstractClassDeclarationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitAbstractClassDeclaration(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public HaxeClassBody getClassBody() {
    return findChildByClass(HaxeClassBody.class);
  }

  @Override
  @Nullable
  public HaxeComponentName getComponentName() {
    return findChildByClass(HaxeComponentName.class);
  }

  @Override
  @Nullable
  public HaxeFunctionType getFunctionType() {
    return findChildByClass(HaxeFunctionType.class);
  }

  @Override
  @Nullable
  public HaxeGenericParam getGenericParam() {
    return findChildByClass(HaxeGenericParam.class);
  }

  @Override
  @NotNull
  public List<HaxeIdentifier> getIdentifierList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeIdentifier.class);
  }

  @Override
  @Nullable
  public HaxeMacroClassList getMacroClassList() {
    return findChildByClass(HaxeMacroClassList.class);
  }

  @Override
  @Nullable
  public HaxePrivateKeyWord getPrivateKeyWord() {
    return findChildByClass(HaxePrivateKeyWord.class);
  }

  @Override
  @NotNull
  public List<HaxeType> getTypeList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeType.class);
  }

  @Override
  @Nullable
  public HaxeTypeOrAnonymous getTypeOrAnonymous() {
    return findChildByClass(HaxeTypeOrAnonymous.class);
  }

}
