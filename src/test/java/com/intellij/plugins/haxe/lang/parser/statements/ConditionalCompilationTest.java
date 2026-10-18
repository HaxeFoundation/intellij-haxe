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

import com.intellij.plugins.haxe.util.HaxeTestDefines;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Created by ebishton on 6/2/17.
 */
@DisplayName("Parsing: conditional compilation")
public class ConditionalCompilationTest extends StatementTestBase {
  public ConditionalCompilationTest() {
    super("conditionalcompilation");
  }

  private void setDefines(String defines) {
    HaxeTestDefines.set(getProject(), defines);
  }

  @Test
  @DisplayName("conditional compilation")
  public void testConditionalCompilation() throws Throwable {
    setDefines("neko,mydebug");
    doTest(true);
  }

  @Test
  @DisplayName("dotted conditional identifiers")
  public void testDottedConditionalIdentifiers() throws Throwable {
    setDefines("vm.neko,my.debug");
    doTest(true);
  }

  @Test
  @DisplayName("constant not defined")
  public void testConstantNotDefined() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("constant defined")
  public void testConstantDefined() throws Throwable {
    setDefines("cpp");
    doTest(true);
  }

  @Test
  @DisplayName("flag define compares as its compiler value 1")
  public void testFlagDefineVersionComparison() throws Throwable {
    // a bare flag carries "1" (never a boolean): 1 < 9.0.0 - first branch active
    setDefines("myVersion");
    doTest(true);
  }

  @Test
  @DisplayName("if true block")
  public void testTrue() throws Throwable {
    // #if true
    doTest(true);
  }

  @Test
  @DisplayName("if not true block")
  public void testNotTrue() throws Throwable {
    // #if !true
    doTest(true);
  }

  @Test
  @DisplayName("true in parens")
  public void testTrueInParens() throws Throwable {
    // #if (true)
    doTest(true);
  }

  @Test
  @DisplayName("false")
  public void testFalse() throws Throwable {
    // #if false
    doTest(true);
  }

  @Test
  @DisplayName("not false")
  public void testNotFalse() throws Throwable {
    // #if !false
    doTest(true);
  }

  @Test
  @DisplayName("constant in parens")
  public void testConstantInParens() throws Throwable {
    // #if (cpp)
    setDefines("cpp");
    doTest(true);
  }

  @Test
  @DisplayName("not constant in parens")
  public void testNotConstantInParens() throws Throwable {
    // #if !(cpp)
    setDefines("cpp");
    doTest(true);
  }

  @Test
  @DisplayName("not constant inside parens")
  public void testNotConstantInsideParens() throws Throwable {
    // #if (!cpp)
    setDefines("cpp");
    doTest(true);
  }

  @Test
  @DisplayName("constant set false")
  public void testConstantSetFalse() throws Throwable {
    setDefines("cpp=false");
    doTest(true);
  }

  @Test
  @DisplayName("string equals string")
  public void testStringEqualsString() throws Throwable {
    // #if ("string" =="string")
    doTest(true);
  }

  @Test
  @DisplayName("string not equal string")
  public void testStringNotEqualString() throws Throwable {
    // #if ("string" != "other")
    doTest(true);
  }

  @Test
  @DisplayName("string less than string")
  public void testStringLessThanString() throws Throwable {
    //  #if ("this" < "that")
    doTest(true);
  }

  @Test
  @DisplayName("string greater than value")
  public void testStringGreaterThanValue() throws Throwable {
    //  #if ("this" > 1)  -- False!
    doTest(true);
  }

  @Test
  @DisplayName("value equal boolean")
  public void testValueEqualBoolean() throws Throwable {
    //  #if ( 1 == true )
    doTest(true);
  }

  @Test
  @DisplayName("complex multi expression with parens")
  public void testComplexMultiExpressionWithParens() throws Throwable {
    // #if !( (((!cpp) && js) && haxe_ver < 3.5 ) || (haxe_ver >= 3.2 && !cpp && "foo" != "bar"))
    setDefines("cpp=false,js=true,haxe-ver=\"3.2\"");
    doTest(true);
  }

  @Test
  @DisplayName("comparison operators")
  public void testComparisonOperators() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("actuate lib example from manual")
  public void testActuateLibExampleFromManual() throws Throwable {
    setDefines("actuate=1.8.7");
    doTest(true);
  }
}
