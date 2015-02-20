// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeExternClassDeclaration extends HaxeClass {

  @NotNull
  HaxeComponentName getComponentName();

  @Nullable
  HaxeExternClassDeclarationBody getExternClassDeclarationBody();

  @NotNull
  List<HaxeExternOrPrivate> getExternOrPrivateList();

  @Nullable
  HaxeGenericParam getGenericParam();

  @Nullable
  HaxeInheritList getInheritList();

  @Nullable
  HaxeMacroClassList getMacroClassList();

}
