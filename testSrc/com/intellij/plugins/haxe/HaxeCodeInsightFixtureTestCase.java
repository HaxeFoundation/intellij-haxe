package com.intellij.plugins.haxe;

import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

/**
 * Created by fedorkorotkov.
 */
abstract public class HaxeCodeInsightFixtureTestCase extends JavaCodeInsightFixtureTestCase {
  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  protected HaxeCodeInsightFixtureTestCase() {
    PlatformTestCase.initPlatformLangPrefix();
  }

  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }
}
