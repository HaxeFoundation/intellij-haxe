package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

/** Whether a container uses its SDK's shared compilation server or a dedicated instance of its own; see {@link HaxeServerAssignment}. */
public enum HaxeServerSharing {
  SHARED("haxe.toolwindow.server.instance.shared"),
  DEDICATED("haxe.toolwindow.server.instance.dedicated");

  private final String presentableKey;

  HaxeServerSharing(String presentableKey) {
    this.presentableKey = presentableKey;
  }

  @NotNull
  public static HaxeServerSharing of(boolean dedicated) {
    return dedicated ? DEDICATED : SHARED;
  }

  public boolean isDedicated() {
    return this == DEDICATED;
  }

  @NotNull
  public String getPresentableText() {
    return HaxeBundle.message(presentableKey);
  }
}
