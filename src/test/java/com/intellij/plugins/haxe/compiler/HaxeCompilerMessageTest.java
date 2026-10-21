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
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Created by ebishton on 10/29/16.
 */
@DisplayName("Compiler: message")
public class HaxeCompilerMessageTest {

  /** hxcpp 3.3 progress lines, reported verbatim (spaces and trailing newline included) as information. */
  static final List<String> INFORMATION_LINES = List.of(
    // link message
    " -  - Link : ApplicationMain: xcrun",
    // link message 2 - with trailing newline
    " - Link : ApplicationMain: xcrun\n",
    // compile message
    " - Compile : src/ApplicationMain.hx",
    // compiling message
    " - Compiling src/ApplicationMain.hx : <some_message>",
    // generating message
    "Generating out/ApplicationMain.cpp : <some_message>");

  @ParameterizedTest(name = "{0}")
  @FieldSource("INFORMATION_LINES")
  @DisplayName("information line is reported verbatim")
  public void testInformationLineIsReportedVerbatim(String output) {
    assertMessage(HaxeCompilerMessage.create("", output), Category.INFORMATION, output, null, -1, -1);
  }

  /** (compiler output, error text) for errors naming no file. */
  static final List<Arguments> UNLOCATED_ERRORS = List.of(
    // library not installed
    arguments("Error: Library Flixel is not installed. Please run haxelib...",
              "Library Flixel is not installed. Please run haxelib..."),
    // library not installed hxcpp (hxcpp 3.3)
    arguments("Library hxcpp is not installed", "Library hxcpp is not installed"),
    // generic error
    arguments("Unknown Error : This is an error message.", " (Unknown Error) This is an error message."),
    // hxcpp build failure
    arguments("Error: Build failed", "Build failed"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("UNLOCATED_ERRORS")
  @DisplayName("unlocated error keeps its text")
  public void testUnlocatedErrorKeepsItsText(String output, String message) {
    assertMessage(HaxeCompilerMessage.create("", output), Category.ERROR, message, null, -1, -1);
  }

  /** (compiler output, error text, path, line, column); an empty root with the existence check on marks the file missing. */
  static final List<Arguments> LOCATED_ERRORS = List.of(
    // lines error
    arguments("Test.hx:4: lines 4-10 : Invalid -main : Test does not have static function main",
              "Invalid -main : Test does not have static function main", "Missing file: /Test.hx", 4, -1),
    // unexpected character
    arguments("Test.hx:4: characters 6-7 : Unexpected %", "Unexpected %", "Missing file: /Test.hx", 4, 6));

  @ParameterizedTest(name = "{0}")
  @FieldSource("LOCATED_ERRORS")
  @DisplayName("located error carries file line and column")
  public void testLocatedErrorCarriesFileLineAndColumn(String output, String message, String path, int line, int column) {
    assertMessage(HaxeCompilerMessage.create("", output), Category.ERROR, message, path, line, column);
  }

  /** (project root, compiler output, category, message, path, line, column): the build window's rooted form, where relative source paths join the root. */
  static final List<Arguments> ROOTED_MESSAGES = List.of(
    // rooted error with windows root
    arguments("C:/Users/username/workspace/project",
              "src/Main.hx:5: characters 0-21 : Class not found : StringTools212",
              Category.ERROR, "Class not found : StringTools212",
              "C:/Users/username/workspace/project/src/Main.hx", 5, 0),
    // rooted error with dot relative path
    arguments("/trees/test",
              "./HelloWorld.hx:12: characters 1-16 : Unknown identifier : addEvetListener",
              Category.ERROR, "Unknown identifier : addEvetListener",
              "/trees/test/./HelloWorld.hx", 12, 1),
    // rooted error without a column
    arguments("/trees/test",
              "hello/HelloWorld.hx:18: lines 18-24 : Interfaces cannot implement another interface (use extends instead)",
              Category.ERROR, "Interfaces cannot implement another interface (use extends instead)",
              "/trees/test/hello/HelloWorld.hx", 18, -1),
    // rooted warning
    arguments("/trees/test",
              "hello/HelloWorld.hx:18: lines 18-24 : Warning : Danger, Will Robinson!",
              Category.WARNING, "Danger, Will Robinson!",
              "/trees/test/hello/HelloWorld.hx", 18, -1));

  @ParameterizedTest(name = "{1}")
  @FieldSource("ROOTED_MESSAGES")
  @DisplayName("rooted message joins the project root")
  public void testRootedMessageJoinsTheProjectRoot(String rootPath, String output, Category category, String message,
                                                   String path, int line, int column) {
    assertMessage(HaxeCompilerMessage.create(rootPath, output, false), category, message, path, line, column);
  }

  @Test
  @DisplayName("absolute path ignores the root")
  public void testAbsolutePathIgnoresTheRoot(@TempDir Path hostDirectory) {
    // only the host's own absolute form is recognizable as absolute
    String file = hostDirectory.resolve("HelloWorld.hx").toString().replace('\\', '/');
    String error = file + ":12: characters 1-16 : Unknown identifier : addEvetListener";

    HaxeCompilerMessage message = HaxeCompilerMessage.create("/trees/test", error, false);

    assertMessage(message, Category.ERROR, "Unknown identifier : addEvetListener", file, 12, 1);
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

  private static void assertMessage(HaxeCompilerMessage message, Category category, String text, String path, int line, int column) {
    assertNotNull(message);
    assertEquals(category, message.getCategory());
    assertEquals(text, message.getMessage());
    assertEquals(path, message.getPath());
    assertEquals(line, message.getLine());
    assertEquals(column, message.getColumn());
  }
}
