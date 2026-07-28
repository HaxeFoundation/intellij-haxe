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

import com.intellij.plugins.haxe.compilation.HaxeCompilerMessage.Category;
import com.intellij.plugins.haxe.compilation.HaxeCompilerMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  @DisplayName("link message 2")
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
}
