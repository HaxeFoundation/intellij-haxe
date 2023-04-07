/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Eric Bishton
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
package com.intellij.plugins.haxe.lang.parser.declarations;

import org.junit.Test;

public class VarOrConstDeclarationTest extends DeclarationTestBase {
  public VarOrConstDeclarationTest() {
    super("variable");
  }

  @Test
  public void testAssign() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAssignArray() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAssignArrayOfObjectLiteral() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAssignArrayOfObjectLiteralWithExtraComma() throws Throwable {
    doTest(true);
  }

  @Test
  public void testBoolExpression() throws Throwable {
    doTest(true);
  }

  @Test
  public void testExpression() throws Throwable {
    doTest(true);
  }

  @Test
  public void testSimple() throws Throwable {
    doTest(true);
  }

  @Test
  public void testTemplate() throws Throwable {
    doTest(true);
  }

  @Test
  public void testConstants() throws Throwable {
    doTest(true);
  }

  @Test
  public void testTivo_42() throws Throwable {
    doTest(true);
  }

  @Test
  public void testFatArrowMapLiteral() throws Throwable {
    doTest(true);
  }

  @Test
  public void testFatArrowMapLiteralWithExtraComma() throws Throwable {
    doTest(true);
  }

  @Test
  public void testMapLiteralComprehensionFor() throws Throwable {
    doTest(true);
  }

  @Test
  public void testMapLiteralComprehensionWhile() throws Throwable {
    doTest(true);
  }

  @Test
  public void testMapLiteralComprehensionForFor() throws Throwable {
    doTest(true);
  }

  @Test
  public void testMapLiteralComprehensionWhileWhile() throws Throwable {
    doTest(true);
  }

  @Test
  public void testMapLiteralComprehensionForWhile() throws Throwable {
    doTest(true);
  }

  @Test
  public void testMapLiteralComprehensionWhileFor() throws Throwable {
    doTest(true);
  }

  @Test
  public void testCommaAssignmentRequiringSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  public void testCommaAssignmentRequiringMissingSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  public void testCommaAssignmentNotRequiringSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAtConstInstantiationString() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAtConstInstantiationInt() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAssignParameterizedTypeInstance() throws Throwable {
    doTest(true);
  }

  @Test
  public void testStructureInitialization() throws Throwable {
    doTest(true);
  }

  @Test
  public void testStructureInitWithStringVarName() throws Throwable {
    doTest(true);
  }
}
