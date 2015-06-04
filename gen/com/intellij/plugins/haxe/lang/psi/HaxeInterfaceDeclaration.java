// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeInterfaceDeclaration extends HaxeClass {

  @Nullable
  HaxeComponentName getComponentName();

  @Nullable
  HaxeGenericParam getGenericParam();

  @Nullable
  HaxeInheritList getInheritList();

  @Nullable
  HaxeInterfaceBody getInterfaceBody();

  @Nullable
  HaxeMacroClassList getMacroClassList();

  @Nullable
  HaxePrivateKeyWord getPrivateKeyWord();

}
