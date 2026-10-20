package com.intellij.plugins.haxe.resolve;

import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedSymbolInspection;
import com.intellij.plugins.haxe.ide.inspections.resolve.HaxeUnresolvedTypeInspection;
import com.intellij.util.ArrayUtil;

/**
 * Base for the resolve-highlighting tests: the unresolved-type plus
 * unresolved-symbol inspections over a test-name-derived fixture.
 * Subclasses supply only their fixture directory via {@code getBasePath()}.
 */
public abstract class HaxeResolveHighlightingTestBase extends HaxeToolkitLightFixtureTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    setTestStyleSettings(2);
  }

  protected void doTest(String... additionalFiles) {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
    myFixture.enableInspections(HaxeUnresolvedSymbolInspection.class, HaxeUnresolvedTypeInspection.class);
    myFixture.testHighlighting(true, true, true);
  }
}
