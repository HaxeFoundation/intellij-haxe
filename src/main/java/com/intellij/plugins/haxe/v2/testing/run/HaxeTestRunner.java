package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for Haxe unit-test configurations. Keys on
 * {@link HaxeTestRunConfiguration} only, so no other runner is involved; the
 * Debug executor goes through {@link HaxeTestDebugRunner}.
 */
public class HaxeTestRunner extends HaxePlainRunner {

  public HaxeTestRunner() {
    super("HaxeTestRunner", HaxeTestRunConfiguration.class);
  }
}
