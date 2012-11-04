package com.intellij.plugins.haxe.config;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public enum NMETarget {
  IOS("iOS", "ios", "-simulator"),
  ANDROID("Android", "android"),
  WEOS("webOS", "webos"),
  BLACKBERRY("BlackBerry", "blackberry"),
  WINDOWS("Windows", "windows"),
  MAC("Mac OS", "mac"),
  LINUX("Linux", "linux"),
  LINUX64("Linux 64", "linux", "-64"),
  FLASH("Flash", "flash"),
  HTML5("HTML5", "html5");

  private final String[] flags;
  private final String description;

  NMETarget(String description, String... flags) {
    this.flags = flags;
    this.description = description;
  }

  public String getTargetFlag() {
    return flags.length > 0 ? flags[0] : "";
  }

  public String[] getFlags() {
    return flags;
  }

  public static void initCombo(@NotNull DefaultComboBoxModel comboBoxModel) {
    for (NMETarget target : NMETarget.values()) {
      comboBoxModel.insertElementAt(target, 0);
    }
  }

  @Override
  public String toString() {
    return description;
  }
}
