// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeFunctionLiteralImpl extends HaxeExpressionImpl implements HaxeFunctionLiteral {

  public HaxeFunctionLiteralImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeParameterList getParameterList() {
    return findChildByClass(HaxeParameterList.class);
  }

  @Override
  @Nullable
  public HaxeReturnStatementWithoutSemicolon getReturnStatementWithoutSemicolon() {
    return findChildByClass(HaxeReturnStatementWithoutSemicolon.class);
  }

  @Override
  @Nullable
  public HaxeStatement getStatement() {
    return findChildByClass(HaxeStatement.class);
  }

  @Override
  @Nullable
  public HaxeTypeTag getTypeTag() {
    return findChildByClass(HaxeTypeTag.class);
  }

}
