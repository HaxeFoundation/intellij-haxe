// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeAbstractClassDeclaration extends HaxeClass {

  @Nullable
  HaxeClassBody getClassBody();

  @Nullable
  HaxeComponentName getComponentName();

  @Nullable
  HaxeFunctionType getFunctionType();

  @Nullable
  HaxeGenericParam getGenericParam();

  @NotNull
  List<HaxeIdentifier> getIdentifierList();

  @Nullable
  HaxeMacroClassList getMacroClassList();

  @Nullable
  HaxePrivateKeyWord getPrivateKeyWord();

  @NotNull
  List<HaxeType> getTypeList();

  @Nullable
  HaxeTypeOrAnonymous getTypeOrAnonymous();

}
