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
package com.intellij.plugins.haxe.lang.completion;

import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class KeywordCompletionTest extends HaxeCompletionTestBase {
  public KeywordCompletionTest() {
    super("completion", "keywords");
  }

  @Test
  public void testElse() throws Throwable {
    doTest();
  }

  @Test
  public void testEmpty() throws Throwable {
    doTest();
  }

  @Test
  public void testFunction1() throws Throwable {
    doTest();
  }

  @Test
  public void testFunction2() throws Throwable {
    doTest();
  }

  @Test
  public void testStatement1() throws Throwable {
    doTest();
  }

  @Test
  public void testStatement2() throws Throwable {
    doTest();
  }

  @Test
  public void testReference() throws Throwable {
    doTest();
  }

  @Test
  public void testInherit1() throws Throwable {
    doTest();
  }

  @Test
  public void testInherit2() throws Throwable {
    doTest();
  }
}
