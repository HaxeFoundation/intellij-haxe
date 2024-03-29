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
package com.intellij.plugins.haxe.ide;

import com.intellij.execution.Platform;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.plugins.haxe.compilation.HaxeCompilerError;
import junit.framework.TestCase;
import org.junit.Test;

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

    TestCase.assertNotNull(compilerError);
    TestCase.assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    TestCase.assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    TestCase.assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    TestCase.assertEquals(5, compilerError.getLine());
    TestCase.assertEquals(0, compilerError.getColumn());
  }

  @Test
  public void testNMEErrorWin() {
    final String error = "src/Main.hx:5: characters 0-21 : Class not found : StringTools212";
    final String rootPath = "C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    TestCase.assertNotNull(compilerError);
    TestCase.assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    TestCase.assertEquals("C:/Users/fedor.korotkov/workspace/haxe-bubble-breaker/src/Main.hx", compilerError.getPath());
    TestCase.assertEquals("Class not found : StringTools212", compilerError.getErrorMessage());
    TestCase.assertEquals(5, compilerError.getLine());
    TestCase.assertEquals(0, compilerError.getColumn());
  }

  @Test
  public void testNMEErrorRelativeUnix() {
    final String error = "./HelloWorld.hx:12: characters 1-16 : Unknown identifier : addEvetListener";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    TestCase.assertNotNull(compilerError);
    TestCase.assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    TestCase.assertEquals("/trees/test/./HelloWorld.hx", compilerError.getPath());
    TestCase.assertEquals("Unknown identifier : addEvetListener", compilerError.getErrorMessage());
    TestCase.assertEquals(12, compilerError.getLine());
    TestCase.assertEquals(1, compilerError.getColumn());
  }

  @Test
  public void testNMEErrorAbsoluteUnix() {
    final String error = "/an/absolute/path/HelloWorld.hx:12: characters 1-16 : Unknown identifier : addEvetListener";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    TestCase.assertNotNull(compilerError);
    TestCase.assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    if (Platform.current() == Platform.UNIX) {
      TestCase.assertEquals("/an/absolute/path/HelloWorld.hx", compilerError.getPath());
    }
    TestCase.assertEquals("Unknown identifier : addEvetListener", compilerError.getErrorMessage());
    TestCase.assertEquals(12, compilerError.getLine());
    TestCase.assertEquals(1, compilerError.getColumn());
  }

  @Test
  public void testNMEErrorNoColumnUnix() {
    final String error = "hello/HelloWorld.hx:18: lines 18-24 : Interfaces cannot implement another interface (use extends instead)";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    TestCase.assertNotNull(compilerError);
    TestCase.assertEquals(CompilerMessageCategory.ERROR, compilerError.getCategory());
    TestCase.assertEquals("/trees/test/hello/HelloWorld.hx", compilerError.getPath());
    TestCase.assertEquals("Interfaces cannot implement another interface (use extends instead)", compilerError.getErrorMessage());
    TestCase.assertEquals(18, compilerError.getLine());
    TestCase.assertEquals(-1, compilerError.getColumn());
  }

  @Test
  public void testWarnings() {
    final String error = "hello/HelloWorld.hx:18: lines 18-24 : Warning : Danger, Will Robinson!";
    final String rootPath = "/trees/test";
    final HaxeCompilerError compilerError = HaxeCompilerError.create(rootPath, error, false);

    TestCase.assertNotNull(compilerError);
    TestCase.assertEquals(CompilerMessageCategory.WARNING, compilerError.getCategory());
    TestCase.assertEquals("/trees/test/hello/HelloWorld.hx", compilerError.getPath());
    TestCase.assertEquals("Danger, Will Robinson!", compilerError.getErrorMessage());
    TestCase.assertEquals(18, compilerError.getLine());
    TestCase.assertEquals(-1, compilerError.getColumn());
  }
}
