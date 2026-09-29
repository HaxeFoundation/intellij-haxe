/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2020 Eric Bishton
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

import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedSymbolInspection;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedTypeInspection;
import com.intellij.util.ArrayUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Annotation: haxe annotation")
public class HaxeAnnotationTest extends HaxeToolkitLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/annotation/";
  }

  private void doTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    myFixture.configureByFile(getTestName(false) + ".hx");
    myFixture.enableInspections(getAnnotatorBasedInspection());
    myFixture.enableInspections(HaxeUnresolvedTypeInspection.class);
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }

  private void doUnresolvedSymbolTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }

  private void doUnresolvedSymbolWarningsOnlyTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    myFixture.testHighlighting(true, false, false, myFixture.getFile().getVirtualFile());
  }

  @Test
  @DisplayName("inherited field in object literal")
  public void testInheritedFieldInObjectLiteral() throws Exception {
    doUnresolvedSymbolWarningsOnlyTest();
  }

  /*
   * When an object-literal member has no value expression HaxeObjectLiteralElement.getExpression() should not return null.
   */
  @Test
  @DisplayName("incomplete object literal member")
  public void testIncompleteObjectLiteralMember() throws Exception {
    myFixture.configureByFile(getTestName(false) + ".hx");
    myFixture.doHighlighting();
  }

  @Test
  @DisplayName("IDEA 100331 - declared type parameter resolves")
  public void testIDEA_100331() throws Throwable {
    doTest("test/TArray.hx");
  }

  @Test
  @DisplayName("IDEA 100331 2 - undeclared type parameter is unresolved")
  public void testIDEA_100331_2() throws Throwable {
    doTest("test/TArray.hx");
  }

  @Test
  @DisplayName("IDEA 106515 - unresolved type in static call")
  public void testIDEA_106515() throws Throwable {
    doTest("test/TArray.hx");
  }

  @Test
  @DisplayName("IDEA 106515 2 - unresolved type as bare statement")
  public void testIDEA_106515_2() throws Throwable {
    doTest("test/TArray.hx");
  }

  /* Test that an import file containing no classes of the same name is resolved.
   * The standard haxe library file haxe/macro/Tools.hx was the definitive error
   * case.
   *
   * @throws Throwable
   */
  @Test
  @DisplayName("IDEA resolve import without type")
  public void testIDEA_ResolveImportWithoutType() throws Throwable {
    final String[] paths = {"test/stdTools.hx", getTestName(false) + ".hx"};
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    final String haxe_macro_Tools_contents = "package haxe.macro;\ntypedef TExprTools = ExprTools;\n";
    myFixture.addFileToProject("haxe/macro/Tools.hx", haxe_macro_Tools_contents);
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }

  @Test
  @DisplayName("value type unresolved on dynamic map")
  public void testValueTypeUnresolvedOnDynamicMap() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("value type known from type tag on map access")
  public void testValueTypeKnownFromTypeTagOnMapAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("value type known on map access")
  public void testValueTypeKnownOnMapAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("value type inferred on map access")
  public void testValueTypeInferredOnMapAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("inferred type on array access")
  public void testInferredTypeOnArrayAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("known type on array access")
  public void testKnownTypeOnArrayAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("known type from type tag on array access")
  public void testKnownTypeFromTypeTagOnArrayAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("dynamic array cant be accessed")
  public void testDynamicArrayCantBeAccessed() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("method access through nullable")
  public void testMethodAccessThroughNullable() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("var access through nullable")
  public void testVarAccessThroughNullable() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("method access through abstract")
  public void testMethodAccessThroughAbstract() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("method access through abstract and null")
  public void testMethodAccessThroughAbstractAndNull() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  @DisplayName("for loop variable type")
  public void testForLoopVariableType() throws Exception {
    usingHaxeToolkit(); // need ArrayAccess & ArrayIterator from std
    doUnresolvedSymbolTest();
  }
}
