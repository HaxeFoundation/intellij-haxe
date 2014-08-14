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

public class HaxeExternClassDeclarationImpl extends AbstractHaxePsiClass implements HaxeExternClassDeclaration {

  public HaxeExternClassDeclarationImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitExternClassDeclaration(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HaxeAutoBuildMacro> getAutoBuildMacroList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeAutoBuildMacro.class);
  }

  @Override
  @NotNull
  public List<HaxeBitmapMeta> getBitmapMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeBitmapMeta.class);
  }

  @Override
  @NotNull
  public List<HaxeBuildMacro> getBuildMacroList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeBuildMacro.class);
  }

  @Override
  @NotNull
  public HaxeComponentName getComponentName() {
    return findNotNullChildByClass(HaxeComponentName.class);
  }

  @Override
  @NotNull
  public List<HaxeCustomMeta> getCustomMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeCustomMeta.class);
  }

  @Override
  @Nullable
  public HaxeExternClassDeclarationBody getExternClassDeclarationBody() {
    return findChildByClass(HaxeExternClassDeclarationBody.class);
  }

  @Override
  @NotNull
  public List<HaxeExternOrPrivate> getExternOrPrivateList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeExternOrPrivate.class);
  }

  @Override
  @NotNull
  public List<HaxeFakeEnumMeta> getFakeEnumMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeFakeEnumMeta.class);
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
  @NotNull
  public List<HaxeJsRequireMeta> getJsRequireMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeJsRequireMeta.class);
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

}
