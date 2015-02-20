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

public class HaxeInterfaceDeclarationImpl extends AbstractHaxePsiClass implements HaxeInterfaceDeclaration {

  public HaxeInterfaceDeclarationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitInterfaceDeclaration(this);
    else super.accept(visitor);
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
  public HaxeGenericParam getGenericParam() {
    return findChildByClass(HaxeGenericParam.class);
  }

  @Override
  @Nullable
  public HaxeInheritList getInheritList() {
    return findChildByClass(HaxeInheritList.class);
  }

  @Override
  @Nullable
  public HaxeInterfaceBody getInterfaceBody() {
    return findChildByClass(HaxeInterfaceBody.class);
  }

  @Override
  @Nullable
  public HaxeMacroClassList getMacroClassList() {
    return findChildByClass(HaxeMacroClassList.class);
  }

}
