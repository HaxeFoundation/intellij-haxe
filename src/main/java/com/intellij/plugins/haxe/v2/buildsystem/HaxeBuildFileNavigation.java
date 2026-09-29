package com.intellij.plugins.haxe.v2.buildsystem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Best-effort text offsets for navigating from tool window rows into build files.
 */
public final class HaxeBuildFileNavigation {

  private HaxeBuildFileNavigation() {
  }

  /**
   * Offset of a define's declaration in the build file content, or 0 when it cannot
   * be located (navigation then lands at the file start).
   */
  public static int findDefineOffset(@NotNull String content, @NotNull HaxeBuildFileType type, @NotNull String defineName) {
    List<String> patterns = switch (type) {
      // hxml flag forms; the bare name fallback below covers values written as -D name=value
      case HXML -> List.of("-D " + defineName, "--define " + defineName);
      // xml attribute form used by haxedef/define/undefine tags
      case OPENFL, LIME, NMML -> List.of("name=\"" + defineName + "\"", "name='" + defineName + "'");
      case HXP_PROJECT, HXP_SCRIPT -> List.of();
    };

    for (String pattern : patterns) {
      int index = content.indexOf(pattern);
      if (index >= 0) {
        return index + pattern.indexOf(defineName);
      }
    }
    return Math.max(content.indexOf(defineName), 0);
  }
}
