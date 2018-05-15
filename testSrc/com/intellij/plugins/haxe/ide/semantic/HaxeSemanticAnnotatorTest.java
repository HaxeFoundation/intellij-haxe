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

public class HaxeSemanticAnnotatorTest extends HaxeBaseSemanticAnnotatorTestCase {

  public void testInterfaceMethodsShouldHaveTypeTags() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOptionalWithInitWarning() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonConstantArgument() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNonConstantArgumentAbstractEnum() throws Exception {
    doTestNoFixWithWarnings("test/SampleAbstractEnum.hx", "std/StdTypes.hx");
  }

  public void testConstructorMustNotBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testInitMagicMethodShouldBeStatic() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testRepeatedArgumentName() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testAbstractFromTo() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNullFunction() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideVisibility() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testUcFirstClassName2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testRepeatedFields() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testPropertiesSimpleCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testPropertyAllowNonConstantInitialization() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testOverrideSignature2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testImplementSignature() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testImplementExternInterface() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testSimpleAssignUnknownGeneric() throws Exception {
    doTestNoFixWithWarnings("std/StdTypes.hx", "std/Array.hx");
  }

  public void testExtendsAnonymousType() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testExtendsSelf() throws Exception {
    doTestNoFixWithWarnings("test/Bar.hx", "test/IBar.hx", "test/TBar.hx");
  }

  public void testFieldInitializerCheck() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testVariableRedefinition() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testFinalKeyword() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNullTAssignment1() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testNullTAssignment2() throws Exception {
    doTestNoFixWithWarnings();
  }

  public void testStringLiteral() throws Exception {
    doTestNoFixWithWarnings();
  }
}
