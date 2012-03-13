// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeExternClassDeclarationBody;
import com.intellij.plugins.haxe.lang.psi.HaxeExternClassDeclarationBodyPart;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeExternClassDeclarationBodyImpl extends HaxePsiCompositeElementImpl implements HaxeExternClassDeclarationBody {

  public HaxeExternClassDeclarationBodyImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeExternClassDeclarationBodyPart> getExternClassDeclarationBodyPartList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeExternClassDeclarationBodyPart.class);
  }
}
