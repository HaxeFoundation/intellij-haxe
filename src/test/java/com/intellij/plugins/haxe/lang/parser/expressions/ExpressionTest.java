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
package com.intellij.plugins.haxe.lang.parser.expressions;

import com.intellij.plugins.haxe.lang.parser.HaxeParsingTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author fedor.korotkov
 */
@DisplayName("Parsing: expression")
public class ExpressionTest extends HaxeParsingTestBase {
  public ExpressionTest() {
    super("parsing", "haxe", "expressions");
  }

  @Test
  @DisplayName("haxe 3")
  public void testHaxe3() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("unbound identifier")
  public void testUnboundIdentifier() throws Throwable {
    doTest(true, true);
  }

  @Test
  @DisplayName("offers")
  public void testOffers() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 1 - local function declared and called")
  public void testTest1() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 2 - closure capturing a local var")
  public void testTest2() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 3 - chained calls with field and array access")
  public void testTest3() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 4 - arithmetic precedence and unary operators")
  public void testTest4() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 5 - new in call chains and bitwise or")
  public void testTest5() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 6 - object and regex literals")
  public void testTest6() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 7 - switch as return value with casts")
  public void testTest7() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("test 8 - if and try as expressions")
  public void testTest8() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("tivo 42")
  public void testTivo_42() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("new")
  public void testNew() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("json string literals issue 498")
  public void testJsonStringLiteralsIssue498() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("type check")
  public void testTypeCheck() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("issue 544")
  public void testIssue544() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("optional var on function type")
  public void testOptionalVarOnFunctionType() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("shift left assign")
  public void testShiftLeftAssign() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("shift right assign")
  public void testShiftRightAssign() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("nested typed classes assignment")
  public void testNestedTypedClassesAssignment() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("unsigned shift right assign")
  public void testUnsignedShiftRightAssign() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("complex expression")
  public void testComplexExpression() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("static initializer")
  public void testStaticInitializer() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("parenthesized array access")
  public void testParenthesizedArrayAccess() throws Throwable {
    doTest(true);
  }
  @Test
  @DisplayName("safe cast expressions")
  public void testSafeCastExpressions() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("sequential operators should fail parsing")
  public void testSequentialOperatorsShouldFailParsing() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("too many metas")
  public void testTooManyMetas() throws Throwable {
    // github.com/HaxeFoundation/intellij-haxe/issues/81
    doTest(true);
  }

  @Test
  @DisplayName("is keyword")
  public void testIsKeyword() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("strings")
  public void testStrings() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("inline call expression")
  public void testInlineCallExpression() throws Throwable {
    doTest(true);
  }

  @Test
  @DisplayName("regular expressions")
  public void testRegularExpressions() throws Throwable {
    doTest(true, true);
  }

  @Test
  @DisplayName("xml literals")
  public void testXmlLiterals() throws Throwable {
    doTest(true);
  }
}
