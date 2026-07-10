package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import java.util.List;
import lombok.Data;

/**
 * Arguments for the "launch" request (adapter-specific per the DAP spec).
 * {@code program} is the path to a .hl file compiled with -debug.
 * {@code hlPath} overrides the HashLink executable used to run it; when absent
 * the adapter uses the VM it is itself running on.
 * {@code stopOnEntry} is accepted but not yet honored by the adapter.
 */
@Data
public class LaunchRequestArguments {
  private String program;
  private List<String> args;
  private String cwd;
  private String hlPath;
  private Boolean stopOnEntry;
}
