package com.intellij.plugins.haxe.lang.psi;

/**
 * @author: Fedor.Korotkov
 */
public interface HaxeNamedComponent extends HaxePsiCompositeElement {
  HaxeComponentName getComponentName();
}
