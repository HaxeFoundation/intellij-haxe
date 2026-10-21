package com.intellij.plugins.haxe.v2.buildtools.settings;

import org.jetbrains.annotations.NotNull;

/** One IDE define entry of a build file: set on top of the build's own defines, or removing one of them (see {@link HaxeIdeOverridesStore}). */
public record DefineOverride(@NotNull String name, @NotNull String value, @NotNull DefineEffect effect) {
}
