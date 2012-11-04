package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSdkUtilBase {
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
      if (fileExists(candidatePath)) {
        return FileUtil.toSystemIndependentName(candidatePath);
      }
    }

    final String resultPath = folderPath + "/" + getExecutableName(name);
    if (fileExists(resultPath)) {
      return FileUtil.toSystemIndependentName(resultPath);
    }

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
