package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.config.HaxeTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// What a Haxe build/project file declares: the compilation target (HXML only —
/// XML-based projects choose their target in the UI), the target's output argument
/// (e.g. `bin/app.hl`), the defines it sets, the haxelib dependencies it
/// pulls in and its class paths (relative ones resolve against the build file's
/// directory — for `lime display` output these are the fully evaluated dependency
/// roots, as the tool emits `-cp` instead of `-lib`).
public record HaxeBuildFileInfo(@Nullable HaxeTarget target,
                                @Nullable String targetOutput,
                                @NotNull List<HaxeDefine> defines,
                                @NotNull List<HaxeLibDependency> libraries,
                                @NotNull List<String> classpaths) {

  public static final HaxeBuildFileInfo EMPTY = new HaxeBuildFileInfo(null, null, List.of(), List.of(), List.of());

  @NotNull
  public HaxeBuildFileInfo withTarget(@Nullable HaxeTarget target, @Nullable String targetOutput) {
    return new HaxeBuildFileInfo(target, targetOutput, defines, libraries, classpaths);
  }

  @NotNull
  public HaxeBuildFileInfo withLibraries(@NotNull List<HaxeLibDependency> libraries) {
    return new HaxeBuildFileInfo(target, targetOutput, defines, libraries, classpaths);
  }

  /// A `-D name=value` compiler define.
  public record HaxeDefine(@NotNull String name, @Nullable String value) {
  }

  /** A haxelib dependency, optionally pinned to a version (or git ref). */
  public record HaxeLibDependency(@NotNull String name, @Nullable String version) {
  }
}
