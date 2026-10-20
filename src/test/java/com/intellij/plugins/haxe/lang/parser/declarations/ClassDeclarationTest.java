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
package com.intellij.plugins.haxe.lang.parser.declarations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Parsing: class declaration")
public class ClassDeclarationTest extends DeclarationTestBase {
  public ClassDeclarationTest() {
    super("class");
  }

  @Test
  @DisplayName("extends")
  public void testExtends() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("extends implements")
  public void testExtendsImplements() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("final")
  public void testFinal() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("overload modifier order")
  public void testOverloadModifierOrder() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("implements")
  public void testImplements() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("multiextends")
  public void testMultiextends() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("simple")
  public void testSimple() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("constraint")
  public void testConstraint() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("native random")
  public void testNativeRandom() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("array utils")
  public void testArrayUtils() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("full of macro")
  public void testFullOfMacro() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("native annotation issue 490")
  public void testNativeAnnotationIssue490() throws Throwable {
    doTest(true);
  }
}
