package com.intellij.plugins.haxe.v2.buildtools.settings;

import org.jetbrains.annotations.NotNull;

/** One user-managed define entry of a container's environment (see {@link HaxeEnvironmentStore}). */
public record EnvironmentDefine(@NotNull String name, @NotNull String value, @NotNull DefineEffect effect) {
}
