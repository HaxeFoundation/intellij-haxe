/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.compilation.HaxeCompilerError;
import junit.framework.TestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerErrorParsingTest extends TestCase {
  public void testUserProperiesErrorWin() {
    final String error =
      "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx:5: characters 0-21 : Class not found : StringTools212";
    final String rootPath = "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    assertEquals(5, compilerError.getLine());
  }

  public void testNMEErrorWin() {
    final String error = "src/Main.hx:5: characters 0-21 : Class not found : StringTools212";
    final String rootPath = "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    assertEquals(5, compilerError.getLine());
  }
}
