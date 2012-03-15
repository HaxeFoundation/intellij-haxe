// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeExternClassDeclaration extends HaxeClass {

  @Nullable
  public HaxeComponentName getComponentName();

  @Nullable
  public HaxeExternClassDeclarationBody getExternClassDeclarationBody();

  @NotNull
  public List<HaxeFakeEnumMeta> getFakeEnumMetaList();

  @Nullable
  public HaxeInheritList getInheritList();

  @NotNull
  public List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  public HaxeTypeParam getTypeParam();

}
