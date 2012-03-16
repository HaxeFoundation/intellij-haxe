// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeExternClassDeclarationBodyImpl extends HaxePsiCompositeElementImpl implements HaxeExternClassDeclarationBody {

  public HaxeExternClassDeclarationBodyImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeExternFunctionDeclaration> getExternFunctionDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeExternFunctionDeclaration.class);
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
