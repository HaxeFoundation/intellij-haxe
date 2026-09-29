package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for the dedicated HXCPP configuration (experimental). Keys on
 * {@link HxcppVshaxeRunConfiguration} only, so the legacy Haxe runners are never
 * involved in an HXCPP launch and vice versa.
 */
public class HxcppVshaxeRunner extends HaxePlainRunner {

  public HxcppVshaxeRunner() {
    super("HxcppVshaxeRunner", HxcppVshaxeRunConfiguration.class);
  }
}
