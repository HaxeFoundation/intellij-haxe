/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

public class ImportDeclarationTest extends DeclarationTestBase {
  public ImportDeclarationTest() {
    super("import");
  }

  public void testEmpty() throws Throwable {
      doTest(true);
    assertEmpty("");
  }

  public void testEmpty182() throws Throwable {
      doTest(true);
  }

  public void testMulti() throws Throwable {
    doTest(true);
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testIn() throws Throwable {
    doTest(true);
  }

  public void testWildcard() throws Throwable {
    doTest(true);
  }
}
