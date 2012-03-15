// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeFunctionType extends HaxePsiCompositeElement {

  @NotNull
  public List<HaxeAnonymousType> getAnonymousTypeList();

  @Nullable
  public HaxeFunctionType getFunctionType();

  @NotNull
  public List<HaxeType> getTypeList();

}
