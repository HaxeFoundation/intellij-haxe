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

  public void testDottedConditionalIdentifiers() throws Throwable {
    setDefines("vm.neko,my.debug");
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
  public void testConstantSetFalse() throws Throwable {
    setDefines("cpp=false");
    doTest(true);
  }
  public void testConstantSetTrue() throws Throwable {
    setDefines("cpp=true");
    doTest(true);
  }
  public void testConstantSetToZeroString() throws Throwable {
    setDefines("cpp=\"0\"");
    doTest(true);
  }
  public void testConstantSetToNonZeroString() throws Throwable {
    setDefines("cpp=\"2.1\"");
    doTest(true);
  }
  public void testConstantSetToZeroValue() throws Throwable {
    setDefines("cpp=0");
    doTest(true);
  }
  public void testConstantSetToNonZeroValue() throws Throwable {
    setDefines("cpp=1.2");
    doTest(true);
  }

  public void testStringEqualsString() throws Throwable {
    // #if ("string" =="string")
    doTest(true);
  }
  public void testStringNotEqualString() throws Throwable {
    // #if ("string" != "other")
    doTest(true);
  }
  public void testStringLessThanString() throws Throwable {
    //  #if ("this" < "that")
    doTest(true);
  }
  public void testStringGreaterThanValue() throws Throwable {
    //  #if ("this" > 1)  -- False!
    doTest(true);
  }
  public void testValueEqualBoolean() throws Throwable {
    //  #if ( 1 == true )
    doTest(true);
  }
  public void testConstantOrConstant() throws Throwable {
    // When either one or both are set, we should have the same result.
    setDefines("cpp"); // And not js
    doTest(true);
    setDefines("js"); // And not cpp
    doTest(true);
    setDefines("js,cpp");
    doTest(true);
    setDefines("js=1,cpp=2.3");
    doTest(true);
    setDefines("js=\"foo\"");
  }
  public void testConstantAndConstant() throws Throwable {
    setDefines("cpp,js=false");
    doTest(true);
    setDefines("cpp=false,js");
    doTest(true);
    setDefines("cpp");
    doTest(true);
    setDefines("js");
    doTest(true);
  }
  public void testComplexMultiExpressionWithParens() throws Throwable {
    // #if !( (((!cpp) && js) && haxe_ver < 3.5 ) || (haxe_ver >= 3.2 && !cpp && "foo" != "bar"))
    setDefines("cpp=false,js=true,haxe-ver=\"3.2\"");
    doTest(true);
  }
  public void testComparisonOperators() throws Throwable {
    doTest(true);
  }

  public void testActuateLibExampleFromManual() throws Throwable {
    setDefines("actuate=1.8.7");
    doTest(true);
  }
}
