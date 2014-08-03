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

import com.intellij.plugins.haxe.HaxeCommonBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public enum HaxeTarget {
  NEKO("neko", HaxeCommonBundle.message("haxe.target.neko")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".n";
    }
  }, JAVA_SCRIPT("js", HaxeCommonBundle.message("haxe.target.js")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".js";
    }
  }, FLASH("swf", HaxeCommonBundle.message("haxe.target.swf")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".swf";
    }
  }, CPP("cpp", HaxeCommonBundle.message("haxe.target.cpp")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".exe";
    }
  }, PHP("php", HaxeCommonBundle.message("haxe.target.php")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".php";
    }
  }, JAVA("java", HaxeCommonBundle.message("haxe.target.java")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".jar";
    }
  }, CSHARP("cs", HaxeCommonBundle.message("haxe.target.csharp")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".exe";
    }
  }, PYTHON("python", HaxeCommonBundle.message("haxe.target.python")) {
    @NotNull
    @Override
    public String getTargetFileNameWithExtension(String fileName) {
      return fileName + ".py";
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
}