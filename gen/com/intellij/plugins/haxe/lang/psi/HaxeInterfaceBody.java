// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface HaxeInterfaceBody extends HaxePsiCompositeElement {

  @NotNull
  public List<HaxeFunctionPrototypeDeclarationWithAttributes> getFunctionPrototypeDeclarationWithAttributesList();

  @NotNull
  public List<HaxePp> getPpList();

  @NotNull
  public List<HaxeVarDeclaration> getVarDeclarationList();
}
