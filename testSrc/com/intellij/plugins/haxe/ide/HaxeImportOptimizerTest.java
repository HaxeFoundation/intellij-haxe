package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.actions.OptimizeImportsAction;
import com.intellij.ide.DataManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;

/**
 * Created by fedorkorotkov.
 */
public class HaxeImportOptimizerTest extends LightCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return FileUtil.toSystemDependentName("/plugins/haxe/testData/imports/optimize/");
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

  private void runOptimizeAction(String... additionalFiles) throws Throwable {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(true) + ".hx"}, additionalFiles));
    OptimizeImportsAction.actionPerformedImpl(DataManager.getInstance().getDataContext(myFixture.getEditor().getContentComponent()));
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(true) + "_expected.hx");
  }
}
