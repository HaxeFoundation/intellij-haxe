package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.psi.PsiFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Build tools: tool config detector")
public class HaxeToolConfigDetectorTest extends HaxeLightFixtureTestCase {
  @Override
  protected String getBasePath() {
    // fixtures are built from text; no test-data directory
    return "";
  }

  @Test
  @DisplayName("answers unknown until the background search lands")
  public void testAnswersUnknownUntilTheBackgroundSearchLands() throws Exception {
    myFixture.addFileToProject("hxformat.json", "{}");
    VirtualFile deep = myFixture.addFileToProject("sub/pack/Deep.hx", "class Deep {}").getVirtualFile();
    HaxeToolConfigDetector detector = HaxeToolConfigDetector.getInstance(getProject());

    VirtualFile firstAsk = detector.knownConfigDirectory(deep, HaxeToolConfigs.FORMATTER_CONFIG_NAME);
    assertNull(firstAsk, "the first ask answers unknown and only starts the search");

    detector.awaitSearchesForTests();
    VirtualFile known = detector.knownConfigDirectory(deep, HaxeToolConfigs.FORMATTER_CONFIG_NAME);
    assertEquals(deep.getParent().getParent().getParent(), known);
  }

  @Test
  @DisplayName("a config appearing drops the memo")
  public void testAConfigAppearingDropsTheMemo() throws Exception {
    PsiFile lone = myFixture.addFileToProject("pack/Lone.hx", "class Lone {}");
    VirtualFile file = lone.getVirtualFile();
    HaxeToolConfigDetector detector = HaxeToolConfigDetector.getInstance(getProject());
    detector.knownConfigDirectory(file, HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME);
    detector.awaitSearchesForTests();
    assertNull(detector.knownConfigDirectory(file, HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME), "no config anywhere yet");

    myFixture.addFileToProject("pack/checkstyle.json", "{}");
    VirtualFile afterChange = detector.knownConfigDirectory(file, HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME);
    assertNull(afterChange, "the memo is dropped, so the ask is unknown again");

    detector.awaitSearchesForTests();
    VirtualFile known = detector.knownConfigDirectory(file, HaxeToolConfigs.CHECKSTYLE_CONFIG_NAME);
    assertEquals(file.getParent(), known);
  }
}
