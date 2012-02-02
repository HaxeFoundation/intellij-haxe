package com.intellij.plugins.haxe.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompilationUtil {
  public static final String NEKO_BIN_EXTENSION = ".n";

  public static String getClassNameByPath(String path) {
    Pattern pattern = Pattern.compile("\\.*([\\w\\d]+)\\." + HaxeFileType.DEFAULT_EXTENSION);
    Matcher matcher = pattern.matcher(path);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

  public static String getSourceFolderByModule(Module module) {
    VirtualFile moduleDir = module.getModuleFile().getParent();
    VirtualFile sourceDir = moduleDir.findChild("src");
    return sourceDir.getPath();
  }

  public static String getNekoBinPathForModule(Module module) {
    VirtualFile moduleFile = module.getModuleFile();
    File makeFile = new File(moduleFile.getParent().getPath(), getNekoBinName(module.getName()));
    return makeFile.getPath();
  }

  public static String getNekoBinName(String className) {
    return className + NEKO_BIN_EXTENSION;
  }
}
