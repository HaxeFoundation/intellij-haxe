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
package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import org.junit.Test;

/**
 * Test for the HaxeDeprecatedInspection.
 * <p/>
 * Created by Usievaład Kimajeŭ on 7.06.2016.
 */
public class HaxeDeprecatedInspectionTest extends HaxeCodeInsightFixtureTestCase {
  @Test
  public void testMethod() {
    doTest("Method.hx");
  }

  @Test
  public void testMethodStatic() {
    doTest("MethodStatic.hx");
  }

  @Test
  public void testMethodStaticText() {
    doTest("MethodStaticText.hx");
  }

  @Test
  public void testMethodText() {
    doTest("MethodText.hx");
  }

  @Test
  public void testProperty() {
    doTest("Property.hx");
  }

  @Test
  public void testPropertyStatic() {
    doTest("PropertyStatic.hx");
  }

  @Test
  public void testPropertyStaticText() {
    doTest("PropertyStaticText.hx");
  }

  @Test
  public void testPropertyText() {
    doTest("PropertyText.hx");
  }

  @Test
  public void testVariable() {
    doTest("Variable.hx");
  }

  @Test
  public void testVariableStatic() {
    doTest("VariableStatic.hx");
  }

  @Test
  public void testVariableStaticText() {
    doTest("VariableStaticText.hx");
  }

  @Test
  public void testVariableText() {
    doTest("VariableText.hx");
  }

  @Override
  protected String getBasePath() {
    return "/annotation.semantic/deprecated/";
  }

  private void doTest(String fileName) {
    myFixture.configureByFiles(fileName, "Deprecated.hx");
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.enableInspections(new HaxeDeprecatedInspection());
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }
}