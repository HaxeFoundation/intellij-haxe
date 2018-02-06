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


/**
 * @author: Fedor.Korotkov
 */
public enum HaxeTarget {

  NEKO("neko", HaxeCommonBundle.message("haxe.target.neko"), "neko", OUTPUT_TYPE.FILE) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".n";
    }
  }, JAVA_SCRIPT("js", HaxeCommonBundle.message("haxe.target.js"), "js", OUTPUT_TYPE.FILE) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".js";
    }
  }, FLASH("swf", HaxeCommonBundle.message("haxe.target.swf"), "flash", OUTPUT_TYPE.FILE) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".swf";
    }
  }, CPP("cpp", HaxeCommonBundle.message("haxe.target.cpp"), "cpp", OUTPUT_TYPE.DIRECTORY) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".exe";
    }
  }, CPPIA("cppia", HaxeCommonBundle.message("haxe.target.cppia"), "cppia", OUTPUT_TYPE.FILE) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".cppia";
    }
  }, PHP("php", HaxeCommonBundle.message("haxe.target.php"), "php", OUTPUT_TYPE.DIRECTORY) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".php";
    }

  }, JAVA("java", HaxeCommonBundle.message("haxe.target.java"), "java", OUTPUT_TYPE.DIRECTORY) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".jar";
    }
  }, CSHARP("cs", HaxeCommonBundle.message("haxe.target.csharp"), "cs", OUTPUT_TYPE.DIRECTORY) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".exe";
    }
  }, PYTHON("python", HaxeCommonBundle.message("haxe.target.python"), "python", OUTPUT_TYPE.FILE) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".py";
    }
  }, LUA("lua", HaxeCommonBundle.message("haxe.target.lua"), "lua", OUTPUT_TYPE.FILE) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".lua";
    }
  }, HL("hl", HaxeCommonBundle.message("haxe.target.hl"), "hl", OUTPUT_TYPE.FILE) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) { return fileName + ".hl"; }
  };

  private enum OUTPUT_TYPE {
    FILE,
    DIRECTORY
  }

  private final String flag;
  private final String description;
  private final String outputDir;
  private final OUTPUT_TYPE outputType;

  HaxeTarget(String flag, String description, String outputDir, OUTPUT_TYPE outputType) {
    this.flag = flag;
    this.description = description;
    this.outputDir = outputDir;
    this.outputType = outputType;
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
  public abstract String getTargetFileNameWithExtension(String fileName);

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
}