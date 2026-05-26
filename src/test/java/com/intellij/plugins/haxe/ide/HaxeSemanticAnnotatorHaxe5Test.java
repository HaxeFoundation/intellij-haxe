package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.util.HaxeTestUtils;
import org.junit.Test;


/**
 * Runs the same semantic-annotator regression tests against the Haxe 5 preview
 * std-lib fixture (instead of the default 4.3.6). Use this to pin down bugs
 * that only manifest when the project SDK is Haxe 5.
 */
public class HaxeSemanticAnnotatorHaxe5Test extends HaxeSemanticAnnotatorTestBase {
  @Override
  public void setUp() throws Exception {
    useHaxeToolkit(HaxeTestUtils.VERSION_5_0_0_PREVIEW);
    super.setUp();
    setTestStyleSettings(2);
  }

  @Override
  protected String getBasePath() {
    return "/annotation.semantic/";
  }

  @Test
  public void testStaticToStringReturnsString() throws Exception {
    doTestNoFixWithWeakWarnings();
  }
}
