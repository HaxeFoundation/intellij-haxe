package com.intellij.plugins.haxe.config;

import org.jetbrains.annotations.Nullable;

/** A build target of a lime-family framework; {@code toString()} is its display name. */
public interface FrameworkTarget {

  /** The output target of the listed target whose flags include {@code flag}, or null when none does. */
  @Nullable
  static HaxeTarget outputTargetFor(FrameworkTarget[] targets, String flag) {
    for (FrameworkTarget target : targets) {
      for (String candidate : target.getFlags()) {
        if (candidate.equals(flag)) return target.getOutputTarget();
      }
    }
    return null;
  }

  /** The first command-line flag, the target's name on the framework's command line. */
  String getTargetFlag();

  String[] getFlags();

  /** The Haxe target the framework compiles to for this build target. */
  HaxeTarget getOutputTarget();
}
