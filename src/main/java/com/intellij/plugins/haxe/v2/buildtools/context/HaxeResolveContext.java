package com.intellij.plugins.haxe.v2.buildtools.context;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import org.jetbrains.annotations.NotNull;

/**
 * The build a file is analysed under: one of the project's known build
 * files, with the target or section that build file is currently set to
 * (those selections live per build file in their own stores). A context
 * names only the build; the define context service derives its defines.
 */
// TODO: multiverse (the platform's per-file code-insight contexts): this record becomes the plugin's CodeInsightContext
public record HaxeResolveContext(@NotNull HaxeBuildFile buildFile) {

  /** The build file's path, the key every per-build-file store uses. */
  @NotNull
  public String path() {
    return buildFile.file().getPath();
  }
}
