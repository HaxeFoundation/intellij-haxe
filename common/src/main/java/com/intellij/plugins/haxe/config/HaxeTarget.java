/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author: Fedor.Korotkov
 */
public enum HaxeTarget {

  // Target     clFlag    Extension,  OutputDir,  OutputType              Description                                     Platform (+ flag-set defines)

  NEKO(         "neko",   ".n",       "neko",     OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.neko"),   Platform.NEKO),
  JAVA_SCRIPT(  "js",     ".js",      "js",       OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.js"),     Platform.JS),
  FLASH(        "swf",    ".swf",     "flash",    OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.swf"),    Platform.FLASH),
  CPP(          "cpp",    ".exe",     "cpp",      OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.cpp"),    Platform.CPP),
  CPPIA(        "cppia",  ".cppia",   "cppia",    OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.cppia"),  Platform.CPP, "cppia"),
  PHP(          "php",    ".php",     "php",      OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.php"),    Platform.PHP),
  // the enum does not tell --java from --jvm; jvm is the only path a current toolchain supports
  JAVA(         "java",   ".jar",     "java",     OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.java"),   Platform.JAVA, "jvm"),
  CSHARP(       "cs",     ".exe",     "cs",       OUTPUT_TYPE.DIRECTORY,  HaxeCommonBundle.message("haxe.target.csharp"), Platform.CS),
  PYTHON(       "python", ".py",      "python",   OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.python"), Platform.PYTHON),
  LUA(          "lua",    ".lua",     "lua",      OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.lua"),    Platform.LUA),
  HL(           "hl",     ".hl",      "hl",       OUTPUT_TYPE.FILE,       HaxeCommonBundle.message("haxe.target.hl"),     Platform.HL),
  INTERP(       "-interp","",         "",         OUTPUT_TYPE.NONE,       HaxeCommonBundle.message("haxe.target.interp"), Platform.EVAL, "interp");

  /** The value the compiler gives a define set without one ({@code -D flag} == {@code -D flag=1}). */
  public static final String FLAG_DEFINE_VALUE = "1";

  /**
   * A compiler platform and the capabilities its platform config declares
   * (haxe 4.3 {@code Common.get_config}); {@link #defines()} mirrors what
   * {@code init_platform} defines from them. Several targets share one
   * platform: cppia is cpp, --interp is eval.
   */
  private enum Platform {
    //       name      static sys    utf16  thread unicode atomics
    JS(     "js",     false, false, true,  false, true,  true),
    LUA(    "lua",    false, true,  false, false, true,  false),
    NEKO(   "neko",   false, true,  false, true,  false, false),
    FLASH(  "flash",  true,  false, true,  false, true,  false),
    PHP(    "php",    false, true,  false, false, true,  false),
    CPP(    "cpp",    true,  true,  true,  true,  true,  true),
    CS(     "cs",     true,  true,  true,  true,  true,  true),
    JAVA(   "java",   true,  true,  true,  true,  true,  true),
    PYTHON( "python", false, true,  false, true,  true,  false),
    HL(     "hl",     true,  true,  true,  true,  true,  true),
    EVAL(   "eval",   false, true,  false, true,  true,  false);

    private final String name;
    private final boolean isStatic;
    private final boolean sys;
    private final boolean utf16;
    private final boolean threaded;
    private final boolean unicode;
    private final boolean atomics;

    Platform(String name, boolean isStatic, boolean sys, boolean utf16, boolean threaded, boolean unicode, boolean atomics) {
      this.name = name;
      this.isStatic = isStatic;
      this.sys = sys;
      this.utf16 = utf16;
      this.threaded = threaded;
      this.unicode = unicode;
      this.atomics = atomics;
    }

    /** The platform's defines in the compiler's order; {@code target.name} carries the name, the rest are flags. */
    Map<String, String> defines() {
      Map<String, String> defines = new LinkedHashMap<>();
      if (isStatic) putFlags(defines, "target.static", "static");
      if (sys) putFlags(defines, "target.sys", "sys");
      if (utf16) putFlags(defines, "target.utf16", "utf16");
      if (threaded) putFlags(defines, "target.threaded");
      if (unicode) putFlags(defines, "target.unicode");
      defines.put("target.name", name);
      putFlags(defines, name);
      if (atomics) putFlags(defines, "target.atomics");
      return defines;
    }

    private static void putFlags(Map<String, String> defines, String... names) {
      for (String name : names) {
        defines.put(name, FLAG_DEFINE_VALUE);
      }
    }
  }

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
  private final Map<String, String> defines;

  HaxeTarget(String flag, String fileExtension, String outputDir, OUTPUT_TYPE outputType, String description,
             Platform platform, String... flagDefines) {
    this.flag = flag;
    this.description = description;
    this.outputDir = outputDir;
    this.outputType = outputType;
    this.fileExtension = fileExtension;
    this.defines = targetDefines(platform, flagDefines);
  }

  /** The target flag's own defines ({@code cppia}, {@code jvm}, {@code interp}) are set before the platform's. */
  private static Map<String, String> targetDefines(Platform platform, String... flagDefines) {
    Map<String, String> defines = new LinkedHashMap<>();
    for (String name : flagDefines) {
      defines.put(name, FLAG_DEFINE_VALUE);
    }
    defines.putAll(platform.defines());
    return Collections.unmodifiableMap(defines);
  }

  public String getFlag() {
    return flag;
  }

  public String getCompilerFlag() {
    return "-" + flag;
  }

  /**
   * The defines the compiler sets for this target before reading any build
   * file: the platform name, {@code target.name=<name>}, {@code sys},
   * {@code static}, {@code utf16} and the {@code target.*} capability flags,
   * plus the target flag's own define. Flags carry {@link #FLAG_DEFINE_VALUE}.
   */
  public Map<String, String> getDefines() {
    return defines;
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