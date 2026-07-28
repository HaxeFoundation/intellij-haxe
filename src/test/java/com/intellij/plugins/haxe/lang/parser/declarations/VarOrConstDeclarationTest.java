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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Parsing: var or const declaration")
public class VarOrConstDeclarationTest extends DeclarationTestBase {
  public VarOrConstDeclarationTest() {
    super("variable");
  }

  @Test
  @DisplayName("assign")
  public void testAssign() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("assign array")
  public void testAssignArray() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("assign array of object literal")
  public void testAssignArrayOfObjectLiteral() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("assign array of object literal with extra comma")
  public void testAssignArrayOfObjectLiteralWithExtraComma() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("bool expression")
  public void testBoolExpression() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("expression")
  public void testExpression() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("simple")
  public void testSimple() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("template")
  public void testTemplate() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("constants")
  public void testConstants() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("tivo 42")
  public void testTivo_42() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("fat arrow map literal")
  public void testFatArrowMapLiteral() throws Throwable {
    doTest(true);
  }
  @Test
  @DisplayName("macro fat arrow expression")
  public void testMacroFatArrowExpression() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("fat arrow map literal with extra comma")
  public void testFatArrowMapLiteralWithExtraComma() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("map literal comprehension for")
  public void testMapLiteralComprehensionFor() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("map literal comprehension while")
  public void testMapLiteralComprehensionWhile() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("map literal comprehension for for")
  public void testMapLiteralComprehensionForFor() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("map literal comprehension while while")
  public void testMapLiteralComprehensionWhileWhile() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("map literal comprehension for while")
  public void testMapLiteralComprehensionForWhile() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("map literal comprehension while for")
  public void testMapLiteralComprehensionWhileFor() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("comma assignment requiring semicolon")
  public void testCommaAssignmentRequiringSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("comma assignment requiring missing semicolon")
  public void testCommaAssignmentRequiringMissingSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("comma assignment not requiring semicolon")
  public void testCommaAssignmentNotRequiringSemicolon() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("at const instantiation string")
  public void testAtConstInstantiationString() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("at const instantiation int")
  public void testAtConstInstantiationInt() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("assign parameterized type instance")
  public void testAssignParameterizedTypeInstance() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("structure initialization")
  public void testStructureInitialization() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("structure init with string var name")
  public void testStructureInitWithStringVarName() throws Throwable {
    doTest(true);
  }
}
