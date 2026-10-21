/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.actions.OptimizeImportsAction;
import com.intellij.ide.DataManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Created by fedorkorotkov.
 */
@DisplayName("Refactoring: import optimizer")
public class HaxeImportOptimizerTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/imports/optimize/";
  }

  @Test
  @DisplayName("helper 1 - drops imports unused by extends")
  public void testHelper1() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("simple 1 - removes the only unused import")
  public void testSimple1() throws Throwable {
    runOptimizeAction("com/foo/Foo.hx");
  }

  @Test
  @DisplayName("simple 2 - keeps used removes unused")
  public void testSimple2() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("in - keeps used aliased import")
  public void testIn() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("wildcard")
  public void testWildcard() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("duplicate")
  public void testDuplicate() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("keep used imports - import used by field type stays")
  public void testKeepusedimports() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("remove unused in - drops unused aliased import")
  public void testRemoveunusedin() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("remove duplicate in - drops duplicated aliased import")
  public void testRemoveduplicatein() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("keep wildcard imports - wildcard stays when explicit duplicate removed")
  public void testKeepwildcardimports() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  @Test
  @DisplayName("reorder imports")
  public void testReorderImports() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx", "com/foo/IFoo.hx", "com/foo/Unused.hx");
  }

  @Test
  @DisplayName("reorder imports with comments")
  public void testReorderImportsWithComments() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx", "com/foo/IFoo.hx", "com/foo/Unused.hx");
  }

  @Test
  @DisplayName("reorder imports issue 493")
  public void testReorderImportsIssue493() throws Throwable {
    runOptimizeAction("js/Browser.hx", "js/Cookie.hx", "js/Lib.hx");
  }

  private void runOptimizeAction(String... additionalFiles) throws Throwable {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(true) + ".hx"}, additionalFiles));
    OptimizeImportsAction.actionPerformedImpl(DataManager.getInstance().getDataContext(myFixture.getEditor().getContentComponent()));
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(true) + "_expected.hx");
  }
}
