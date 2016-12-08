/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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
package com.intellij.plugins.haxe.editor;

import com.intellij.codeInsight.AbstractEnterActionTestCase;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author winmain
 */
public class HaxeEnterActionTest extends AbstractEnterActionTestCase {
  private HaxeDebugLogger.HierarchyManipulator oldLogSettings;

  public void setUp() throws Exception {
    oldLogSettings = HaxeDebugLogger.mutePrimaryConfiguration();
    super.setUp();
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    oldLogSettings.restore();
    oldLogSettings = null;
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  @Override
  protected void doTest() throws Exception {
    doTest("hx");
  }

  public void testEnterInAbstract() throws Throwable {
    doTextTest("hx",
               "abstract Test {\n" +
               "    var a;<caret>\n" +
               "}",
               "abstract Test {\n" +
               "    var a;\n" +
               "    \n" +
               "}");
  }

  public void testEnterInClass() throws Throwable {
    doTextTest("hx",
               "class Test {\n" +
               "    var a;<caret>\n" +
               "}",
               "class Test {\n" +
               "    var a;\n" +
               "    \n" +
               "}");
  }

  public void testEnterInEnum() throws Throwable {
    doTextTest("hx",
               "enum Test {\n" +
               "    FOO;<caret>\n" +
               "}",
               "enum Test {\n" +
               "    FOO;\n" +
               "    \n" +
               "}");
  }

  public void testEnterInExternClass() throws Throwable {
    doTextTest("hx",
               "extern class Test {\n" +
               "    var a;<caret>\n" +
               "}",
               "extern class Test {\n" +
               "    var a;\n" +
               "    \n" +
               "}");
  }

  public void testEnterInInterface() throws Throwable {
    doTextTest("hx",
               "interface Test {\n" +
               "    function qwe():Void;<caret>\n" +
               "}",
               "interface Test {\n" +
               "    function qwe():Void;\n" +
               "    \n" +
               "}");
  }
}
