// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeInterfaceBody extends HaxePsiCompositeElement {

  @NotNull
  public List<HaxeFunctionPrototypeDeclarationWithAttributes> getFunctionPrototypeDeclarationWithAttributesList();

  @NotNull
  public List<HaxePp> getPpList();

  @NotNull
  public List<HaxeVarDeclaration> getVarDeclarationList();

}
