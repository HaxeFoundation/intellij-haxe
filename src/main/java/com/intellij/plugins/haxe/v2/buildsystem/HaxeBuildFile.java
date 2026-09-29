package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * A build/project file discovered in a module, shown as a tree node in the Haxe tool window.
 */
public record HaxeBuildFile(@NotNull VirtualFile file, @NotNull HaxeBuildFileType type) {
}
