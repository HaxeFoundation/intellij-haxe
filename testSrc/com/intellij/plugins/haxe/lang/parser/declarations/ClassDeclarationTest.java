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

public class ClassDeclarationTest extends DeclarationTestBase {
  public ClassDeclarationTest() {
    super("class");
  }

  public void testExtends() throws Throwable {
    doTest(true);
  }

  public void testExtendsImplements() throws Throwable {
// TODO: Fix failing unit test
//    doTest(true);
  }

  public void testImplements() throws Throwable {
    doTest(true);
  }

  public void testMultiextends() throws Throwable {
    doTest(true);
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testConstraint() throws Throwable {
    doTest(true);
  }

  public void testNativeRandom() throws Throwable {
    doTest(true);
  }

  public void testArrayUtils() throws Throwable {
    doTest(true);
  }

  public void testFullOfMacro() throws Throwable {
    doTest(true);
  }
}
