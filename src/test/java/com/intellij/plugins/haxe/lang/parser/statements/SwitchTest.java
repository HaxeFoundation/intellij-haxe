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
package com.intellij.plugins.haxe.lang.parser.statements;

import org.junit.Test;

/**
 * @author fedor.korotkov
 */
public class SwitchTest extends StatementTestBase {
  public SwitchTest() {
    super("switch");
  }

  @Test
  public void testSimple() throws Throwable {
    doTest(true);
  }

  @Test
  public void testCaseWithEmptyBrackets() throws Throwable {
    doTest(true);
  }

  @Test
  public void testFatArrowExpression() throws Throwable {
    doTest(true);
  }

  @Test
  public void testExtractorExpression() throws Throwable {
    doTest(true);
  }
  @Test
  public void testChainedExtractorsExpression() throws Throwable {
    doTest(true);
  }
}
