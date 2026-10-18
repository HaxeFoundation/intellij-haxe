package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import java.util.Set;

/** Which adapter output events reach the debug console. */
final class DapOutputEvents {
  /** The categories carrying the debuggee's own output. */
  private static final Set<String> PROGRAM_STREAMS = Set.of("stdout", "stderr");

  /** Adapter telemetry (js-debug's "js-debug/dap/operation" reports); the protocol says a client never shows it. */
  private static final String TELEMETRY = "telemetry";

  private DapOutputEvents() {
  }

  /**
   * Whether an output event of {@code category} is shown; {@code programOutputEchoed}
   * says the backend's adapter repeats what the process streams already deliver
   * ({@link DapBackend#echoesProgramOutput()}).
   */
  static boolean shownToUser(String category, boolean programOutputEchoed) {
    if (category == null) return true;
    if (TELEMETRY.equals(category)) return false;
    return !(programOutputEchoed && PROGRAM_STREAMS.contains(category));
  }
}
