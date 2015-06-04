// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeTypedefDeclaration extends HaxeClass {

  @NotNull
  HaxeComponentName getComponentName();

  @Nullable
  HaxeExternKeyWord getExternKeyWord();

  @Nullable
  HaxeFunctionType getFunctionType();

  @Nullable
  HaxeGenericParam getGenericParam();

  @Nullable
  HaxeMacroClassList getMacroClassList();

  @Nullable
  HaxePrivateKeyWord getPrivateKeyWord();

  @Nullable
  HaxeTypeOrAnonymous getTypeOrAnonymous();

}
