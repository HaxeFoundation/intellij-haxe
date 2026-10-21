package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The one rule behind the tree's (Active) badge, the define context and the resolve contexts. */
@DisplayName("Build tools: known build files")
public class HaxeKnownBuildFilesTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("a stored active file that was hidden yields to the sole remaining build file")
  public void testAStoredActiveFileThatWasHiddenYieldsToTheSoleRemainingBuildFile() {
    VirtualFile main = myFixture.addFileToProject("build.hxml", "-main Main").getVirtualFile();
    VirtualFile tests = myFixture.addFileToProject("tests.hxml", "-main TestMain").getVirtualFile();
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(tests.getPath());
    assertEquals(tests.getPath(), effectiveActivePath());

    HaxeBuildFilesStore.getInstance(getProject()).removeFile(myFixture.getModule().getName(), tests.getPath());

    assertEquals(main.getPath(), effectiveActivePath());
    assertFalse(ReadAction.compute(() -> HaxeKnownBuildFiles.isKnown(getProject(), tests.getPath())));
  }

  private String effectiveActivePath() {
    return ReadAction.compute(() -> HaxeKnownBuildFiles.effectiveActivePath(getProject()));
  }
}
