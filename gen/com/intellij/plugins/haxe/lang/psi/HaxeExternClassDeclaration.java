// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeExternClassDeclaration extends HaxeClass {

  @NotNull
  List<HaxeBitmapMeta> getBitmapMetaList();

  @Nullable
  HaxeComponentName getComponentName();

  @NotNull
  List<HaxeCustomMeta> getCustomMetaList();

  @Nullable
  HaxeExternClassDeclarationBody getExternClassDeclarationBody();

  @NotNull
  List<HaxeFakeEnumMeta> getFakeEnumMetaList();

  @Nullable
  HaxeInheritList getInheritList();

  @NotNull
  List<HaxeMetaMeta> getMetaMetaList();

  @NotNull
  List<HaxeNativeMeta> getNativeMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  HaxeTypeParam getTypeParam();

}
