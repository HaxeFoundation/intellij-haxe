package com.intellij.plugins.haxe.runner.neko;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/** Plain Run for the Neko configuration - the only executor it supports. */
public class NekoRunner extends HaxePlainRunner {

  public NekoRunner() {
    super("HaxeNekoRunner", NekoRunConfiguration.class);
  }
}
