package com.intellij.plugins.haxe.runner.debugger.flash;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for the dedicated AIR configuration: adl launches the app, no
 * Flash plugin involved. Keys on {@link AirRunConfiguration} only.
 */
public class AirRunner extends HaxePlainRunner {

  public AirRunner() {
    super("HaxeAirRunner", AirRunConfiguration.class);
  }
}
