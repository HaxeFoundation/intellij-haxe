// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeStatementImpl extends HaxePsiCompositeElementImpl implements HaxeStatement {

  public HaxeStatementImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeExpression getExpression() {
    return findChildByClass(HaxeExpression.class);
  }

  @Override
  @Nullable
  public HaxeLocalFunctionDeclaration getLocalFunctionDeclaration() {
    return findChildByClass(HaxeLocalFunctionDeclaration.class);
  }

  @Override
  @Nullable
  public HaxeLocalVarDeclaration getLocalVarDeclaration() {
    return findChildByClass(HaxeLocalVarDeclaration.class);
  }

  @Override
  @Nullable
  public HaxePp getPp() {
    return findChildByClass(HaxePp.class);
  }

}
