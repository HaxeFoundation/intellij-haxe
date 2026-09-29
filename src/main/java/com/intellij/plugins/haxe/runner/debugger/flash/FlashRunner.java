package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for the dedicated Flash configuration. Keys on
 * {@link FlashRunConfiguration} only, so the other flavours are never involved
 * in a Flash launch and vice versa.
 */
public class FlashRunner extends HaxePlainRunner {

  public FlashRunner() {
    super("HaxeFlashRunner", FlashRunConfiguration.class);
  }
}
