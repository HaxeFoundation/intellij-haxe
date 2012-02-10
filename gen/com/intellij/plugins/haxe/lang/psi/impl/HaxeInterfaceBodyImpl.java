// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeFunctionPrototypeDeclarationWithAttributes;
import com.intellij.plugins.haxe.lang.psi.HaxeInterfaceBody;
import com.intellij.plugins.haxe.lang.psi.HaxePp;
import com.intellij.plugins.haxe.lang.psi.HaxeVarDeclaration;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeInterfaceBodyImpl extends HaxePsiCompositeElementImpl implements HaxeInterfaceBody {

  public HaxeInterfaceBodyImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeFunctionPrototypeDeclarationWithAttributes> getFunctionPrototypeDeclarationWithAttributesList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeFunctionPrototypeDeclarationWithAttributes.class);
  }

  @Override
  @NotNull
  public List<HaxePp> getPpList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxePp.class);
  }

  @Override
  @NotNull
  public List<HaxeVarDeclaration> getVarDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeVarDeclaration.class);
  }
}
