// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeStatement extends HaxePsiCompositeElement {

  @Nullable
  public HaxeExpression getExpression();

  @Nullable
  public HaxeLocalFunctionDeclaration getLocalFunctionDeclaration();

  @Nullable
  public HaxeLocalVarDeclaration getLocalVarDeclaration();

  @Nullable
  public HaxePp getPp();

}
