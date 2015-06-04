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

public class HaxeEnumDeclarationImpl extends AbstractHaxePsiClass implements HaxeEnumDeclaration {

  public HaxeEnumDeclarationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitEnumDeclaration(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public HaxeComponentName getComponentName() {
    return findChildByClass(HaxeComponentName.class);
  }

  @Override
  @Nullable
  public HaxeEnumBody getEnumBody() {
    return findChildByClass(HaxeEnumBody.class);
  }

  @Override
  @Nullable
  public HaxeExternKeyWord getExternKeyWord() {
    return findChildByClass(HaxeExternKeyWord.class);
  }

  @Override
  @Nullable
  public HaxeGenericParam getGenericParam() {
    return findChildByClass(HaxeGenericParam.class);
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

}
