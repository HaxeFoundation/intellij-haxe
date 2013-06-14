package com.intellij.plugins.haxe.lang.parser;

import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.testFramework.ParsingTestCase;

abstract public class HaxeParsingTestBase extends ParsingTestCase {
  public HaxeParsingTestBase(String... path) {
    super(getPath(path), HaxeFileType.DEFAULT_EXTENSION, new HaxeParserDefinition());
  }

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      if (result.length() > 0) {
        result.append("/");
      }
      result.append(folder);
    }
    return result.toString();
  }

  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  @Override
  protected boolean skipSpaces() {
    return true;
  }
}
