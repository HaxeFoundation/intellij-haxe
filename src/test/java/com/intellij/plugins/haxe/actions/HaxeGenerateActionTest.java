/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2020 Eric Bishton
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
package com.intellij.plugins.haxe.actions;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.ide.generation.*;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.plugins.haxe.HaxeLightCodeInsightTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Code generation: generate action")
public class HaxeGenerateActionTest extends HaxeLightCodeInsightTestBase {

  @Override
  protected void tearDown() throws Exception {
    HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
    super.tearDown();
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

  protected void doConstructorTest() {
    configureByFile(getTestName(false) + ".hx");
    doTest(new HaxeConstructorHandler());
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

  @Test
  @DisplayName("implement 1")
  public void testImplement1() throws Throwable {
    configureByFile("Implement1.hx");
    doImplementTest();
  }

  @Test
  @DisplayName("implement 2")
  public void testImplement2() throws Throwable {
    configureByFile("Implement2.hx");
    doImplementTest();
  }

  @Test
  @DisplayName("override 1")
  public void testOverride1() throws Throwable {
    configureByFile("Override1.hx");
    doOverrideTest();
  }

  @Test
  @DisplayName("override 2")
  public void testOverride2() throws Throwable {
    configureByFile("Override2.hx");
    doOverrideTest();
  }

  @Test
  @DisplayName("getter 1")
  public void testGetter1() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTER);
  }

  @Test
  @DisplayName("getter 2")
  public void testGetter2() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTER);
  }

  @Test
  @DisplayName("setter 1")
  public void testSetter1() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.SETTER);
  }

  @Test
  @DisplayName("setter 2")
  public void testSetter2() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.SETTER);
  }

  @Test
  @DisplayName("getter setter 1")
  public void testGetterSetter1() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTERSETTER);
  }

  @Test
  @DisplayName("getter setter 2")
  public void testGetterSetter2() throws Throwable {
    doGetterSetterTest(CreateGetterSetterFix.Strategy.GETTERSETTER);
  }

  @Test
  @DisplayName("constructor")
  public void testConstructor() throws Throwable {
    doConstructorTest();
  }

  @Test
  @DisplayName("constructor 2")
  public void testConstructor2() throws Throwable {
    doConstructorTest();
  }
  @Test
  @DisplayName("constructor in abstract")
  public void testConstructorInAbstract() throws Throwable {
    doConstructorTest();
  }
}
