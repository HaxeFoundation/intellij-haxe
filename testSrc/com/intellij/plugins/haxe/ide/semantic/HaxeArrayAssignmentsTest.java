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

public class HaxeArrayAssignmentsTest extends HaxeBaseSemanticAnnotatorTestCase {

  public void testArrayAssignmentFromEmpty() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentBadFunctionType() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentWrongType() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentBadArrowFunction() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentWithAbstract() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testArrayAssignmentWithArrowFunctions() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }
}
