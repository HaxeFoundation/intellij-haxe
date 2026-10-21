package com.intellij.plugins.haxe.v2.buildtools.settings;

/** How a user-managed define applies on top of the build file's own defines. */
public enum DefineEffect {
  /** Adds the define, or overrides the build file's value. */
  SET,
  /** Removes a define the build file declares. */
  REMOVE
}
