// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeFunctionLiteral extends HaxeExpression {

  @Nullable
  HaxeBlockStatement getBlockStatement();

  @Nullable
  HaxeExpression getExpression();

  @Nullable
  HaxeParameterList getParameterList();

  @Nullable
  HaxeReturnStatementWithoutSemicolon getReturnStatementWithoutSemicolon();

  @Nullable
  HaxeTypeTag getTypeTag();

}
