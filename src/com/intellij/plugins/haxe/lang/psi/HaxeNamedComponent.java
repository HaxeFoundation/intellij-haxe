package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeNamedComponent extends HaxePsiCompositeElement {
  HaxeComponentName getComponentName();
  @Nullable
  HaxeNamedComponent getTypeComponent();
}
