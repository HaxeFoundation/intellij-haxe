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
   * desktop C++ (in-debuggee hxcpp server), browser JS (CDP adapters), Flash
   * and the interpreter (the debug server is part of haxe itself). The
   * compiled ones take {@link HaxeDebugAdditions}.
   */
  private static final Set<HaxeTarget> PROGRAM_TARGETS =
    Set.of(HaxeTarget.INTERP, HaxeTarget.HL, HaxeTarget.CPP, HaxeTarget.JAVA_SCRIPT, HaxeTarget.FLASH);

  /**
   * Targets a TEST debug session has a lane for - the same targets, partly
   * through other hosts: the interpreter (the compile IS the debuggee — the
   * eval lane), flash (fdb hosts the swf under adl; the test console parses
   * the trace lines fdb relays — see {@code HaxeTestFlashDebugRunner}) and js
   * under NODE ({@code node --inspect-brk} with the vscode-js-debug adapter
   * attached) or, for a Lime html5 tests build, in the browser through the
   * browser debug lane.
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
