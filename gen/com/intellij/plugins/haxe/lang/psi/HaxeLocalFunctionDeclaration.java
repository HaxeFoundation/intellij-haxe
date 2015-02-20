// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeLocalFunctionDeclaration extends HaxeMethod {

  @Nullable
  HaxeBlockStatement getBlockStatement();

  @Nullable
  HaxeComponentName getComponentName();

  @Nullable
  HaxeDoWhileStatement getDoWhileStatement();

  @Nullable
  HaxeExpression getExpression();

  @Nullable
  HaxeForStatement getForStatement();

  @Nullable
  HaxeGenericParam getGenericParam();

  @Nullable
  HaxeIfStatement getIfStatement();

  @Nullable
  HaxeParameterList getParameterList();

  @Nullable
  HaxeReturnStatement getReturnStatement();

  @Nullable
  HaxeThrowStatement getThrowStatement();

  @Nullable
  HaxeTypeTag getTypeTag();

  @Nullable
  HaxeWhileStatement getWhileStatement();

}
