package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.NotNull;

/**
 * Interface to make it possible to collect both `extends` and `implements` expressions using findChildren etc.
 */
public interface HaxeInheritDeclaration extends HaxePsiCompositeElement {

    @NotNull
    HaxeType getType();

}
