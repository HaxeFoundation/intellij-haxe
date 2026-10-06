package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The build-file watcher's settings files are the tool window's build files
 * plus the configuration files they import, and its subscribers learn that
 * the set may have changed whenever a sync lands or a build file is
 * registered.
 */
@DisplayName("Build tools: build files project aware")
public class HaxeBuildFilesProjectAwareTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("settings files include the imported configuration files")
  public void testSettingsFilesIncludeTheImportedConfigurationFiles() {
    VirtualFile build = addFile("build.hxml", """
      config/common.hxml
      -main Main
      --interp
      """);
    VirtualFile common = addFile("config/common.hxml", "-cp src");
    VirtualFile project = addFile("project.xml", """
      <project>
        <haxelib name="lime"/>
        <include path="config/defs.xml"/>
      </project>
      """);
    VirtualFile defs = addFile("config/defs.xml", "<project><haxedef name=\"flag\"/></project>");

    Set<String> settingsFiles = new HaxeBuildFilesProjectAware(getProject()).getSettingsFiles();

    Set<String> expected = Set.of(build.getPath(), common.getPath(), project.getPath(), defs.getPath());
    assertTrue(settingsFiles.containsAll(expected), "watched: " + settingsFiles);
  }

  @Test
  @DisplayName("subscribers hear a settings files list change on sync and on build file registration")
  public void testSubscribersHearASettingsFilesListChangeOnSyncAndOnBuildFileRegistration() {
    HaxeBuildFilesProjectAware projectAware = new HaxeBuildFilesProjectAware(getProject());
    CountingListener listener = new CountingListener();
    projectAware.subscribe(listener, getTestRootDisposable());

    getProject().getMessageBus().syncPublisher(HaxeBuildConfigListener.TOPIC).buildConfigurationChanged();
    assertEquals(1, listener.settingsFilesListChanges, "a finished sync may have changed what the build files import");

    HaxeBuildFilesStore.getInstance(getProject()).addFile("module", "/elsewhere/build.hxml");
    assertEquals(2, listener.settingsFilesListChanges, "a registered build file brings its own imports");
  }

  @Test
  @DisplayName("events of a file the build files no longer import are ignored")
  public void testEventsOfAFileTheBuildFilesNoLongerImportAreIgnored() {
    // a <haxelib> makes the xml a build file; the scanner ignores a bare <project> root
    VirtualFile project = addFile("project.xml", "<project><haxelib name=\"lime\"/><include path=\"config/defs.xml\"/></project>");
    VirtualFile defs = addFile("config/defs.xml", "<project/>");
    HaxeBuildFilesProjectAware projectAware = new HaxeBuildFilesProjectAware(getProject());
    assertTrue(projectAware.isWatched(defs.getPath()), "before the first collection every event counts");

    projectAware.getSettingsFiles();
    assertTrue(projectAware.isWatched(defs.getPath()), "an imported file is watched");

    myFixture.saveText(project, "<project><haxelib name=\"lime\"/></project>");
    projectAware.getSettingsFiles();
    assertFalse(projectAware.isWatched(defs.getPath()), "the tracker keeps once-watched paths; their events are ignored here");
    assertTrue(projectAware.isWatched(project.getPath()), "the build file itself stays watched");
  }

  private VirtualFile addFile(String relativePath, String text) {
    return myFixture.addFileToProject(relativePath, text).getVirtualFile();
  }

  private static final class CountingListener implements ExternalSystemProjectListener {
    int settingsFilesListChanges;

    @Override
    public void onSettingsFilesListChange() {
      settingsFilesListChanges++;
    }
  }
}
