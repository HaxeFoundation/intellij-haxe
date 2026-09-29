package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.v2.buildtools.HaxeDebugAdditions;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * ONE home for "does a debugger lane exist for this target": program and
 * test sessions both gate on it. The build-system side of the question
 * (which target does THIS file's selection compile to) is
 * {@code HaxeBuildSystem}'s job.
 */
public final class HaxeDebugSupport {

  /**
   * Targets a PROGRAM debug session has a lane for: HashLink (DAP adapter),
   * desktop C++ (in-debuggee hxcpp server), browser JS (CDP adapters) and
   * Flash. Their compiles take {@link HaxeDebugAdditions}.
   */
  private static final Set<HaxeTarget> PROGRAM_TARGETS =
    Set.of(HaxeTarget.HL, HaxeTarget.CPP, HaxeTarget.JAVA_SCRIPT, HaxeTarget.FLASH);

  /**
   * Targets a TEST debug session has a lane for. Tests additionally debug the
   * interpreter (the compile IS the debuggee — the eval lane), flash (fdb
   * hosts the swf under adl; the test console parses the trace lines fdb
   * relays — see {@code HaxeTestFlashDebugRunner}) and js under NODE
   * ({@code node --inspect-brk} with the vscode-js-debug adapter attached).
   * Every runnable js tests plan launches through node today; BROWSER-hosted
   * html5 builds are refused at plan time, so the coarse per-target answer
   * stays accurate.
   */
  private static final Set<HaxeTarget> TEST_TARGETS =
    Set.of(HaxeTarget.INTERP, HaxeTarget.HL, HaxeTarget.CPP, HaxeTarget.FLASH, HaxeTarget.JAVA_SCRIPT);

  private HaxeDebugSupport() {
  }

  public static boolean supportsProgramDebug(@Nullable HaxeTarget target) {
    return target != null && PROGRAM_TARGETS.contains(target);
  }

  public static boolean supportsTestDebug(@Nullable HaxeTarget target) {
    return target != null && TEST_TARGETS.contains(target);
  }
}
