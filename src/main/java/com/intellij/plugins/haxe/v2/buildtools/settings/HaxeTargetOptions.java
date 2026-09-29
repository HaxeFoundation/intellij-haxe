package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.TargetDefinition;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The selectable target platforms for XML/HXP-based build files (HXML declares its
 * target inside the file and is not selectable). Each build system's list comes
 * from {@link HaxeFrameworkTargetSettings} (Settings | Haxe | Frameworks) —
 * user-configurable, seeded from the built-in defaults — and is offered
 * identically here and in the project wizard. Ids are the configured names.
 */
public final class HaxeTargetOptions {

  public record TargetChoice(@NotNull String id, @NotNull String displayName) {
  }

  private HaxeTargetOptions() {
  }

  public static boolean isTargetSelectable(@NotNull HaxeBuildFileType type) {
    // hxml declares its target in the file; a plain hxp script decides in code
    return type != HaxeBuildFileType.HXML && type != HaxeBuildFileType.HXP_SCRIPT;
  }

  @NotNull
  public static List<TargetChoice> choicesFor(@NotNull HaxeBuildFileType type) {
    return targetsFor(type).stream()
      .map(target -> new TargetChoice(target.name(), target.name()))
      .toList();
  }

  /** Display name for the stored id, falling back to the default target when unset or stale. */
  @NotNull
  public static String displayNameFor(@NotNull HaxeBuildFileType type, @Nullable String targetId) {
    return resolvedTarget(type, targetId).name();
  }

  /** The tool's command-line flag for the stored target id, e.g. "html5" (falls back to the default target). */
  @NotNull
  public static String targetFlagFor(@NotNull HaxeBuildFileType type, @Nullable String targetId) {
    return resolvedTarget(type, targetId).primaryFlag();
  }

  /** All configured flags for the stored id — the target word first, extras (e.g. "-64") after. */
  @NotNull
  public static List<String> targetFlagsFor(@NotNull HaxeBuildFileType type, @Nullable String targetId) {
    return resolvedTarget(type, targetId).flags();
  }

  @NotNull
  public static TargetChoice defaultChoice(@NotNull HaxeBuildFileType type) {
    TargetDefinition target = defaultTarget(type, targetsFor(type));
    return new TargetChoice(target.name(), target.name());
  }

  /** The stored id's configured target, or the default when unset or stale. */
  @NotNull
  private static TargetDefinition resolvedTarget(@NotNull HaxeBuildFileType type, @Nullable String targetId) {
    List<TargetDefinition> targets = targetsFor(type);
    return targets.stream()
      .filter(target -> target.name().equals(targetId))
      .findFirst()
      .orElseGet(() -> defaultTarget(type, targets));
  }

  /**
   * The framework's declared default (each target enum's {@code DEFAULT}) —
   * reordering the configured list does not change it. The first row serves
   * when the default's entry was removed.
   */
  @NotNull
  private static TargetDefinition defaultTarget(@NotNull HaxeBuildFileType type, @NotNull List<TargetDefinition> targets) {
    String preferred = HaxeFrameworkTargetSettings.defaultTargetName(frameworkFor(type));
    return targets.stream()
      .filter(target -> target.name().equals(preferred))
      .findFirst()
      .orElse(targets.getFirst());
  }

  @NotNull
  private static List<TargetDefinition> targetsFor(@NotNull HaxeBuildFileType type) {
    return HaxeFrameworkTargetSettings.getInstance().getTargets(frameworkFor(type));
  }

  @NotNull
  private static Framework frameworkFor(@NotNull HaxeBuildFileType type) {
    return switch (type) {
      case NMML -> Framework.NME;
      case OPENFL -> Framework.OPENFL;
      // hxp scripts build through the lime tool
      default -> Framework.LIME;
    };
  }
}
