/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test for the HaxeDeprecatedInspection.
 * <p/>
 * Created by Usievaład Kimajeŭ on 7.06.2016.
 */
@DisplayName("Inspection: deprecated")
public class HaxeDeprecatedInspectionTest extends HaxeLightFixtureTestCase {
  @Test
  @DisplayName("method")
  public void testMethod() {
    doTest("Method.hx");
  }

  @Test
  @DisplayName("method static")
  public void testMethodStatic() {
    doTest("MethodStatic.hx");
  }

  @Test
  @DisplayName("method static text")
  public void testMethodStaticText() {
    doTest("MethodStaticText.hx");
  }

  @Test
  @DisplayName("method text")
  public void testMethodText() {
    doTest("MethodText.hx");
  }

  @Test
  @DisplayName("property")
  public void testProperty() {
    doTest("Property.hx");
  }

  @Test
  @DisplayName("property static")
  public void testPropertyStatic() {
    doTest("PropertyStatic.hx");
  }

  @Test
  @DisplayName("property static text")
  public void testPropertyStaticText() {
    doTest("PropertyStaticText.hx");
  }

  @Test
  @DisplayName("property text")
  public void testPropertyText() {
    doTest("PropertyText.hx");
  }

  @Test
  @DisplayName("variable")
  public void testVariable() {
    doTest("Variable.hx");
  }

  @Test
  @DisplayName("variable static")
  public void testVariableStatic() {
    doTest("VariableStatic.hx");
  }

  @Test
  @DisplayName("variable static text")
  public void testVariableStaticText() {
    doTest("VariableStaticText.hx");
  }

  @Test
  @DisplayName("variable text")
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