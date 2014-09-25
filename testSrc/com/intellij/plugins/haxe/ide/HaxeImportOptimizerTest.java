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
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;

/**
 * Created by fedorkorotkov.
 */
public class HaxeImportOptimizerTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/imports/optimize/";
  }

  public void testHelper1() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  public void testSimple1() throws Throwable {
    runOptimizeAction("com/foo/Foo.hx");
  }

  public void testSimple2() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  public void testIn() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  public void testWildcard() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  public void testDuplicate() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  public void testKeepusedimports() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  public void testRemoveunusedin() throws Throwable {
    runOptimizeAction("com/foo/Bar.hx", "com/foo/Foo.hx");
  }

  private void runOptimizeAction(String... additionalFiles) throws Throwable {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(true) + ".hx"}, additionalFiles));
    OptimizeImportsAction.actionPerformedImpl(DataManager.getInstance().getDataContext(myFixture.getEditor().getContentComponent()));
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(true) + "_expected.hx");
  }
}
