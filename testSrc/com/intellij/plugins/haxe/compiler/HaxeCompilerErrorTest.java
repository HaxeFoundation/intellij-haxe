/*
 * Written by Eric Bishton.  No copyright is asserted for this file.
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

import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.plugins.haxe.compilation.HaxeCompilerError;
import com.intellij.testFramework.UsefulTestCase;

/**
 * Created by ebishton on 10/29/16.
 */
public class HaxeCompilerErrorTest extends UsefulTestCase {

  private void doTest(String output, CompilerMessageCategory cat, String msg, String path, int line, int col) throws Throwable {
    HaxeCompilerError e = HaxeCompilerError.create("", output);
    assertEquals(cat, e.getCategory());
    assertEquals(msg, e.getErrorMessage());
    assertEquals(path, e.getPath());
    assertEquals(line, e.getLine());
    assertEquals(col, e.getColumn());
  }

  private void doInfoTest(String compilerOutput) throws Throwable {
    doTest(compilerOutput, CompilerMessageCategory.INFORMATION, compilerOutput.trim(), "", -1, -1);
  }

  private void doWarningTest(String compilerOutput) throws Throwable {
    doTest(compilerOutput, CompilerMessageCategory.WARNING, compilerOutput.trim(), "", -1, -1);
  }

  private void doErrorTest(String compilerOutput, String expected) throws Throwable {
    doTest(compilerOutput, CompilerMessageCategory.ERROR, expected, "", -1, -1);
  }

  // hxcpp 3.3 link message
  public void testLinkMessage() throws Throwable {
    doInfoTest(" -  - Link : ApplicationMain: xcrun");
  }

  // hxcpp 3.3 compile message.
  public void testCompileMessage() throws Throwable {
    doInfoTest(" - Compile : src/ApplicationMain.hx");
  }

  public void testCompilingMessage() throws Throwable {
    doInfoTest(" - Compiling src/ApplicationMain.hx : <some_message>");
  }

  public void testGeneratingMessage() throws Throwable {
    doInfoTest("Generating out/ApplicationMain.cpp : <some_message>");
  }

  public void testLibraryNotInstalled() throws Throwable {
    String compilerOutput = "Error: Library Flixel is not installed. Please run haxelib...";
    doTest(compilerOutput, CompilerMessageCategory.ERROR, "Library Flixel is not installed. Please run haxelib...", "", -1, -1);
  }

  // Hxcpp 3.3
  public void testLibraryNotInstalledHxcpp() throws Throwable {
    String compilerOutput = "Library hxcpp is not installed";
    doErrorTest(compilerOutput, compilerOutput);
  }

  public void testGenericError() throws Throwable {
    String compilerOutput = "Unknown Error : This is an error message.";
    String expected = " (Unknown Error) This is an error message.";
    doErrorTest(compilerOutput, expected);
  }

  public void testLinesError() throws Throwable {
    String compilerOutput = "Test.hx:4: lines 4-10 : Invalid -main : Test does not have static function main";
    doTest(compilerOutput, CompilerMessageCategory.ERROR, "Invalid -main : Test does not have static function main", "Missing file: /Test.hx", 4, -1);
  }

  public void testHxcppBuildFailure() throws Throwable {
    String compilerOutput = "Error: Build failed";
    String expected = "Build failed";
    doErrorTest(compilerOutput, expected);
  }

  public void testUnexpectedCharacter() throws Throwable {
    String compilerOutput = "Test.hx:4: characters 6-7 : Unexpected %";
    String expected = "Unexpected %";
    doTest(compilerOutput, CompilerMessageCategory.ERROR, expected, "Missing file: /Test.hx", 4, 6);
  }

}
