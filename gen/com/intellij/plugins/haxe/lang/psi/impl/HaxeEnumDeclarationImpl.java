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

  @Override
  @NotNull
  public List<HaxeBitmapMeta> getBitmapMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeBitmapMeta.class);
  }

  @Override
  @Nullable
  public HaxeComponentName getComponentName() {
    return findChildByClass(HaxeComponentName.class);
  }

  @Override
  @NotNull
  public List<HaxeCustomMeta> getCustomMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeCustomMeta.class);
  }

  @Override
  @Nullable
  public HaxeEnumBody getEnumBody() {
    return findChildByClass(HaxeEnumBody.class);
  }

  @Override
  @Nullable
  public HaxeExternOrPrivate getExternOrPrivate() {
    return findChildByClass(HaxeExternOrPrivate.class);
  }

  @Override
  @NotNull
  public List<HaxeFakeEnumMeta> getFakeEnumMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeFakeEnumMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeMetaMeta> getMetaMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeMetaMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeNativeMeta> getNativeMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeNativeMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeNsMeta> getNsMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeNsMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeRequireMeta> getRequireMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeRequireMeta.class);
  }

  @Override
  @Nullable
  public HaxeTypeParam getTypeParam() {
    return findChildByClass(HaxeTypeParam.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitEnumDeclaration(this);
    else super.accept(visitor);
  }

}
