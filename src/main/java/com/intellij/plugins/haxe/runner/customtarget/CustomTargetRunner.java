package com.intellij.plugins.haxe.runner.customtarget;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/** Plain Run for the Custom Target configuration - the only executor it supports. */
public class CustomTargetRunner extends HaxePlainRunner {

  public CustomTargetRunner() {
    super("HaxeCustomTargetRunner", CustomTargetRunConfiguration.class);
  }
}
