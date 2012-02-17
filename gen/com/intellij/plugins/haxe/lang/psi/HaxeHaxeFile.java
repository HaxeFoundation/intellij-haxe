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
  public List<HaxeImportStatement> getImportStatementList();

  @NotNull
  public List<HaxeInterfaceDeclaration> getInterfaceDeclarationList();

  @Nullable
  public HaxePackageStatement getPackageStatement();

  @NotNull
  public List<HaxePp> getPpList();

  @NotNull
  public List<HaxeTypedefDeclaration> getTypedefDeclarationList();

}
