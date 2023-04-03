/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
public enum NMETarget {

  // The HaxeTarget values declared here are the most obvious intention.
  // They may not be correct.  They follow the mapping that the lime command
  // does when invoked for a target OS.  They are used by the classpath generator
  // the create the implicit classpath that the Haxe compiler uses.
  // This may lead to the wrong source file being presented when debugging,
  // but it's a definite step up from showing the interface file.
  // Note that the HaxeTarget is only used for IDEA's convenience and is not
  // passed to the compiler (lime) command, while the flags (third and later
  // arguments) are passed to the compiler.

  IOS("iOS", HaxeTarget.NEKO, "ios", "-simulator"),
  ANDROID("Android", HaxeTarget.NEKO, "android"),
  WEBOS("webOS", HaxeTarget.NEKO, "webos"),
  BLACKBERRY("BlackBerry", HaxeTarget.NEKO, "blackberry"),
  WINDOWS("Windows", HaxeTarget.NEKO, "windows"),
  MAC("Mac OS", HaxeTarget.NEKO, "mac"),
  LINUX("Linux", HaxeTarget.NEKO, "linux"),
  LINUX64("Linux 64", HaxeTarget.NEKO,  "linux", "-64"),
  FLASH("Flash", HaxeTarget.FLASH, "flash"),
  HTML5("HTML5", HaxeTarget.JAVA_SCRIPT, "html5"),
  NEKO("Neko", HaxeTarget.NEKO, "neko");

  private final String[] flags;
  private final String description;
  private final HaxeTarget outputTarget;

  NMETarget(String description, HaxeTarget target, String... flags) {
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

  public HaxeTarget getOutputTarget() { return outputTarget; }

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
