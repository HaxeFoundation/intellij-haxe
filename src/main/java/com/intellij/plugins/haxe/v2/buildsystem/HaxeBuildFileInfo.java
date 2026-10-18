package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.util.HaxeModuleVariants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

  /**
   * Whether the build declares Node.js: the hxnodejs library or the
   * {@code nodejs} define. A hint, not proof - a shared hxml can carry it
   * into a browser build too.
   */
  public boolean hasNodeSignal() {
    boolean hasLib = libraries.stream().anyMatch(library -> library.name().equalsIgnoreCase("hxnodejs"));
    return hasLib || defines.stream().anyMatch(define -> define.name().equals("nodejs"));
  }

  /**
   * The custom target the build selects ({@code --custom-target go}), read
   * from the {@code custom_target} and {@code target.name} defines a custom
   * target surfaces as; null for a stock target or none.
   */
  @Nullable
  public String customTargetName() {
    boolean custom = defines.stream().anyMatch(define -> define.name().equals(HaxeModuleVariants.CUSTOM_TARGET_DEFINE));
    if (!custom) return null;
    return defines.stream()
      .filter(define -> define.name().equals(HaxeModuleVariants.TARGET_NAME_DEFINE))
      .map(HaxeDefine::value)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  /// A `-D name=value` compiler define, the name as the build file spells it.
  public record HaxeDefine(@NotNull String name, @Nullable String value) {

    /// The name `#if` finds a define under: the compiler turns every dash into an
    /// underscore (`-D my-flag` defines `my_flag`). Macros accept either spelling
    /// (`Context.defined("my-flag")`); only `#if` needs this one.
    @NotNull
    public static String compilerName(@NotNull String name) {
      return name.replace('-', '_');
    }
  }

  /** A haxelib dependency, optionally pinned to a version (or git ref). */
  public record HaxeLibDependency(@NotNull String name, @Nullable String version) {

    /// The dependency an hxml `-lib` value names: `name`, `name:1.2.0` or `name:git:https://...`.
    @NotNull
    public static HaxeLibDependency parse(@NotNull String value) {
      String[] parts = value.split(":", 2);
      return new HaxeLibDependency(parts[0], parts.length > 1 ? parts[1] : null);
    }

    /// The key two spellings of one library share: haxelib names are case-insensitive
    /// (`OpenFL` and `openfl` are one library), so every lookup keyed by a library name uses this.
    @NotNull
    public static String key(@NotNull String name) {
      return name.toLowerCase(Locale.ROOT);
    }

    /** {@link #key(String)} of this dependency's name. */
    @NotNull
    public String key() {
      return key(name);
    }
  }
}
