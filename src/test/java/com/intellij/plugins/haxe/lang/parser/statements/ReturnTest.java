/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.lang.parser.statements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author fedor.korotkov
 */
@DisplayName("Parsing: return")
public class ReturnTest extends StatementTestBase {
  public ReturnTest() {
    super("return");
  }

  @Test
  @DisplayName("value")
  public void testValue() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("void")
  public void testVoid() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("object literal")
  public void testObjectLiteral() throws Throwable {
    // github.com/tivo/intellij-haxe/issues/278
    doTest(true);
  }

  @Test
  @DisplayName("ternary expression")
  public void testTernaryExpression() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("block statement")
  public void testBlockStatement() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("variable declaration")
  public void testVariableDeclaration() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("array comprehension")
  public void testArrayComprehension() throws Throwable {
    doTest(true);
  }
}
