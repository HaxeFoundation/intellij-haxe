// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeInterfaceDeclarationImpl extends AbstractHaxeNamedComponent implements HaxeInterfaceDeclaration {

  public HaxeInterfaceDeclarationImpl(ASTNode node) {
    super(node);
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
  @NotNull
  public List<HaxeFakeEnumMeta> getFakeEnumMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeFakeEnumMeta.class);
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
  @NotNull
  public List<HaxeRequireMeta> getRequireMetaList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeRequireMeta.class);
  }

  @Override
  @Nullable
  public HaxeTypeParam getTypeParam() {
    return findChildByClass(HaxeTypeParam.class);
  }
}
