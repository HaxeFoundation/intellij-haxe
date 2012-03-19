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

public class HaxeTypedefDeclarationImpl extends AbstractHaxePsiClass implements HaxeTypedefDeclaration {

  public HaxeTypedefDeclarationImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeAnonymousType getAnonymousType() {
    return findChildByClass(HaxeAnonymousType.class);
  }

  @Override
  @Nullable
  public HaxeComponentName getComponentName() {
    return findChildByClass(HaxeComponentName.class);
  }

  @Override
  @Nullable
  public HaxeExternOrPrivate getExternOrPrivate() {
    return findChildByClass(HaxeExternOrPrivate.class);
  }

  @Override
  @Nullable
  public HaxeType getType() {
    return findChildByClass(HaxeType.class);
  }

  @Override
  @Nullable
  public HaxeTypeParam getTypeParam() {
    return findChildByClass(HaxeTypeParam.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitTypedefDeclaration(this);
    else super.accept(visitor);
  }

}
