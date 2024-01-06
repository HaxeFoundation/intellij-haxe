package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeImportAlias;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeAliasModel implements HaxeModel {
  String aliasName;
  HaxeImportAlias aliasPsi;

  public HaxeAliasModel(HaxeImportAlias importAlias) {
    aliasPsi = importAlias;
    aliasName = importAlias.getIdentifier().getText();
  }

  public String getName() {
    return aliasName;
  }

  @NotNull
  public PsiElement getBasePsi() {
    return aliasPsi;
  }

  @Nullable
  public HaxeExposableModel getExhibitor() {
    return null;
  }

  @Nullable
  public FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }

}
