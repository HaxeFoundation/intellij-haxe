package com.intellij.plugins.haxe.config;

/** A build target of a lime-family framework; {@code toString()} is its display name. */
public interface FrameworkTarget {

  /** The first command-line flag, the target's name on the framework's command line. */
  String getTargetFlag();

  String[] getFlags();

  /** The Haxe target the framework compiles to for this build target. */
  HaxeTarget getOutputTarget();
}
