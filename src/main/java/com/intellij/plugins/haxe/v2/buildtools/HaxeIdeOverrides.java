package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeIdeOverridesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How code analysis applies a build file's IDE overrides
 * ({@link HaxeIdeOverridesStore}). The define entries always apply; the
 * custom target only to a build that selects no target of its own, since a
 * second target fails the compile.
 */
public final class HaxeIdeOverrides {

  private HaxeIdeOverrides() {
  }

  /** The custom target analysis uses for the build, or null. Call in a read action. */
  @Nullable
  public static String effectiveCustomTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    String stored = HaxeIdeOverridesStore.getInstance(project).getCustomTarget(buildFile.file().getPath());
    if (stored == null) return null;
    return customTargetFor(stored, buildSelectsTarget(project, buildFile));
  }

  /** The stored custom target unless the build selects its own target. */
  @Nullable
  static String customTargetFor(@Nullable String stored, boolean buildSelectsTarget) {
    return buildSelectsTarget ? null : stored;
  }

  /**
   * Whether the build selects a target itself: always for the lime family
   * and nme, for an hxml when its selected section, includes merged, names
   * one. Call in a read action.
   */
  public static boolean buildSelectsTarget(@NotNull Project project, @NotNull HaxeBuildFile buildFile) {
    if (buildFile.type() != HaxeBuildFileType.HXML) return true;
    String content = HaxeBuildSections.selectedSectionContent(project, buildFile);
    return content != null && HxmlFileParser.declaresAnyTarget(content);
  }
}
