/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
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

import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;

/**
 * Created by ebishton on 6/2/17.
 */
public class ConditionalCompilationTest extends StatementTestBase {
  public ConditionalCompilationTest() {
    super("conditionalcompilation");
  }

  private void setDefines(String defines) {
    myProject.putUserData(HaxeConditionalExpression.DEFINES_KEY, defines);
  }

  public void testConditionalCompilation() throws Throwable {
    setDefines("neko,mydebug");
    doTest(true);
  }

  public void testConstantNotDefined() throws Throwable {
    doTest(true);
  }

  public void testConstantDefined() throws Throwable {
    setDefines("cpp");
    doTest(true);
  }

  public void testTrue() throws Throwable {
    // #if true
    doTest(true);
  }
  public void testNotTrue() throws Throwable {
    // #if !true
    doTest(true);
  }
  public void testTrueInParens() throws Throwable {
    // #if (true)
    doTest(true);
  }
  public void testFalse() throws Throwable {
    // #if false
    doTest(true);
  }
  public void testNotFalse() throws Throwable {
    // #if !false
    doTest(true);
  }

  public void testConstantInParens() throws Throwable {
    // #if (cpp)
    setDefines("cpp");
    doTest(true);
  }
  public void testNotConstantInParens() throws Throwable {
    // #if !(cpp)
    setDefines("cpp");
    doTest(true);
  }
  public void testNotConstantInsideParens() throws Throwable {
    // #if (!cpp)
    setDefines("cpp");
    doTest(true);
  }
  public void testNotConstantWhenNotDefined() throws Throwable {
    // #if(!cpp)
    doTest(true);
  }

}
