package com.intellij.plugins.haxe.runner.nodejs;

import com.intellij.plugins.haxe.runner.HaxePlainRunner;

/** Plain Run for the Node.js configuration - the only executor it supports. */
public class NodeRunner extends HaxePlainRunner {

  public NodeRunner() {
    super("HaxeNodeRunner", NodeRunConfiguration.class);
  }
}
