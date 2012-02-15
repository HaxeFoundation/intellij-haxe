package com.intellij.plugins.haxe.config;

import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public enum HaxeTarget {
  NEKO("neko", HaxeBundle.message("haxe.target.neko")) {
    @NotNull
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".n";
    }
  }, JAVA_SCRIPT("js", HaxeBundle.message("haxe.target.js")) {
    @NotNull
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".js";
    }
  }, FLASH("swf", HaxeBundle.message("haxe.target.swf")) {
    @NotNull
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".swf";
    }
  };

  private final String flag;
  private final String description;

  HaxeTarget(String flag, String description) {
    this.flag = flag;
    this.description = description;
  }

  public String getCompilerFlag() {
    return "-" + flag;
  }

  @NotNull
  public abstract String getTargetOutput(Module module);

  public static void initCombo(@NotNull DefaultComboBoxModel comboBoxModel) {
    for (HaxeTarget target : HaxeTarget.values()) {
      comboBoxModel.insertElementAt(target, 0);
    }
  }

  @Override
  public String toString() {
    return description;
  }
}
