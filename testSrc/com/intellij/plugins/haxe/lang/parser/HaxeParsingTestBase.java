package com.intellij.plugins.haxe.lang.parser;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.testFramework.ParsingTestCase;

import java.io.File;

abstract public class HaxeParsingTestBase extends ParsingTestCase {
  public HaxeParsingTestBase(String... path) {
    super(getPath(path), HaxeFileType.DEFAULT_EXTENSION, new HaxeParserDefinition());
  }

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      if (result.length() > 0) {
        result.append(File.separator);
      }
      result.append(folder);
    }
    return result.toString();
  }

  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/");
  }

  @Override
  protected boolean skipSpaces() {
    return true;
  }
}
