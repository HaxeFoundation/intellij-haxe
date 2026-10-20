package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.ActionReference;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeCustomActionsStore.CustomAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Build tools: build file move tracker")
public class HaxeBuildFileMoveTrackerTest extends HaxeLightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // the stores are services of the shared light project
      HaxeCustomActionsStore.getInstance(getProject()).loadState(new HaxeCustomActionsStore.State());
      HaxeTargetSelectionStore.getInstance(getProject()).loadState(new HaxeTargetSelectionStore.State());
      HaxeWorkDirectoryStore.getInstance(getProject()).loadState(new HaxeWorkDirectoryStore.State());
      HaxeActiveBuildFileStore.getInstance(getProject()).loadState(new HaxeActiveBuildFileStore.State());
      HaxeEnvironmentStore.getInstance(getProject()).loadState(new HaxeEnvironmentStore.State());
      HaxeTestsBuildFileStore.getInstance(getProject()).loadState(new HaxeTestsBuildFileStore.State());
      HaxeBuildFilesStore.getInstance(getProject()).loadState(new HaxeBuildFilesStore.State());
      HaxeResolveContextStore.getInstance(getProject()).loadState(new HaxeResolveContextStore.State());
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("renaming and moving a build file carries its actions and the references to them")
  public void testRenamingAndMovingABuildFileCarriesItsActionsAndTheReferencesToThem() throws IOException {
    VirtualFile library = myFixture.addFileToProject("lib/build.hxml", "-main Lib").getVirtualFile();
    VirtualFile root = myFixture.addFileToProject("build.hxml", "-main Main").getVirtualFile();
    VirtualFile target = myFixture.getTempDirFixture().findOrCreateDir("libs");
    HaxeCustomActionsStore store = HaxeCustomActionsStore.getInstance(getProject());
    store.addAction(library.getPath(), new CustomAction("prepare", "cmd-prepare", ""));
    ActionReference prepare = new ActionReference(library.getPath(), "prepare");
    store.addAction(root.getPath(), new CustomAction("release", "cmd-release", "", null, List.of(prepare)));

    WriteAction.runAndWait(() -> library.rename(this, "main.hxml"));
    assertEquals(List.of("prepare"), actionNames(store, library.getPath()));
    assertEquals(List.of(new ActionReference(library.getPath(), "prepare")), store.runBeforeChain(root.getPath(), "release"));

    WriteAction.runAndWait(() -> library.getParent().move(this, target));
    assertTrue(library.getPath().contains("/libs/lib/"), library.getPath());
    assertEquals(List.of("prepare"), actionNames(store, library.getPath()));
    assertEquals(List.of(new ActionReference(library.getPath(), "prepare")), store.runBeforeChain(root.getPath(), "release"));
  }

  @Test
  @DisplayName("renaming a build file carries every store entry keyed by its path")
  public void testRenamingABuildFileCarriesEveryStoreEntryKeyedByItsPath() throws IOException {
    VirtualFile library = myFixture.addFileToProject("lib/build.hxml", "-main Lib").getVirtualFile();
    VirtualFile source = myFixture.addFileToProject("lib/src/Lib.hx", "class Lib {}").getVirtualFile();
    VirtualFile opened = myFixture.addFileToProject("lib/src/Opened.hx", "class Opened {}").getVirtualFile();
    String containerId = HaxeContainers.containerIdFor(getProject(), library);
    String oldPath = library.getPath();
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(library, "html5");
    HaxeWorkDirectoryStore.getInstance(getProject()).setWorkDirectory(oldPath, library.getParent().getPath());
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(oldPath);
    HaxeEnvironmentStore.getInstance(getProject()).setCompileCommand(containerId, new HaxeEnvironmentStore.CompileCommand(oldPath, null, ""));
    HaxeTestsBuildFileStore.getInstance(getProject()).markTestsFile(containerId, oldPath);
    HaxeBuildFilesStore.getInstance(getProject()).addFile(containerId, oldPath);
    HaxeResolveContextStore.getInstance(getProject()).pin(source, oldPath);
    HaxeResolveContextStore.getInstance(getProject()).follow(opened, oldPath);

    WriteAction.runAndWait(() -> library.rename(this, "main.hxml"));

    String newPath = library.getPath();
    assertEquals("html5", HaxeTargetSelectionStore.getInstance(getProject()).getSelectedTargetId(library));
    assertEquals(library.getParent().getPath(), HaxeWorkDirectoryStore.getInstance(getProject()).getWorkDirectory(newPath));
    assertEquals(newPath, HaxeActiveBuildFileStore.getInstance(getProject()).getActiveFilePath());
    assertEquals(newPath, HaxeEnvironmentStore.getInstance(getProject()).getCompileCommand(containerId).buildFilePath());
    assertEquals(List.of(newPath), HaxeTestsBuildFileStore.getInstance(getProject()).getTestsFilePaths(containerId));
    assertEquals(List.of(newPath), HaxeBuildFilesStore.getInstance(getProject()).getAddedPaths(containerId));
    assertEquals(newPath, HaxeResolveContextStore.getInstance(getProject()).getPinnedBuildFile(source));
    assertEquals(newPath, HaxeResolveContextStore.getInstance(getProject()).getFollowedBuildFile(opened));
  }

  private static List<String> actionNames(HaxeCustomActionsStore store, String buildFile) {
    return store.getActions(buildFile).stream()
      .map(CustomAction::name)
      .toList();
  }
}
