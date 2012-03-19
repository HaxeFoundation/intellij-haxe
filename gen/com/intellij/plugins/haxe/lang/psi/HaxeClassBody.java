// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeClassBody extends HaxePsiCompositeElement {

  @NotNull
  List<HaxeFunctionDeclarationWithAttributes> getFunctionDeclarationWithAttributesList();

  @NotNull
  List<HaxePp> getPpList();

  @NotNull
  List<HaxeVarDeclaration> getVarDeclarationList();

}
