package com.intellij.plugins.haxe.ide;

import com.intellij.codeInspection.DefaultHighlightVisitorBasedInspection;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.annotator.HaxeTypeAnnotator;
import com.intellij.util.ArrayUtil;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeAnnotationTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/annotation/";
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

  public void testIDEA_106515() throws Throwable {
    doTest("test/TArray.hx");
  }

  public void testIDEA_106515_2() throws Throwable {
    doTest("test/TArray.hx");
  }
}
