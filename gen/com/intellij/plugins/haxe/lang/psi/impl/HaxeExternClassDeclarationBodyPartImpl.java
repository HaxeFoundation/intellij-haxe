// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeExternClassDeclarationBodyPartImpl extends HaxePsiCompositeElementImpl implements HaxeExternClassDeclarationBodyPart {

  public HaxeExternClassDeclarationBodyPartImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeExternFunctionDeclaration getExternFunctionDeclaration() {
    return findChildByClass(HaxeExternFunctionDeclaration.class);
  }

  @Override
  @Nullable
  public HaxePp getPp() {
    return findChildByClass(HaxePp.class);
  }

  @Override
  @Nullable
  public HaxeVarDeclaration getVarDeclaration() {
    return findChildByClass(HaxeVarDeclaration.class);
  }

}
