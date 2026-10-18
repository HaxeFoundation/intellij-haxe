package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.io.OSAgnosticPathUtil;
import com.intellij.util.PathUtilRt;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Paths named by text the IDE did not write: tool output (haxelib, nme, the
 * compiler), debugger and profiler frames, build-file values. Such text may
 * be a notice instead of a path (a newer haxelib prints its warnings among
 * the classpaths), may hold characters the file system rejects, or may name a
 * network share. A network path is refused: merely looking at one makes the
 * OS connect to that host and, on Windows, offer the user's credentials to it.
 */
public final class HaxeExternalPaths {

  private HaxeExternalPaths() {
  }

  /** The path the text names, relative ones included; null for blank text, a control character, a network path or text the file system cannot parse. */
  @Nullable
  public static Path pathOf(@Nullable String text) {
    if (text == null || text.isBlank()) return null;
    if (hasControlCharacter(text) || PathUtilRt.startsWithSeparatorSeparator(text)) return null;
    try {
      return Path.of(text);
    }
    catch (InvalidPathException e) {
      return null;
    }
  }

  /** The absolute path the text names, a drive path counting on every OS ({@code C:/lib}); null for a relative path and for anything {@link #pathOf} refuses. */
  @Nullable
  public static Path absolutePathOf(@Nullable String text) {
    Path path = pathOf(text);
    return path != null && OSAgnosticPathUtil.isAbsolute(text) ? path : null;
  }

  private static boolean hasControlCharacter(String text) {
    return text.chars().anyMatch(Character::isISOControl);
  }
}
