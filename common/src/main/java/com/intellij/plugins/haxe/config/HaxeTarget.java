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

import com.intellij.plugins.haxe.HaxeCommonBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


/**
 * @author: Fedor.Korotkov
 */
public enum HaxeTarget {

  // Target     clFlag    Extension,  OutputDir,  OutputType              Description

  NEKO(         "neko",   ".n",       "neko",     OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.neko")),
  JAVA_SCRIPT(  "js",     ".js",      "js",       OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.js")),
  FLASH(        "swf",    ".swf",     "flash",    OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.swf")),
  CPP(          "cpp",    ".exe",     "cpp",      OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.cpp")),
  CPPIA(        "cppia",  ".cppia",   "cppia",    OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.cppia")),
  PHP(          "php",    ".php",     "php",      OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.php")),
  JAVA(         "java",   ".jar",     "java",     OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.java")),
  CSHARP(       "cs",     ".exe",     "cs",       OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.csharp")),
  PYTHON(       "python", ".py",      "python",   OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.python")),
  LUA(          "lua",    ".lua",     "lua",      OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.lua")),
  HL(           "hl",     ".hl",      "hl",       OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.hl")),
  INTERP(       "-interp","",         "",         OUTPUT_TYPE.NONE,       HaxeCommonBundle.message("haxe.target.interp"));

  private enum OUTPUT_TYPE {
    FILE,
    DIRECTORY,
    NONE
  }

  private final String flag;
  private final String description;
  private final String outputDir;
  private final String fileExtension;
  private final OUTPUT_TYPE outputType;

  HaxeTarget(String flag, String fileExtension, String outputDir, OUTPUT_TYPE outputType, String description) {
    this.flag = flag;
    this.description = description;
    this.outputDir = outputDir;
    this.outputType = outputType;
    this.fileExtension = fileExtension;
  }

  public String getFlag() {
    return flag;
  }

  public String getCompilerFlag() {
    return "-" + flag;
  }

  public String getDefaultOutputSubdirectory() {
    return outputDir;
  }

  @NotNull
  public String getTargetFileNameWithExtension(String fileName) {
    return fileName + fileExtension;
  }

  public static void initCombo(@NotNull DefaultComboBoxModel comboBoxModel) {
    for (HaxeTarget target : HaxeTarget.values()) {
      comboBoxModel.insertElementAt(target, 0);
    }
  }

  @Override
  public String toString() {
    return description;
  }

  /**
   * Match the string against the compiler's argument/parameter/flag for the
   * target output.
   *
   * @param compilerTargetArgument - string to compare, e.g. '-js', '-neko'
   * @return The target matching the flag, or null, if not found.
   */
  @Nullable
  public static HaxeTarget matchOutputTarget(String compilerTargetArgument) {
    for (HaxeTarget t : HaxeTarget.values()) {
      if (t.getCompilerFlag().equals(compilerTargetArgument)) {
        return t;
      }
    }
    // as3 is an old case.
    if ("-as3".equals(compilerTargetArgument)) {
      return HaxeTarget.FLASH;
    }
    return null;
  }

  public boolean isOutputToDirectory() {
    return outputType == OUTPUT_TYPE.DIRECTORY;
  }
  public boolean isOutputToSingleFile() {
    return outputType == OUTPUT_TYPE.FILE;
  }
  public boolean isNoOutput() {
    return outputType == OUTPUT_TYPE.NONE;
  }
}