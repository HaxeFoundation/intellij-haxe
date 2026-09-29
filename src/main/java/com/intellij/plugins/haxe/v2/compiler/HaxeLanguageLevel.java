package com.intellij.plugins.haxe.v2.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Haxe language level, based on released Haxe compiler versions (major.minor).
 * Used by the Haxe compiler settings page to pick a per-project / per-module level.
 */
public enum HaxeLanguageLevel {
  HAXE_3_4(3, 4),
  HAXE_4_0(4, 0),
  HAXE_4_1(4, 1),
  HAXE_4_2(4, 2),
  HAXE_4_3(4, 3),
  HAXE_5_0(5, 0);

  // the leading major.minor of a version (4.3.7, 5.0.0-rc.1): groups major, minor
  private static final Pattern VERSION_PATTERN = Pattern.compile("^\\s*(\\d+)\\.(\\d+)");

  private final int major;
  private final int minor;

  HaxeLanguageLevel(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  /** Stable identifier used for persistence, e.g. {@code "4.3"}. */
  @NotNull
  public String getVersionString() {
    return major + "." + minor;
  }

  @NotNull
  public String getPresentableText() {
    return getVersionString();
  }

  public boolean isAtLeast(@NotNull HaxeLanguageLevel other) {
    return ordinal() >= other.ordinal();
  }

  /**
   * The level immediately below this one. The oldest known level has nothing
   * below it and returns itself, so callers always get a usable level.
   */
  @NotNull
  public HaxeLanguageLevel previous() {
    int ordinal = ordinal();
    return ordinal == 0 ? this : values()[ordinal - 1];
  }

  @NotNull
  public static HaxeLanguageLevel latest() {
    HaxeLanguageLevel[] values = values();
    return values[values.length - 1];
  }

  /**
   * Resolves a language level from a version string such as {@code "4.3"}, {@code "4.3.7"}
   * or {@code "5.0.0-rc.1"}. Versions between known releases resolve to the closest lower
   * known level (e.g. {@code "4.4"} resolves to 4.3). Returns {@code null} for unparsable
   * versions or versions older than the oldest supported level.
   */
  @Nullable
  public static HaxeLanguageLevel fromVersionString(@Nullable String version) {
    if (version == null) return null;
    Matcher matcher = VERSION_PATTERN.matcher(version);
    if (!matcher.find()) return null;

    int major = Integer.parseInt(matcher.group(1));
    int minor = Integer.parseInt(matcher.group(2));

    HaxeLanguageLevel best = null;
    for (HaxeLanguageLevel level : values()) {
      boolean atMostRequested = level.major < major || (level.major == major && level.minor <= minor);
      if (atMostRequested) {
        best = level;
      }
    }
    return best;
  }
}
