/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.resolve;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Resolve: expression")
public class HaxeExpressionResolveTest extends HaxeResolveHighlightingTestBase {

  @Override
  protected String getBasePath() {
    return "/resolve/expressions/";
  }

  @Test
  @DisplayName("string array expressions")
  public void testStringArrayExpressions() {
    doTest();
  }

  @Test
  @DisplayName("new with immediate")
  public void testNewWithImmediate() {
    doTest();
  }

  @Test
  @DisplayName("array access")
  public void testArrayAccess() {
    doTest();
  }

  @Test
  @DisplayName("dynamic extension")
  public void testDynamicExtension() {
    doTest();
  }

  @Test
  @DisplayName("enum extensions")
  public void testEnumExtensions() {
    doTest();
  }

  @Test
  @DisplayName("enum using meta extension")
  public void testEnumUsingMetaExtension() {
    doTest("colors/Color.hx");
  }
}
