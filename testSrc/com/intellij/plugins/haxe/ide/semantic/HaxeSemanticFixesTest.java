/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2018 AS3Boyan
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
package com.intellij.plugins.haxe.ide.semantic;

public class HaxeSemanticFixesTest extends HaxeBaseSemanticAnnotatorTestCase {
  public void testFixPackage() throws Exception {
    doTest("Set package name to ''");
  }

  public void testRemoveOverride() throws Exception {
    doTest("Drop modifier 'override'");
  }

  public void testRemoveFinal() throws Exception {
    doTest("Drop modifier '@:final' from 'Base.test'");
  }

  public void testChangeArgumentType() throws Exception {
    doTest("Change parameter 'a' type from 'Int' to 'Bool'");
  }

  public void testRemoveArgumentInit() throws Exception {
    doTest("Drop initializer");
  }

  public void testOverrideSignature() throws Exception {
    doTest("Remove parameter");
  }

  public void testOverrideSignature3() throws Exception {
    doTest("Remove parameter");
  }

  public void testOverrideSignature4() throws Exception {
    doTest("Remove parameter");
  }

  public void testUcFirstClassName() throws Exception {
    doTest("Rename to 'Test'");
  }
}
