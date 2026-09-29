package com.intellij.plugins.haxe.util;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Injects compiler defines for unit tests through
 * {@link HaxeConditionalExpression#DEFINES_KEY}, keeping the comma-separated
 * DSL the suites write ({@code "cpp,js=false,haxe-ver=\"3.2\""}). A dashed
 * name is stored under its underscore form: the compiler treats the two as
 * the same define, and a conditional expression can only spell the
 * underscore one (a dash is a subtraction operator there).
 */
public final class HaxeTestDefines {

  private HaxeTestDefines() {
  }

  /** Replaces the project's test defines; null clears them. */
  public static void set(@NotNull Project project, @Nullable String defines) {
    project.putUserData(HaxeConditionalExpression.DEFINES_KEY, defines == null ? null : parse(defines));
  }

  @NotNull
  private static Map<String, String> parse(@NotNull String defines) {
    Map<String, String> map = new HashMap<>();
    for (String entry : defines.split(",")) {
      String[] split = entry.split("=", 2);
      map.put(split[0].replace('-', '_'), split.length > 1 ? split[1] : "");
    }
    return map;
  }
}
