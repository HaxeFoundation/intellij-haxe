package com.intellij.plugins.haxe.actions;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.ide.generation.*;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
abstract public class HaxeGenerateActionTest extends LightPlatformCodeInsightTestCase {
  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  protected HaxeGenerateActionTest() {
    PlatformTestCase.initPlatformLangPrefix();
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH + FileUtil.toSystemDependentName("/generate/");
  }

  protected void doOverrideTest() {
    doTest(new HaxeOverrideMethodHandler());
  }

  protected void doImplementTest() {
    doTest(new HaxeImplementMethodHandler());
  }

  protected void doGetterSetterTest(CreateGetterSetterFix.Strategy strategy) {
    configureByFile(getTestName(false) + ".hx");
    doTest(new HaxeGenerateAccessorHandler(strategy) {
      @Override
      protected String getTitle() {
        return "";
      }
    });
  }

  protected void doTest(BaseHaxeGenerateHandler anAction) {
    anAction.invoke(getProject(), getEditor(), getFile());
    checkResultByFile(getTestName(false) + ".txt");
  }

  public void testImplement1() throws Throwable {
    configureByFile("Implement1.hx");
    doImplementTest();
  }

  public void testImplement2() throws Throwable {
    configureByFile("Implement2.hx");
    doImplementTest();
  }

  public void testOverride1() throws Throwable {
    configureByFile("Override1.hx");
    doOverrideTest();
  }

  public void testGetter1() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTER);
  }

  public void testGetter2() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTER);
  }

  public void testSetter1() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.SETTER);
  }

  public void testSetter2() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.SETTER);
  }

  public void testGetterSetter1() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTERSETTER);
  }

  public void testGetterSetter2() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTERSETTER);
  }
}
