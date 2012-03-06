// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeVarDeclarationPart extends HaxeComponent {

  @NotNull
  public HaxeComponentName getComponentName();

  @Nullable
  public HaxePropertyDeclaration getPropertyDeclaration();

  @Nullable
  public HaxeTypeTag getTypeTag();

  @Nullable
  public HaxeVarInit getVarInit();

}
