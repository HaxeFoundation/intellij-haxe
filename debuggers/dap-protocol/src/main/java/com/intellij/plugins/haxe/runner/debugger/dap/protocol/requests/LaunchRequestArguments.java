package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import java.util.List;
import lombok.Data;

/**
 * Arguments for the "launch" request (adapter-specific per the DAP spec).
 * {@code program} is the path to a .hl file compiled with -debug.
 * {@code hlPath} overrides the HashLink executable used to run it; when absent
 * the adapter uses the VM it is itself running on.
 * {@code stopOnEntry} is accepted but not yet honored by the adapter.
 *
 * Attach mode: when {@code attachPid} is set the client has already spawned
 * {@code hl --debug <debugPort> --debug-wait <program>} itself and the adapter
 * only attaches to that pid; the client owns the debuggee's stdio and
 * lifetime. This is what the IDE uses — spawning from the adapter (an HL
 * process) forces SW_HIDE onto the debuggee's first window on Windows.
 */
@Data
public class LaunchRequestArguments {
  private String program;
  private List<String> args;
  private String cwd;
  private String hlPath;
  private Boolean stopOnEntry;
  private Integer attachPid;
  private Integer debugPort;
}
