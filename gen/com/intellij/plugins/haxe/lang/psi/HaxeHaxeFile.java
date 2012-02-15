// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeHaxeFile extends HaxePsiCompositeElement {

  @NotNull
  public List<HaxeClassDeclaration> getClassDeclarationList();

  @NotNull
  public List<HaxeEnumDeclaration> getEnumDeclarationList();

  @NotNull
  public List<HaxeInterfaceDeclaration> getInterfaceDeclarationList();

  @NotNull
  public List<HaxePp> getPpList();

  @NotNull
  public List<HaxeStatement> getStatementList();

  @NotNull
  public List<HaxeTypedefDeclaration> getTypedefDeclarationList();

}
