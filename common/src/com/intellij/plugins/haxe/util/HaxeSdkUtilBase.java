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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSdkUtilBase {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.util.HaxeSdkUtilBase");
  protected static final String COMPILER_EXECUTABLE_NAME = "haxe";
  protected static final String HAXELIB_EXECUTABLE_NAME = "haxelib";

  @Nullable
  public static String getCompilerPathByFolderPath(String folderPath) {
    return getExecutablePathByFolderPath(folderPath, COMPILER_EXECUTABLE_NAME);
  }

  @Nullable
  public static String getHaxelibPathByFolderPath(String folderPath) {
    return getExecutablePathByFolderPath(folderPath, HAXELIB_EXECUTABLE_NAME);
  }

  @Nullable
  private static String getExecutablePathByFolderPath(String folderPath, String name) {
    if (!SystemInfo.isLinux) {
      final String candidatePath = folderPath + "/bin/" + getExecutableName(name);
      LOG.debug("candidatePath: " + candidatePath);
      if (fileExists(candidatePath)) {
        return FileUtil.toSystemIndependentName(candidatePath);
      }
    }

    final String resultPath = folderPath + "/" + getExecutableName(name);
    LOG.debug("resultPath: " + resultPath);
    if (fileExists(resultPath)) {
      return FileUtil.toSystemIndependentName(resultPath);
    }

    LOG.debug(name + " path: null");
    return null;
  }

  public static String getExecutableName(String name) {
    if (SystemInfo.isWindows) {
      return name + ".exe";
    }
    return name;
  }

  private static boolean fileExists(@Nullable String filePath) {
    return filePath != null && new File(FileUtil.toSystemDependentName(filePath)).exists();
  }
}
