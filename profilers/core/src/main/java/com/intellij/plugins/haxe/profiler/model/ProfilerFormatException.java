package com.intellij.plugins.haxe.profiler.model;

import java.io.IOException;

/** Malformed or foreign capture input — the message says what did not match. */
public class ProfilerFormatException extends IOException {

  public ProfilerFormatException(String message) {
    super(message);
  }
}
