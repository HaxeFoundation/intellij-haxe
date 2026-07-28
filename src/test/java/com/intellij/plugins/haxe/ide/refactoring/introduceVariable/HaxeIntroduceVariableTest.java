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
package com.intellij.plugins.haxe.ide.refactoring.introduceVariable;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Refactoring: introduce variable")
public class HaxeIntroduceVariableTest extends HaxeIntroduceTestBase {
  @Override
  protected String getBasePath() {
    return "/refactoring/introduceVariable/";
  }

  @Override
  protected HaxeIntroduceHandler createHandler() {
    return new HaxeIntroduceVariableHandler();
  }

  @Test
  @DisplayName("after statement")
  public void testAfterStatement() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("alone")
  public void testAlone() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("replace all 1 - literal occurrences in loop and init")
  public void testReplaceAll1() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("replace all 2 - ternary expression occurrences")
  public void testReplaceAll2() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("replace all 3 - ternary occurrences with inplace rename")
  public void testReplaceAll3() throws Throwable {
    doTestInplace(null);
  }

  @Test
  @DisplayName("replace one 1 - only the selected occurrence")
  public void testReplaceOne1() throws Throwable {
    doTest(null, false);
  }

  @Test
  @DisplayName("suggest name 1 - name derived from getTest call")
  public void testSuggestName1() throws Throwable {
    doTestSuggestions(HaxeCallExpression.class, "test");
  }

  @Test
  @DisplayName("suggest name 2 - name derived from test call")
  public void testSuggestName2() throws Throwable {
    doTestSuggestions(HaxeCallExpression.class, "test1");
  }

  @Test
  @DisplayName("replace constant")
  public void testReplaceConstant() throws Throwable {
    doTestInplace(null, false, null);
  }

  @Test
  @DisplayName("replace constant all")
  public void testReplaceConstantAll() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract regex")
  public void testExtractRegex() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract this")
  public void testExtractThis() throws Throwable {
    doTestInplace(null, false, null);
  }

  @Test
  @DisplayName("extract string")
  public void testExtractString() throws Throwable {
    doTestInplace(null, false, null);
  }

  @Test
  @DisplayName("extract call")
  public void testExtractCall() throws Throwable {
    doTestInplace(null, true, "pi");
  }

  @Test
  @DisplayName("extract anonymous function")
  public void testExtractAnonymousFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract named function")
  public void testExtractNamedFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract arrow function with curly brackets")
  public void testExtractArrowFunctionWithCurlyBrackets() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract simple arrow function")
  public void testExtractSimpleArrowFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract float")
  public void testExtractFloat() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract anonymous structure")
  public void testExtractAnonymousStructure() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract map literal")
  public void testExtractMapLiteral() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract map literal with function")
  public void testExtractMapLiteralWithFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract array literal")
  public void testExtractArrayLiteral() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract array comprehension")
  public void testExtractArrayComprehension() throws Throwable {
    doTestInplace(null, true, null);
  }

  @Test
  @DisplayName("extract regex 2 - selection inside a regex literal")
  public void testExtractRegex2() throws Throwable {
    doTestInplace(null, true, null);
  }
}
