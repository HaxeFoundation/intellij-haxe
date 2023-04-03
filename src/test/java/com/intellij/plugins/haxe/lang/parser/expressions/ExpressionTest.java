/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.lang.parser.expressions;

import com.intellij.plugins.haxe.lang.parser.HaxeParsingTestBase;

/**
 * @author fedor.korotkov
 */
public class ExpressionTest extends HaxeParsingTestBase {
  public ExpressionTest() {
    super("parsing", "haxe", "expressions");
  }

  public void testHaxe3() throws Throwable {
    doTest(true);
  }

  public void testOffers() throws Throwable {
    doTest(true);
  }

  public void testTest1() throws Throwable {
    doTest(true);
  }

  public void testTest2() throws Throwable {
    doTest(true);
  }

  public void testTest3() throws Throwable {
    doTest(true);
  }

  public void testTest4() throws Throwable {
    doTest(true);
  }

  public void testTest5() throws Throwable {
    doTest(true);
  }

  public void testTest6() throws Throwable {
    doTest(true);
  }

  public void testTest7() throws Throwable {
    doTest(true);
  }

  public void testTest8() throws Throwable {
    doTest(true);
  }

  public void testTivo_42() throws Throwable {
    doTest(true);
  }

  public void testNew() throws Throwable {
    doTest(true);
  }

  public void testJsonStringLiteralsIssue498() throws Throwable {
    doTest(true);
  }

  public void testTypeCheck() throws Throwable {
    doTest(true);
  }

  public void testIssue544() throws Throwable {
    doTest(true);
  }

  public void testOptionalVarOnFunctionType() throws Throwable {
    doTest(true);
  }

  public void testShiftLeftAssign() throws Throwable {
    doTest(true);
  }

  public void testShiftRightAssign() throws Throwable {
    doTest(true);
  }

  public void testNestedTypedClassesAssignment() throws Throwable {
    doTest(true);
  }

  public void testUnsignedShiftRightAssign() throws Throwable {
    doTest(true);
  }

  public void testComplexExpression() throws Throwable {
    doTest(true);
  }

  public void testStaticInitializer() throws Throwable {
    doTest(true);
  }

  public void testParenthesizedArrayAccess() throws Throwable {
    doTest(true);
  }

  public void testSequentialOperatorsShouldFailParsing() throws Throwable {
    doTest(true);
  }

  public void testTooManyMetas() throws Throwable {
    // github.com/HaxeFoundation/intellij-haxe/issues/81
    doTest(true);
  }

  public void testIsKeyword() throws Throwable {
    doTest(true);
  }

  public void testStrings() throws Throwable {
    doTest(true);
  }
}
