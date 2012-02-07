package com.intellij.plugins.haxe.config;

import com.intellij.openapi.module.Module;

/**
 * @author: Fedor.Korotkov
 */
public enum HaxeTarget {
  NEKO("neko") {
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".n";
    }
  }, JAVA_SCRIPT("js") {
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".js";
    }
  }, FLASH("swf") {
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".swf";
    }
  };

  private final String flag;

  HaxeTarget(String flag) {
    this.flag = flag;
  }

  public String getCompilerFlag() {
    return "-" + flag;
  }

  public abstract String getTargetOutput(Module module);
}
