// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxePpImpl extends HaxePsiCompositeElementImpl implements HaxePp {

  public HaxePpImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxePpElse getPpElse() {
    return findChildByClass(HaxePpElse.class);
  }

  @Override
  @Nullable
  public HaxePpElseIf getPpElseIf() {
    return findChildByClass(HaxePpElseIf.class);
  }

  @Override
  @Nullable
  public HaxePpEnd getPpEnd() {
    return findChildByClass(HaxePpEnd.class);
  }

  @Override
  @Nullable
  public HaxePpError getPpError() {
    return findChildByClass(HaxePpError.class);
  }

  @Override
  @Nullable
  public HaxePpIf getPpIf() {
    return findChildByClass(HaxePpIf.class);
  }

}
