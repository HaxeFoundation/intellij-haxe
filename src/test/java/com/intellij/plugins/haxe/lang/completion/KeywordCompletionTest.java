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
package com.intellij.plugins.haxe.lang.completion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Completion: keyword")
public class KeywordCompletionTest extends HaxeCompletionTestBase {
  public KeywordCompletionTest() {
    super("completion", "keywords");
  }

  @Test
  @DisplayName("else")
  public void testElse() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("empty")
  public void testEmpty() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("function 1 - after public in empty class")
  public void testFunction1() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("function 2 - after public below existing method")
  public void testFunction2() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("statement 1 - statement keywords in method body")
  public void testStatement1() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("statement 2 - excludes else and catch without context")
  public void testStatement2() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("line comment")
  public void testLineComment() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("doc comment")
  public void testDocComment() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("reference")
  public void testReference() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("inherit 1 - extends in class header")
  public void testInherit1() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("inherit 2 - implements after extends")
  public void testInherit2() throws Throwable {
    doTest();
  }
}
