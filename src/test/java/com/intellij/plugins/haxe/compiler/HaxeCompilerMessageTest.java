/*
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
package com.intellij.plugins.haxe.compiler;

import com.intellij.execution.Platform;
import com.intellij.plugins.haxe.compilation.HaxeCompilerMessage.Category;
import com.intellij.plugins.haxe.compilation.HaxeCompilerMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by ebishton on 10/29/16.
 */
@DisplayName("Compiler: message")
public class HaxeCompilerMessageTest {

  private void doTest(String output, Category cat, String msg, String path, int line, int col) throws Throwable {
    HaxeCompilerMessage e = HaxeCompilerMessage.create("", output);
    assertEquals(cat, e.getCategory());
    assertEquals(msg, e.getMessage());
    assertEquals(path, e.getPath());
    assertEquals(line, e.getLine());
    assertEquals(col, e.getColumn());
  }

  /** The rooted form the build window uses: relative source paths join the project root. */
  private void doRootedTest(String rootPath, String output, Category cat, String msg, String path, int line, int col) {
    HaxeCompilerMessage e = HaxeCompilerMessage.create(rootPath, output, false);
    assertNotNull(e);
    assertEquals(cat, e.getCategory());
    assertEquals(msg, e.getMessage());
    assertEquals(path, e.getPath());
    assertEquals(line, e.getLine());
    assertEquals(col, e.getColumn());
  }

  private void doInfoTest(String compilerOutput) throws Throwable {
    doTest(compilerOutput, Category.INFORMATION, compilerOutput, null, -1, -1);
  }

  private void doWarningTest(String compilerOutput) throws Throwable {
    doTest(compilerOutput, Category.WARNING, compilerOutput.trim(), null, -1, -1);
  }

  private void doErrorTest(String compilerOutput, String expected) throws Throwable {
    doTest(compilerOutput, Category.ERROR, expected, null, -1, -1);
  }

  // hxcpp 3.3 link message
  @Test
  @DisplayName("link message")
  public void testLinkMessage() throws Throwable {
    doInfoTest(" -  - Link : ApplicationMain: xcrun");
  }

  @Test
  @DisplayName("link message 2 - with trailing newline")
  public void testLinkMessage2() throws Throwable {
    doInfoTest(" - Link : ApplicationMain: xcrun\n");
  }

  // hxcpp 3.3 compile message.
  @Test
  @DisplayName("compile message")
  public void testCompileMessage() throws Throwable {
    doInfoTest(" - Compile : src/ApplicationMain.hx");
  }

  @Test
  @DisplayName("compiling message")
  public void testCompilingMessage() throws Throwable {
    doInfoTest(" - Compiling src/ApplicationMain.hx : <some_message>");
  }

  @Test
  @DisplayName("generating message")
  public void testGeneratingMessage() throws Throwable {
    doInfoTest("Generating out/ApplicationMain.cpp : <some_message>");
  }

  @Test
  @DisplayName("library not installed")
  public void testLibraryNotInstalled() throws Throwable {
    String compilerOutput = "Error: Library Flixel is not installed. Please run haxelib...";
    doTest(compilerOutput, Category.ERROR, "Library Flixel is not installed. Please run haxelib...", null, -1, -1);
  }

  // Hxcpp 3.3
  @Test
  @DisplayName("library not installed hxcpp")
  public void testLibraryNotInstalledHxcpp() throws Throwable {
    String compilerOutput = "Library hxcpp is not installed";
    doErrorTest(compilerOutput, compilerOutput);
  }

  @Test
  @DisplayName("generic error")
  public void testGenericError() throws Throwable {
    String compilerOutput = "Unknown Error : This is an error message.";
    String expected = " (Unknown Error) This is an error message.";
    doErrorTest(compilerOutput, expected);
  }

  @Test
  @DisplayName("lines error")
  public void testLinesError() throws Throwable {
    String compilerOutput = "Test.hx:4: lines 4-10 : Invalid -main : Test does not have static function main";
    doTest(compilerOutput, Category.ERROR, "Invalid -main : Test does not have static function main",
           "Missing file: /Test.hx", 4, -1);
  }

  @Test
  @DisplayName("hxcpp build failure")
  public void testHxcppBuildFailure() throws Throwable {
    String compilerOutput = "Error: Build failed";
    String expected = "Build failed";
    doErrorTest(compilerOutput, expected);
  }

  @Test
  @DisplayName("unexpected character")
  public void testUnexpectedCharacter() throws Throwable {
    String compilerOutput = "Test.hx:4: characters 6-7 : Unexpected %";
    String expected = "Unexpected %";
    doTest(compilerOutput, Category.ERROR, expected, "Missing file: /Test.hx", 4, 6);
  }

  // --- rooted messages: relative paths join the project root ---

  @Test
  @DisplayName("rooted error with windows root")
  public void testRootedErrorWithWindowsRoot() {
    doRootedTest("C:/Users/username/workspace/project",
                 "src/Main.hx:5: characters 0-21 : Class not found : StringTools212",
                 Category.ERROR, "Class not found : StringTools212",
                 "C:/Users/username/workspace/project/src/Main.hx", 5, 0);
  }

  @Test
  @DisplayName("rooted error with dot relative path")
  public void testRootedErrorWithDotRelativePath() {
    doRootedTest("/trees/test",
                 "./HelloWorld.hx:12: characters 1-16 : Unknown identifier : addEvetListener",
                 Category.ERROR, "Unknown identifier : addEvetListener",
                 "/trees/test/./HelloWorld.hx", 12, 1);
  }

  @Test
  @DisplayName("rooted error without a column")
  public void testRootedErrorWithoutAColumn() {
    doRootedTest("/trees/test",
                 "hello/HelloWorld.hx:18: lines 18-24 : Interfaces cannot implement another interface (use extends instead)",
                 Category.ERROR, "Interfaces cannot implement another interface (use extends instead)",
                 "/trees/test/hello/HelloWorld.hx", 18, -1);
  }

  @Test
  @DisplayName("rooted warning")
  public void testRootedWarning() {
    doRootedTest("/trees/test",
                 "hello/HelloWorld.hx:18: lines 18-24 : Warning : Danger, Will Robinson!",
                 Category.WARNING, "Danger, Will Robinson!",
                 "/trees/test/hello/HelloWorld.hx", 18, -1);
  }

  @Test
  @DisplayName("absolute path ignores the root")
  public void testAbsolutePathIgnoresTheRoot() {
    String error = "/an/absolute/path/HelloWorld.hx:12: characters 1-16 : Unknown identifier : addEvetListener";
    HaxeCompilerMessage message = HaxeCompilerMessage.create("/trees/test", error, false);

    assertNotNull(message);
    assertEquals(Category.ERROR, message.getCategory());
    // a unix absolute path is only recognizable as absolute on a unix host
    if (Platform.current() == Platform.UNIX) {
      assertEquals("/an/absolute/path/HelloWorld.hx", message.getPath());
    }
    assertEquals("Unknown identifier : addEvetListener", message.getMessage());
    assertEquals(12, message.getLine());
    assertEquals(1, message.getColumn());
  }

  /**
   * Haxe emits global warnings without a file location (e.g. the deprecated
   * flash target notice) in two spellings across versions; both must be
   * reported as warnings, not errors.
   */
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {
    "((unknown)) Warning : (WDeprecatedDefine) The flash target will be removed for Haxe 5",
    "(unknown) : Warning : (WDeprecatedDefine) The flash target will be removed for Haxe 5"})
  @DisplayName("file less warning stays a warning")
  public void testFileLessWarningStaysAWarning(String output) {
    HaxeCompilerMessage message = HaxeCompilerMessage.create("/trees/test", output, false);
    assertNotNull(message);
    assertEquals(Category.WARNING, message.getCategory());
  }
}
