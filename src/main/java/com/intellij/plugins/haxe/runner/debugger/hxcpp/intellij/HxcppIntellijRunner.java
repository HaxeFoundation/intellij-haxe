package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for the HXCPP (IntelliJ debug server) configuration. No env vars
 * are set, so the embedded server sees an unconfigured session, makes one
 * quick connect attempt and stays out of the way — the program just runs.
 */
public class HxcppIntellijRunner extends HaxePlainRunner {

  public HxcppIntellijRunner() {
    super("HxcppIntellijRunner", HxcppIntellijRunConfiguration.class);
  }
}
