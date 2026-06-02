/*
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
package com.intellij.plugins.haxe.resolve;

import com.intellij.lang.LanguageAnnotators;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeUnresolvedTypeAnnotator;
import com.intellij.plugins.haxe.ide.inspections.HaxeUnresolvedSymbolInspection;
import com.intellij.util.ArrayUtil;
import org.junit.Test;

public class HaxeImportTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  public void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
    setTestStyleSettings(2);
  }

  @Override
  protected String getBasePath() {
    return "/resolve/import_hx/";
  }

  public void doTest(String... additionalFiles) {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
    LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, new HaxeUnresolvedTypeAnnotator());
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    myFixture.testHighlighting(true, true, true);
  }

  @Test
  public void testImports() {
    doTest("import.hx", "somepkg/import.hx", "somepkg/Helper.hx");
  }

  // Module-level functions imported via import.hx must resolve at their call
  // sites. The `_` function is the interesting case: `_` is neither upper- nor
  // lower-case, so the import classifier used to treat `Helpers._` as a type
  // import and the call `_("...")` ended up "Unresolved symbol". `wrap` is a
  // control (a normal lower-case name) that should resolve regardless.
  @Test
  public void testModuleLevelFunctionImport() {
    myFixture.configureByFiles("moduleFn/Usage.hx", "moduleFn/Helpers.hx", "moduleFn/import.hx");
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class);
    myFixture.testHighlighting(true, false, false);
  }
}
