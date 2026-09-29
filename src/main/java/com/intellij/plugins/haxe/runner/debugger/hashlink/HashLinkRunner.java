package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for the dedicated HashLink configuration (experimental). Keys on
 * {@link HashLinkRunConfiguration} only, so the legacy Haxe runners are never
 * involved in a HashLink launch and vice versa.
 */
public class HashLinkRunner extends HaxePlainRunner {

  public HashLinkRunner() {
    super("HashLinkRunner", HashLinkRunConfiguration.class);
  }
}
