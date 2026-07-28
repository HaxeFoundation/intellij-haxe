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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Parsing: function declaration")
public class FunctionDeclarationTest extends DeclarationTestBase {
  public FunctionDeclarationTest() {
    super("function");
  }

  @Test
  @DisplayName("bad parameters")
  public void testBadParameters() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("constructor")
  public void testConstructor() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("no return type")
  public void testNoReturnType() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("parameter")
  public void testParameter() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("parameters")
  public void testParameters() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("simple")
  public void testSimple() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("generic")
  public void testGeneric() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("macro")
  public void testMacro() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("array access")
  public void testArrayAccess() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("return statement")
  public void testReturnStatement() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("arrow fn single arg bare assignment")
  public void testArrowFnSingleArgBareAssignment() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("arrow fn single arg parens assignment")
  public void testArrowFnSingleArgParensAssignment() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("arrow fn two arg assignment")
  public void testArrowFnTwoArgAssignment() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("arrow fn single arg bare nested")
  public void testArrowFnSingleArgBareNested() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("arrow fn single arg parens nested")
  public void testArrowFnSingleArgParensNested() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("arrow fn two arg nested")
  public void testArrowFnTwoArgNested() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("arrow fn zero arg")
  public void testArrowFnZeroArg() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("abstract prototype declarations")
  public void testAbstractPrototypeDeclarations() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("named nested function")
  public void testNamedNestedFunction() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("custom metadata empty")
  public void testCustomMetadataEmpty() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("custom metadata with args")
  public void testCustomMetadataWithArgs() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("custom metadata bare")
  public void testCustomMetadataBare() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("extern function declaration simple")
  public void testExternFunctionDeclarationSimple() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("anonymous local function as value")
  public void testAnonymousLocalFunctionAsValue() throws Throwable {
    doTest(true);
  }
  @Test
  @DisplayName("block body with semi")
  public void testBlockBodyWithSemi() throws Throwable {
    doTest(true, true);
  }
}
