package com.intellij.plugins.haxe.util;

import com.intellij.openapi.application.PathManager;

import java.io.File;

/**
 * Created by fedorkorotkov.
 */
public class HaxeTestUtils {
  /**
   * The root of the test data directory
   */
  public static final String BASE_TEST_DATA_PATH = findTestDataPath();

  private static String findTestDataPath() {
    File f = new File("testData");
    if (f.exists()) {
      return f.getAbsolutePath();
    }
    return PathManager.getHomePath() + "/plugins/haxe/testData";
  }
}
