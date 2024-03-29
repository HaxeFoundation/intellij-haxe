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
package com.intellij.plugins.haxe.lang.parser.declarations;

import org.junit.Test;

public class FunctionDeclarationTest extends DeclarationTestBase {
  public FunctionDeclarationTest() {
    super("function");
  }

  @Test
  public void testBadParameters() throws Throwable {
    doTest(true);
  }

  @Test
  public void testConstructor() throws Throwable {
    doTest(true);
  }

  @Test
  public void testNoReturnType() throws Throwable {
    doTest(true);
  }

  @Test
  public void testParameter() throws Throwable {
    doTest(true);
  }

  @Test
  public void testParameters() throws Throwable {
    doTest(true);
  }

  @Test
  public void testSimple() throws Throwable {
    doTest(true);
  }

  @Test
  public void testGeneric() throws Throwable {
    doTest(true);
  }

  @Test
  public void testMacro() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrayAccess() throws Throwable {
    doTest(true);
  }

  @Test
  public void testReturnStatement() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrowFnSingleArgBareAssignment() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrowFnSingleArgParensAssignment() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrowFnTwoArgAssignment() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrowFnSingleArgBareNested() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrowFnSingleArgParensNested() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrowFnTwoArgNested() throws Throwable {
    doTest(true);
  }

  @Test
  public void testArrowFnZeroArg() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAbstractPrototypeDeclarations() throws Throwable {
    doTest(true);
  }

  @Test
  public void testNamedNestedFunction() throws Throwable {
    doTest(true);
  }

  @Test
  public void testCustomMetadataEmpty() throws Throwable {
    doTest(true);
  }

  @Test
  public void testCustomMetadataWithArgs() throws Throwable {
    doTest(true);
  }

  @Test
  public void testCustomMetadataBare() throws Throwable {
    doTest(true);
  }

  @Test
  public void testExternFunctionDeclarationSimple() throws Throwable {
    doTest(true);
  }

  @Test
  public void testAnonymousLocalFunctionAsValue() throws Throwable {
    doTest(true);
  }
}
