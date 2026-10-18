package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How the IDE reaches the compiler for display requests (completion,
 * diagnostics, navigation, types): through the shared {@code --wait}
 * compilation server the builds use, one connection per request, or through
 * a dedicated {@code --server-connect} display process per SDK that keeps
 * one framed connection open. The dedicated process answers without the
 * shared server's per-connection delay and keeps IDE traffic off the build
 * server, at the price of a second haxe process with its own module cache.
 */
public enum HaxeDisplayConnectionMode {
  SHARED_SERVER("shared", "haxe.compiler.display.connection.shared"),
  DEDICATED_PROCESS("dedicated", "haxe.compiler.display.connection.dedicated");

  private final String id;
  private final String presentableKey;

  HaxeDisplayConnectionMode(String id, String presentableKey) {
    this.id = id;
    this.presentableKey = presentableKey;
  }

  /** Stable identifier used for persistence. */
  @NotNull
  public String getId() {
    return id;
  }

  @NotNull
  public String getPresentableText() {
    return HaxeBundle.message(presentableKey);
  }

  @NotNull
  public static HaxeDisplayConnectionMode fromId(@Nullable String id) {
    for (HaxeDisplayConnectionMode mode : values()) {
      if (mode.id.equals(id)) return mode;
    }
    return SHARED_SERVER;
  }
}
