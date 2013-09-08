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

import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.plugins.haxe.compilation.HaxeCompilerError;
import junit.framework.TestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeCompilerErrorParsingTest extends TestCase {
  // XXX this is an unsafe test to do on a non-windows box
  public void disabledtestUserProperiesErrorWin() {
    final String error =
      "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx:5: characters 0-21 : Class not found : StringTools212";
    final String rootPath = "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    assertEquals(5, compilerError.getLine());
    assertEquals(0, compilerError.getColumn());
  }

  public void testNMEErrorWin() {
    final String error = "src/Main.hx:5: characters 0-21 : Class not found : StringTools212";
    final String rootPath = "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    assertEquals(5, compilerError.getLine());
    assertEquals(0, compilerError.getColumn());
  }

  public void testNMEErrorRelativeUnix() {
    final String error = "./HelloWorld.hx:12: characters 1-16 : Unknown identifier : addEvetListener";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    assertEquals("/trees/test/./HelloWorld.hx", compilerError.getPath());
    assertEquals("Unknown identifier : addEvetListener", compilerError.getErrorMessage());
    assertEquals(12, compilerError.getLine());
    assertEquals(1, compilerError.getColumn());
  }

  public void testNMEErrorAbsoluteUnix() {
    final String error = "/an/absolute/path/HelloWorld.hx:12: characters 1-16 : Unknown identifier : addEvetListener";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    assertEquals("/an/absolute/path/HelloWorld.hx", compilerError.getPath());
    assertEquals("Unknown identifier : addEvetListener", compilerError.getErrorMessage());
    assertEquals(12, compilerError.getLine());
    assertEquals(1, compilerError.getColumn());
  }

  public void testNMEErrorNoColumnUnix() {
    final String error = "hello/HelloWorld.hx:18: lines 18-24 : Interfaces cannot implement another interface (use extends instead)";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    assertEquals("/trees/test/hello/HelloWorld.hx", compilerError.getPath());
    assertEquals("Interfaces cannot implement another interface (use extends instead)", compilerError.getErrorMessage());
    assertEquals(18, compilerError.getLine());
    assertEquals(-1, compilerError.getColumn());
  }

  public void testWarnings() {
    final String error = "hello/HelloWorld.hx:18: lines 18-24 : Warning : Danger, Will Robinson!";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    assertNotNull(compilerError);
    assertEquals(CompilerMessageCategory.WARNING, compilerError.getCategory());
    assertEquals("/trees/test/hello/HelloWorld.hx", compilerError.getPath());
    assertEquals("Danger, Will Robinson!", compilerError.getErrorMessage());
    assertEquals(18, compilerError.getLine());
    assertEquals(-1, compilerError.getColumn());
  }
}
