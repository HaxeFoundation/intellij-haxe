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
package com.intellij.plugins.haxe.lang.parser.tests;

import org.junit.Test;

public class UnitTest extends TestsTestBase {
  public UnitTest() {
    super("unit/src/unit");
  }

  @Test
  public void testTest() {
    doTest(true);
  }

  @Test
  public void testTestOps() {
    doTest(true);
  }
}
