package com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for the legacy-HXCPP configuration: launches the executable
 * without any debugger flags. Keys on {@link LegacyHxcppRunConfiguration}
 * only, so the other flavours are never involved and vice versa.
 */
public class LegacyHxcppRunner extends HaxePlainRunner {

  public LegacyHxcppRunner() {
    super("HaxeLegacyHxcppRunner", LegacyHxcppRunConfiguration.class);
  }
}
