package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import org.jetbrains.annotations.NotNull;

/**
 * The two generations of haxe server flags. Haxe 3 listens with
 * {@code --wait}, on a port or on stdio. Haxe 4 renamed that to
 * {@code --server-listen} (haxe 5 drops the old spelling) and added
 * {@code --server-connect}, which dials the IDE. Each generation has a
 * connection mode of its own in the compiler settings.
 */
public enum HaxeServerGeneration {
  HAXE_3("--wait", "haxe.compiler.server.generation.haxe3"),
  HAXE_4("--server-listen", "haxe.compiler.server.generation.haxe4");

  private final String listenFlag;
  private final String presentableKey;

  HaxeServerGeneration(String listenFlag, String presentableKey) {
    this.listenFlag = listenFlag;
    this.presentableKey = presentableKey;
  }

  /** The generation of a haxe of the level; the new flags came with 4.0. */
  @NotNull
  public static HaxeServerGeneration of(@NotNull HaxeLanguageLevel level) {
    return level.isAtLeast(HaxeLanguageLevel.HAXE_4_0) ? HAXE_4 : HAXE_3;
  }

  /** The flag a server starts with to listen, on a port or on stdio. */
  @NotNull
  public String listenFlag() {
    return listenFlag;
  }

  @NotNull
  public String getPresentableText() {
    return HaxeBundle.message(presentableKey);
  }
}
