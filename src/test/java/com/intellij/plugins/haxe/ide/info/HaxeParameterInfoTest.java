/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide.info;

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeResolveResult;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl;
import com.intellij.plugins.haxe.util.HaxeDebugLogUtil;

import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.utils.parameterInfo.MockCreateParameterInfoContext;
import com.intellij.testFramework.utils.parameterInfo.MockParameterInfoUIContext;
import com.intellij.testFramework.utils.parameterInfo.MockUpdateParameterInfoContext;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;


/**
 * @author: Fedor.Korotkov
 */
public class HaxeParameterInfoTest extends LightPlatformCodeInsightTestCase {


  public void setUp() throws Exception {
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
    super.tearDown();
  }


  @NotNull
  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH + FileUtil.toSystemDependentName("/paramInfo/");
  }

  private void doTest(String infoText, int highlightedParameterIndex) throws Exception {
    configureByFile(getTestName(false) + ".hx");

    HaxeParameterInfoHandler parameterInfoHandler = new HaxeParameterInfoHandler();
    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(getEditor(), getFile());
    PsiElement elt = parameterInfoHandler.findElementForParameterInfo(createContext);
    assertNotNull(elt);
    parameterInfoHandler.showParameterInfo(elt, createContext);
    Object[] items = createContext.getItemsToShow();
    assertNotNull(items);
    assertTrue(items.length > 0);
    MockParameterInfoUIContext context = new MockParameterInfoUIContext<PsiElement>(elt);
    parameterInfoHandler.updateUI((HaxeFunctionDescription)items[0], context);
    assertEquals(infoText, context.getText());

    // index check
    MockUpdateParameterInfoContext updateContext = new MockUpdateParameterInfoContext(getEditor(), getFile());
    final PsiElement element = parameterInfoHandler.findElementForUpdatingParameterInfo(updateContext);
    assertNotNull(element);
    updateContext.setParameterOwner(elt);
    parameterInfoHandler.updateParameterInfo(element, updateContext);
    assertEquals(highlightedParameterIndex, updateContext.getCurrentParameter());
  }

  private void configureLoggerForDebugging() {
    HaxeDebugLogUtil.getLogger(HaxeResolveUtil.class).setLevel(LogLevel.DEBUG);
    HaxeDebugLogUtil.getLogger(HaxeReferenceImpl.class).setLevel(LogLevel.DEBUG);
    HaxeDebugLogUtil.getLogger(HaxeResolveResult.class).setLevel(LogLevel.DEBUG);
  }


  @Test
  public void testParamInfo1() throws Throwable {
    doTest("p1:Int, p2:Null<Unknown>, p3:Node", 0);
  }

  @Test
  public void testParamInfo2() throws Throwable {
    doTest("p1:Int, p2:Null<Unknown>, p3:Node", 2);
  }

  @Test
  public void testParamInfo3() throws Throwable {
    doTest("x:Int, y:Int", 0);
  }

  @Test
  public void testParamInfo4() throws Throwable {
    doTest("x:Int, y:Int", 1);
  }

  @Test
  public void testParamInfo5() throws Throwable {
    doTest("x:Int, y:Int", 1);
  }

  @Test
  public void testParamInfo6() throws Throwable {
    doTest("x:Int, y:Int = 239", 1);
  }

  @Test
  public void testParamInfo7() throws Throwable {
    configureLoggerForDebugging();
    doTest("t:Node", 0);
  }

  @Test
  public void testParamInfo8() throws Throwable {
    doTest("t:Node", 0);
  }

  @Test
  public void testParamInfo9() throws Throwable {
    doTest("a:Int, b:Bool = false, ?c:Float = null, ?d:Null<Unknown> = null", 2);
  }

  @Test
  public void testParamInfo10() throws Throwable {
    doTest("a:Int, b:Bool = false, ?c:Float = null, ?d:T = null", 3);
  }

  // Disabled - Tests issue #615.
  //@Test public void testLocalShadowingChainedGenerics() throws Throwable {
  //  doTest("t:Node", 0);
  //}
}
