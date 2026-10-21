package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/**
 * Plain Run for the browser configuration (experimental): serve the content
 * (when configured) and open the browser — no debugger involved. Keys on
 * {@link BrowserRunConfiguration} only.
 */
public class BrowserRunner extends HaxePlainRunner {

  public BrowserRunner() {
    super("HaxeBrowserRunner", BrowserRunConfiguration.class);
  }
}
