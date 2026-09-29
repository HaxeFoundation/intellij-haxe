package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for Haxe action configurations. Keys on
 * {@link HaxeActionRunConfiguration} only, so no other runner is involved.
 */
public class HaxeActionRunner extends HaxePlainRunner {

  public HaxeActionRunner() {
    super("HaxeActionRunner", HaxeActionRunConfiguration.class);
  }
}
