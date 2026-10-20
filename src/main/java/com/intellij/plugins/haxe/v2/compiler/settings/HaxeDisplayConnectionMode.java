package com.intellij.plugins.haxe.v2.compiler.settings;

import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * How the IDE reaches the compiler for display requests (completion,
 * diagnostics, navigation, types), chosen per {@link HaxeServerGeneration}.
 * The shared server is the build's compilation server listening on a port:
 * every request takes a connection of its own and waits behind a running
 * build. The dedicated process is a second haxe per SDK with one framed
 * channel kept open, {@code --server-connect} from haxe 4 on and
 * {@code --wait stdio} on haxe 3: it answers without the listening server's
 * per-connection delay and keeps IDE traffic off the build server, at the
 * price of a module cache of its own. It is the default.
 */
public enum HaxeDisplayConnectionMode {
  SHARED_SERVER("shared"),
  DEDICATED_PROCESS("dedicated");

  /** The mode of a project that never chose one. */
  public static final HaxeDisplayConnectionMode DEFAULT = DEDICATED_PROCESS;

  private final String id;

  HaxeDisplayConnectionMode(String id) {
    this.id = id;
  }

  /** Stable identifier used for persistence. */
  @NotNull
  public String getId() {
    return id;
  }

  /** The mode as the flags a server of the generation starts with. */
  @NotNull
  public String getPresentableText(@NotNull HaxeServerGeneration generation) {
    String key = switch (generation) {
      case HAXE_3 -> this == SHARED_SERVER ? "haxe.compiler.connection.wait.port" : "haxe.compiler.connection.wait.stdio";
      case HAXE_4 -> this == SHARED_SERVER ? "haxe.compiler.connection.server.listen" : "haxe.compiler.connection.server.connect";
    };
    return HaxeBundle.message(key);
  }

  @NotNull
  public static HaxeDisplayConnectionMode fromId(@Nullable String id) {
    for (HaxeDisplayConnectionMode mode : values()) {
      if (mode.id.equals(id)) return mode;
    }
    return DEFAULT;
  }
}
