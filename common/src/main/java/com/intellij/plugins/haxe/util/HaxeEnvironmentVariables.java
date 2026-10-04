package com.intellij.plugins.haxe.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every Haxe-related environment variable the plugin reads, and the path-list rule the Haxe
 * toolchain applies to the ones that hold several folders. Whether a value names a file or a
 * folder is decided where it is used.
 */
public final class HaxeEnvironmentVariables {
  /** The Haxe install folder; set by the Windows installer. */
  public static final String HAXEPATH = "HAXEPATH";

  /** Path list of standard-library folders, read by the compiler: every entry becomes a class path, with no fallback when set. */
  public static final String HAXE_STD_PATH = "HAXE_STD_PATH";

  /** Path list Neko searches for modules (.n) and libraries (.ndll); Homebrew sets it to {@code <prefix>/lib/neko}. */
  public static final String NEKOPATH = "NEKOPATH";

  /** The Neko install folder; set by the Windows installer. */
  public static final String NEKO_INSTPATH = "NEKO_INSTPATH";

  /** The hl executable or its folder; this plugin's own lookup convention. */
  public static final String HASHLINK_BIN = "HASHLINK_BIN";

  /** The hl executable or its folder; this plugin's own lookup convention. */
  public static final String HASHLINK = "HASHLINK";

  /** The hl executable or its folder; this plugin's own lookup convention. */
  public static final String HASHLINKPATH = "HASHLINKPATH";

  /** The AIR SDK folder, as Lime reads it. */
  public static final String AIR_SDK = "AIR_SDK";

  private HaxeEnvironmentVariables() {
  }

  /** The variable's value in the running IDE's environment; {@code null} when it is unset or blank. */
  @Nullable
  public static String value(@NotNull String name) {
    String value = System.getenv(name);
    return value != null && !value.isBlank() ? value : null;
  }

  /** The entries of a path-list variable in the running IDE's environment, in order; empty when it is unset. */
  @NotNull
  public static List<String> pathList(@NotNull String name) {
    return parsePathList(value(name));
  }

  /**
   * The entries of a path-list value as the Haxe compiler ({@code get_std_class_paths}) and Neko
   * ({@code init_path}) read it: split on both {@code ;} and {@code :} on every OS, where a
   * one-letter segment is a Windows drive that the {@code :} split tore off and is glued back onto
   * the segment after it ({@code C:\haxe\std;D:\libs} is two entries). Blank entries are dropped;
   * {@code null} gives an empty list.
   */
  @NotNull
  public static List<String> parsePathList(@Nullable String value) {
    if (value == null) return Collections.emptyList();
    // a ';' or ':' separator; -1 keeps the empty segments a trailing separator produces
    String[] segments = value.split("[;:]", -1);
    List<String> entries = new ArrayList<>();
    for (int i = 0; i < segments.length; i++) {
      String entry = segments[i];
      if (isDriveLetter(entry) && i + 1 < segments.length) {
        entry = entry + ":" + segments[++i];
      }
      if (!entry.isBlank()) entries.add(entry);
    }
    return entries;
  }

  private static boolean isDriveLetter(String segment) {
    return segment.length() == 1 && Character.isLetter(segment.charAt(0));
  }
}
