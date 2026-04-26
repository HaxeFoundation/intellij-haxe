package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.HaxeImportAlias;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeImportStatementImpl;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeImportStub;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeAliasModel implements HaxeModel {
  String aliasName;
  HaxeImportAlias aliasPsi;

  public HaxeAliasModel(HaxeImportAlias importAlias) {
    aliasPsi = importAlias;
    aliasName = getAliasName(importAlias);
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

  @Override
  public boolean isValid() {
    return aliasPsi.isValid();
  }


  private String getAliasName(HaxeImportAlias importAlias) {
    HaxeImportStatementImpl type = PsiTreeUtil.getStubOrPsiParentOfType(aliasPsi, HaxeImportStatementImpl.class);
    if (type != null) {
      HaxeImportStub stub = type.getStub();
      if (stub != null) {
        return stub.getAlias();
      }
    }
    // fallback
    return importAlias.getIdentifier().getText();
  }

}
