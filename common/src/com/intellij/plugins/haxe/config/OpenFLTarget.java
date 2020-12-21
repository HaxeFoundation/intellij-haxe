/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

  // This mapping of the target to output target is the most likely scenario
  // and is mapped according to the lime templates.  (In other words,
  // this is what target lime will pick for you when you select an
  // OS target.)  This may end up being incorrect, but it's normally
  // correct when degugging and is a lot better than being presented
  // the interface file.
  // Note that the HaxeTarget is only used for IDEA's convenience and is not
  // passed to the compiler (lime) command, while the flags (third and later
  // arguments) are passed to the compiler.
  IOS("iOS", HaxeTarget.CPP, "ios", "-simulator"),
  ANDROID("Android", HaxeTarget.CPP, "android"),
  WEBOS("webOS", HaxeTarget.CPP, "webos"),
  BLACKBERRY("BlackBerry", HaxeTarget.CPP, "blackberry"),
  WINDOWS("Windows", HaxeTarget.CPP, "windows"),
  MAC("Mac OS", HaxeTarget.CPP, "mac"),
  LINUX("Linux", HaxeTarget.CPP, "linux"),
  LINUX64("Linux 64", HaxeTarget.CPP, "linux", "-64"),
  FLASH("Flash", HaxeTarget.FLASH, "flash"),
  HTML5("HTML5", HaxeTarget.JAVA_SCRIPT, "html5"),
  NEKO("Neko", HaxeTarget.NEKO, "neko"),
  HASHLINK("HashLink", HaxeTarget.HASHLINK, "hl"),
  TIZEN("Tizen", HaxeTarget.CPP, "tizen"),
  EMSCRIPTEN("Emscripten", HaxeTarget.CPP, "emscripten"),
  AIR("Adobe AIR", HaxeTarget.FLASH, "air");

  private final String[] flags;
  private final String description;
  private final HaxeTarget outputTarget;

  OpenFLTarget(String description, HaxeTarget target, String... flags) {
    this.flags = flags;
    this.description = description;
    this.outputTarget = target;
  }

  public String getTargetFlag() {
    return flags.length > 0 ? flags[0] : "";
  }

  public String[] getFlags() {
    return flags;
  }

  public HaxeTarget getOutputTarget() {
    return outputTarget;
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
