package com.intellij.plugins.haxe.runner.debugger.dap;

import java.util.Locale;

/**
 * Source-path normalization shared across the DAP debuggers. Windows paths
 * reach us with either separator and in varying case (the IDE, the compiler
 * and the runtime disagree), so matching and comparison go through here.
 */
public final class DapPaths {
  private DapPaths() {
  }

  /** {@code path} with backslashes replaced by forward slashes. */
  public static String toForwardSlashes(String path) {
    return path.replace('\\', '/');
  }

  /**
   * A key for matching two spellings of the same source path — e.g. an IDE-sent
   * breakpoint path against a VM-reported frame source. Separators are unified
   * (forward slashes) and case is folded (haxe source trees do not distinguish
   * files by case), so the result is a lookup key, NOT a usable path.
   */
  public static String toMatchKey(String path) {
    return toForwardSlashes(path).toLowerCase(Locale.ROOT);
  }
}
