package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Best-effort text offsets for navigating from tool window rows into build files.
 */
public final class HaxeBuildFileNavigation {

  private HaxeBuildFileNavigation() {
  }

  /**
   * Offset of a define's declaration in the build file content, or 0 when it cannot
   * be located (navigation then lands at the file start). The name matches in
   * either spelling: a row may carry the compiler's {@code my_flag} for a
   * file that writes {@code -D my-flag}.
   */
  public static int findDefineOffset(@NotNull String content, @NotNull HaxeBuildFileType type, @NotNull String defineName) {
    Set<String> spellings = new LinkedHashSet<>(List.of(defineName, HaxeDefine.compilerName(defineName), defineName.replace('_', '-')));
    for (String spelling : spellings) {
      for (String pattern : declarationPatterns(type, spelling)) {
        int index = content.indexOf(pattern);
        if (index >= 0) {
          return index + pattern.indexOf(spelling);
        }
      }
    }
    // the bare name covers values written as -D name=value
    for (String spelling : spellings) {
      int index = content.indexOf(spelling);
      if (index >= 0) return index;
    }
    return 0;
  }

  @NotNull
  private static List<String> declarationPatterns(@NotNull HaxeBuildFileType type, @NotNull String defineName) {
    return switch (type) {
      case HXML -> List.of("-D " + defineName, "--define " + defineName);
      // xml attribute form used by haxedef/define/undefine tags
      case OPENFL, LIME, NMML -> List.of("name=\"" + defineName + "\"", "name='" + defineName + "'");
      case HXP_PROJECT, HXP_SCRIPT -> List.of();
    };
  }
}
