package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.config.HaxeTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Compiler additions that make a target's output debuggable, applied on top of the action's normal compile. */
public final class HaxeDebugAdditions {

  /** The haxelib id of the in-debuggee DAP server hxcpp debug builds compile in. */
  public static final String HXCPP_DEBUG_SERVER_LIB = "intellij-hxcpp-debug-server";

  private HaxeDebugAdditions() {
  }

  /** Extra compiler arguments for a debuggable build, or null when the target has no debugger support yet. */
  @Nullable
  public static List<String> forTarget(@NotNull HaxeTarget target) {
    return switch (target) {
      // HL: debug info in the bytecode; JS: -debug emits the .js.map the
      // browser adapters need; FLASH: -debug embeds the fdb line tables
      case HL, JAVA_SCRIPT, FLASH -> List.of("-debug");
      // hxcpp: line tables plus the in-debuggee DAP server compiled in - it
      // connects out at startup guided by the HXCPP_DEBUG_HOST/PORT env vars
      case CPP -> List.of("-debug", "-lib", HXCPP_DEBUG_SERVER_LIB);
      // TODO: eval (INTERP) has no debug additions yet
      default -> null;
    };
  }
}
