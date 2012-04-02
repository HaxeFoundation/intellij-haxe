package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeNamedComponent extends HaxePsiCompositeElement {
  @Nullable
  HaxeComponentName getComponentName();

  @Nullable
  HaxeNamedComponent getTypeComponent();

  boolean isPublic();

  boolean isStatic();

  boolean isOverride();
}
