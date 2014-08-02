/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.config;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public enum OpenFLTarget {
  IOS("iOS", "ios", "-simulator"),
  ANDROID("Android", "android"),
  WEBOS("webOS", "webos"),
  BLACKBERRY("BlackBerry", "blackberry"),
  WINDOWS("Windows", "windows"),
  MAC("Mac OS", "mac"),
  LINUX("Linux", "linux"),
  LINUX64("Linux 64", "linux", "-64"),
  FLASH("Flash", "flash"),
  HTML5("HTML5", "html5"),
  NEKO("Neko", "neko"),
  TIZEN("Tizen", "tizen"),
  EMSCRIPTEN("Emscripten", "emscripten");

  private final String[] flags;
  private final String description;

  OpenFLTarget(String description, String... flags) {
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
    for (OpenFLTarget target : OpenFLTarget.values()) {
      comboBoxModel.insertElementAt(target, 0);
    }
  }

  @Override
  public String toString() {
    return description;
  }
}
