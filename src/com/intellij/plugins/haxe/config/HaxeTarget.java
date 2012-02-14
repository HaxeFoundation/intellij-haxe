package com.intellij.plugins.haxe.config;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public enum HaxeTarget {
  NEKO("neko") {
    @NotNull
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".n";
    }
  }, JAVA_SCRIPT("js") {
    @NotNull
    @Override
    public String getTargetOutput(Module module) {
      return module.getName() + ".js";
    }
  }, FLASH("swf") {
    @NotNull
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

  @NotNull
  public abstract String getTargetOutput(Module module);

  public static void initCombo(@NotNull DefaultComboBoxModel comboBoxModel) {
    for (HaxeTarget target : HaxeTarget.values()) {
      comboBoxModel.insertElementAt(target, 0);
    }
  }
}
