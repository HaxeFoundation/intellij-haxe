// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface HaxeInterfaceDeclaration extends HaxeComponent {

  @Nullable
  public HaxeComponentName getComponentName();

  @Nullable
  public HaxeExternOrPrivate getExternOrPrivate();

  @NotNull
  public List<HaxeFakeEnumMeta> getFakeEnumMetaList();

  @Nullable
  public HaxeInheritList getInheritList();

  @Nullable
  public HaxeInterfaceBody getInterfaceBody();

  @NotNull
  public List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  public HaxeTypeParam getTypeParam();
}
