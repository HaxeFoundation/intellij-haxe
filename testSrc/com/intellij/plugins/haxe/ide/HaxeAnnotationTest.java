package com.intellij.plugins.haxe.ide;

import com.intellij.codeInspection.DefaultHighlightVisitorBasedInspection;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeTypeAnnotator;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;

import java.util.Arrays;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeAnnotationTest extends LightCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/annotation/");
  }

  private void doTest(String... additionalPaths) throws Exception {
    final String[] paths = ArrayUtil.append(additionalPaths, getTestName(false) + ".hx");
    myFixture.configureByFiles(ArrayUtil.reverseArray(paths));
    final HaxeTypeAnnotator annotator = new HaxeTypeAnnotator();
    LanguageAnnotators.INSTANCE.addExplicitExtension(HaxeLanguage.INSTANCE, annotator);
    myFixture.enableInspections(new DefaultHighlightVisitorBasedInspection.AnnotatorBasedInspection());
    try {
      myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
    }
    finally {
      LanguageAnnotators.INSTANCE.removeExplicitExtension(HaxeLanguage.INSTANCE, annotator);
    }
  }

  public void testIDEA_100331() throws Throwable {
    doTest("test/TArray.hx");
  }

  public void testIDEA_100331_2() throws Throwable {
    doTest("test/TArray.hx");
  }
}
