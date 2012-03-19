// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeFunctionPrototypeDeclarationWithAttributes extends HaxeComponent {

  @Nullable
  HaxeComponentName getComponentName();

  @Nullable
  HaxeDeclarationAttributeList getDeclarationAttributeList();

  @Nullable
  HaxeParameterList getParameterList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  HaxeTypeParam getTypeParam();

  @Nullable
  HaxeTypeTag getTypeTag();

}
