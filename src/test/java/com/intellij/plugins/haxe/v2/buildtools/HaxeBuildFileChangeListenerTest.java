package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Build tools: build file change listener")
public class HaxeBuildFileChangeListenerTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    // every fixture is inline
    return "";
  }

  @Test
  @DisplayName("a saved project build file is announced, IDE settings and outside files are not")
  public void testASavedProjectBuildFileIsAnnouncedIdeSettingsAndOutsideFilesAreNot() {
    HaxeBuildFileChangeListener listener = new HaxeBuildFileChangeListener(getProject());
    VirtualFile build = myFixture.addFileToProject("build.hxml", "-main Main").getVirtualFile();
    VirtualFile include = myFixture.addFileToProject("config/defs.xml", "<project/>").getVirtualFile();
    VirtualFile source = myFixture.addFileToProject("src/Main.hx", "class Main {}").getVirtualFile();
    VirtualFile workspace = myFixture.addFileToProject(".idea/workspace.xml", "<project/>").getVirtualFile();
    VirtualFile outside = new LightVirtualFile("outside.hxml");

    assertTrue(listener.concernsBuildFile(contentChanged(build)));
    assertTrue(listener.concernsBuildFile(contentChanged(include)), "an included xml feeds a build file's defines");
    assertTrue(listener.concernsBuildFile(new VFileDeleteEvent(this, build)));
    assertFalse(listener.concernsBuildFile(contentChanged(source)), "a source file carries no defines");
    assertFalse(listener.concernsBuildFile(contentChanged(workspace)), "the IDE saves its settings on its own schedule");
    assertFalse(listener.concernsBuildFile(contentChanged(outside)), "a file outside the project is not watched");
  }

  private VFileContentChangeEvent contentChanged(VirtualFile file) {
    return new VFileContentChangeEvent(this, file, 0, 1);
  }
}
