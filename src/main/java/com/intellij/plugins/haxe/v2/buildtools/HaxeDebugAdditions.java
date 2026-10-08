package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.config.HaxeTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Compiler additions that make a target's output debuggable, applied on top of the action's normal compile. */
public final class HaxeDebugAdditions {

  /** The haxelib id of the in-debuggee DAP server the HXCPP (IntelliJ) debugger attaches to. */
  public static final String HXCPP_DEBUG_SERVER_LIB = "intellij-hxcpp-debug-server";
  /** The haxelib id of vshaxe's in-debuggee debug server the HXCPP (vshaxe) debugger attaches to. */
  public static final String VSHAXE_DEBUG_SERVER_LIB = "hxcpp-debug-server";

  /**
   * The debugger a launch attaches, derived from its run configuration. Every
   * lane compiles with {@code -debug}; the debugger decides which hxcpp server
   * lib a C++ build compiles in and whether a swf carries the Flash debugger tag.
   */
  public enum Debugger {
    /**
     * HXCPP Application (IntelliJ) and every launch naming no other debugger
     * (HashLink, browser, AIR, tests): a C++ build gets the IntelliJ server,
     * a swf stays untagged.
     */
    DEFAULT(HXCPP_DEBUG_SERVER_LIB, false),
    HXCPP_VSHAXE(VSHAXE_DEBUG_SERVER_LIB, false),
    /** The old {@code debugger} haxelib stays in the user's build file; nothing beyond -debug is added. */
    HXCPP_LEGACY(null, false),
    /**
     * The standalone Flash Player: fdb attaches only to a swf carrying the
     * EnableDebugger2 tag, which haxe writes with {@code -D fdb} on top of
     * -debug's line tables (file names then become absolute). AIR under adl
     * needs no tag, and a tagged swf waits silently for a debugger under
     * adl's debug mode - so the tag is tied to this debugger alone.
     */
    FLASH_PLAYER(null, true);

    private final String hxcppServerLib;
    private final boolean flashDebuggerTag;

    Debugger(@Nullable String hxcppServerLib, boolean flashDebuggerTag) {
      this.hxcppServerLib = hxcppServerLib;
      this.flashDebuggerTag = flashDebuggerTag;
    }

    /** The haxelib a C++ debug build compiles in for this debugger, or null when the build brings its own. */
    @Nullable
    public String hxcppServerLib() {
      return hxcppServerLib;
    }

    /** Whether a swf gets the Flash debugger tag ({@code -D fdb}). */
    public boolean flashDebuggerTag() {
      return flashDebuggerTag;
    }
  }

  private HaxeDebugAdditions() {
  }

  /** Extra haxe compiler arguments for a debuggable build, or null when the target has no debugger support yet. */
  @Nullable
  public static List<String> forTarget(@NotNull HaxeTarget target, @NotNull Debugger debugger) {
    return switch (target) {
      // HL: debug info in the bytecode; JS: -debug emits the .js.map the browser adapters need
      case HL, JAVA_SCRIPT -> List.of("-debug");
      case FLASH -> flashAdditions(debugger);
      case CPP -> cppAdditions(debugger);
      // TODO: eval (INTERP) has no debug additions yet
      default -> null;
    };
  }

  /** -debug embeds the line tables; the player debugger additionally needs the swf tagged. */
  @NotNull
  private static List<String> flashAdditions(@NotNull Debugger debugger) {
    return debugger.flashDebuggerTag() ? List.of("-debug", "-D", "fdb") : List.of("-debug");
  }

  /** Line tables plus the debugger's in-debuggee server - it connects out at startup guided by the HXCPP_DEBUG_HOST/PORT env vars. */
  @NotNull
  private static List<String> cppAdditions(@NotNull Debugger debugger) {
    String serverLib = debugger.hxcppServerLib();
    return serverLib == null ? List.of("-debug") : List.of("-debug", "-lib", serverLib);
  }
}
