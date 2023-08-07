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

import com.intellij.lang.LanguageAnnotators;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeUnresolvedTypeAnnotator;
import com.intellij.plugins.haxe.ide.inspections.HaxeUnresolvedSymbolInspection;
import com.intellij.util.ArrayUtil;
import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeAnnotationTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/annotation/";
  }

  @Override
  protected void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
  }

  private void doTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    myFixture.configureByFile(getTestName(false) + ".hx");
    myFixture.enableInspections(getAnnotatorBasedInspection());
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }

  private void doUnresolvedSymbolTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }

  @Test
  public void testIDEA_100331() throws Throwable {
    doTest("test/TArray.hx");
  }

  @Test
  public void testIDEA_100331_2() throws Throwable {
    doTest("test/TArray.hx");
  }

  @Test
  public void testIDEA_106515() throws Throwable {
    doTest("test/TArray.hx");
  }

  @Test
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
  public void testIDEA_ResolveImportWithoutType() throws Throwable {
    final String[] paths = {"test/stdTools.hx", getTestName(false) + ".hx"};
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    final String haxe_macro_Tools_contents = "package haxe.macro;\ntypedef TExprTools = ExprTools;\n";
    myFixture.addFileToProject("haxe/macro/Tools.hx", haxe_macro_Tools_contents);
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }


  @Test
  public void testValueTypeUnresolvedOnDynamicMap() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testValueTypeKnownFromTypeTagOnMapAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testValueTypeKnownOnMapAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testValueTypeInferredOnMapAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testInferredTypeOnArrayAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testKnownTypeOnArrayAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testKnownTypeFromTypeTagOnArrayAccess() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testDynamicArrayCantBeAccessed() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testMethodAccessThroughNullable() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testVarAccessThroughNullable() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testMethodAccessThroughAbstract() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testMethodAccessThroughAbstractAndNull() throws Exception {
    doUnresolvedSymbolTest();
  }

  @Test
  public void testForLoopVariableType() throws Exception {
    doUnresolvedSymbolTest();
  }
}
