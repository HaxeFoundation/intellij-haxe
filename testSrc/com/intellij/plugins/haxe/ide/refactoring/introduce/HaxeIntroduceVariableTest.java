/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
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
package com.intellij.plugins.haxe.ide.refactoring.introduce;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIntroduceVariableTest extends HaxeIntroduceTestBase {
  @Override
  protected String getBasePath() {
    return "/refactoring/introduceVariable/";
  }

  @Override
  protected HaxeIntroduceHandler createHandler() {
    return new HaxeIntroduceVariableHandler();
  }

  public void testAfterStatement() throws Throwable {
    doTest();
  }

  public void testAlone() throws Throwable {
    doTest();
  }

  public void testReplaceAll1() throws Throwable {
    doTest();
  }

  public void testReplaceAll2() throws Throwable {
    doTest();
  }

  public void testReplaceAll3() throws Throwable {
    doTestInplace(null);
  }

  public void testReplaceOne1() throws Throwable {
    doTest(null, false);
  }

  public void testSuggestName1() throws Throwable {
    doTestSuggestions(HaxeCallExpression.class, "test");
  }

  public void testSuggestName2() throws Throwable {
    doTestSuggestions(HaxeCallExpression.class, "test1");
  }

  public void testReplaceConstant() throws Throwable {
    doTestInplace(null, false, null);
  }

  public void testReplaceConstantAll() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractRegex() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractThis() throws Throwable {
    doTestInplace(null, false, null);
  }

  public void testExtractString() throws Throwable {
    doTestInplace(null, false, null);
  }

  public void testExtractCall() throws Throwable {
    doTestInplace(null, true, "pi");
  }

  public void testExtractAnonymousFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractNamedFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractArrowFunctionWithCurlyBrackets() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractSimpleArrowFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractFloat() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractAnonymousStructure() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractVariableDeclaration() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractMapLiteral() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractMapLiteralWithFunction() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractArrayLiteral() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractArrayComprehension() throws Throwable {
    doTestInplace(null, true, null);
  }

  public void testExtractRegex2() throws Throwable {
    doTestInplace(null, true, null);
  }
}
