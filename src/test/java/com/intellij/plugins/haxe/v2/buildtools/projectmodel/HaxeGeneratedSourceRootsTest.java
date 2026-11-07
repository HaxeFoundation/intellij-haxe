package com.intellij.plugins.haxe.v2.buildtools.projectmodel;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.workspace.WorkspaceModel;
import com.intellij.platform.workspace.jps.entities.SourceRootEntity;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.workspaceModel.ide.NonPersistentEntitySource;
import kotlin.sequences.SequencesKt;
import kotlinx.coroutines.Job;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The selected target's generated folders of an openfl build are generated source roots of its module, for as long as they exist. */
@DisplayName("Build tools: generated source roots")
public class HaxeGeneratedSourceRootsTest extends HaxeCodeInsightFixtureTestCase {
  private static final String PROJECT_XML_SOURCE = """
    <?xml version="1.0" encoding="utf-8"?>
    <project>
      <app main="Main" path="Export" file="Game"/>
      <source path="Source"/>
      <haxelib name="openfl"/>
    </project>
    """;
  private static final String HL_GENERATED_DIR = "Export/hl/haxe";
  private static final String HL_GENERATED_CLASSES_DIR = "Export/hl/haxe/_generated";
  private static final String HTML5_GENERATED_DIR = "Export/html5/haxe";

  private VirtualFile projectXml;

  @Override
  protected String getBasePath() {
    // every fixture is created in-project by the test itself
    return "";
  }

  @BeforeEach
  public void setUpBuild() {
    projectXml = myFixture.addFileToProject("project.xml", PROJECT_XML_SOURCE).getVirtualFile();
    HaxeFrameworkTargetSettings.getInstance().setMarksGeneratedSources(Framework.OPENFL, true);
  }

  /** Back to the default, on, for the tests that run after one turned it off. */
  @AfterEach
  public void resetSetting() {
    HaxeFrameworkTargetSettings.getInstance().setMarksGeneratedSources(Framework.OPENFL, true);
  }

  @Test
  @DisplayName("the selected targets generated folders become generated source roots")
  public void testTheSelectedTargetsGeneratedFoldersBecomeGeneratedSourceRoots() throws IOException {
    createDirectory(HL_GENERATED_CLASSES_DIR);
    createDirectory(HTML5_GENERATED_DIR);
    selectTarget("HashLink");

    reconcile();

    assertEquals(List.of(path(HL_GENERATED_DIR), path(HL_GENERATED_CLASSES_DIR)), generatedRootPaths());
  }

  @Test
  @DisplayName("switching the target moves the roots")
  public void testSwitchingTheTargetMovesTheRoots() throws IOException {
    createDirectory(HL_GENERATED_CLASSES_DIR);
    createDirectory(HTML5_GENERATED_DIR);
    selectTarget("HashLink");
    reconcile();

    selectTarget("HTML5");
    reconcile();

    assertEquals(List.of(path(HTML5_GENERATED_DIR)), generatedRootPaths(), "the html5 build has no _generated folder");
  }

  @Test
  @DisplayName("a clean removes the roots and a rebuild restores them")
  public void testACleanRemovesTheRootsAndARebuildRestoresThem() throws IOException {
    createDirectory(HL_GENERATED_CLASSES_DIR);
    selectTarget("HashLink");
    reconcile();
    assertTrue(roots().affectsGeneratedFolders(path("Export/hl")), "a clean deletes the whole export target directory");
    assertFalse(roots().affectsGeneratedFolders(path("Source")), "source edits are not watched");

    deleteDirectory("Export/hl");
    reconcile();
    assertEquals(List.of(), generatedRootPaths());

    createDirectory(HL_GENERATED_CLASSES_DIR);
    reconcile();
    assertEquals(List.of(path(HL_GENERATED_DIR), path(HL_GENERATED_CLASSES_DIR)), generatedRootPaths());
  }

  @Test
  @DisplayName("turning the setting off removes the roots")
  public void testTurningTheSettingOffRemovesTheRoots() throws IOException {
    createDirectory(HL_GENERATED_CLASSES_DIR);
    selectTarget("HashLink");
    reconcile();

    HaxeFrameworkTargetSettings.getInstance().setMarksGeneratedSources(Framework.OPENFL, false);
    reconcile();

    assertEquals(List.of(), generatedRootPaths());
  }

  @Test
  @DisplayName("the roots are unsaved nested and reach into the excluded export folder")
  public void testTheRootsAreUnsavedNestedAndReachIntoTheExcludedExportFolder() throws IOException {
    VirtualFile exportDir = excludeFolder("Export");
    VirtualFile generatedClassesDir = createDirectory(HL_GENERATED_CLASSES_DIR);
    VirtualFile generatedClass = createFile(HL_GENERATED_CLASSES_DIR + "/Animation.hx", "class Animation {}\n");
    selectTarget("HashLink");

    reconcile();

    ProjectFileIndex index = ProjectFileIndex.getInstance(getProject());
    assertTrue(index.isInSourceContent(generatedClass), "a root inside the excluded export folder is indexed");
    assertTrue(index.isExcluded(exportDir), "the rest of the export folder stays excluded");
    assertEquals(generatedClassesDir, index.getSourceRootForFile(generatedClass), "the nested root wins, so the class keeps its empty package");
    assertTrue(generatedRootsAreUnsaved(), "the roots follow a personal choice and must not reach the .iml");
  }

  private HaxeGeneratedSourceRoots roots() {
    return HaxeGeneratedSourceRoots.getInstance(getProject());
  }

  private void reconcile() {
    Job job = roots().requestReconcile();
    PlatformTestUtil.waitWithEventsDispatching("generated source roots reconciled", job::isCompleted, 10);
  }

  private void selectTarget(String targetId) {
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(projectXml, targetId);
  }

  private VirtualFile createDirectory(String relative) throws IOException {
    return myFixture.getTempDirFixture().findOrCreateDir(relative);
  }

  private VirtualFile createFile(String relative, String text) {
    return myFixture.addFileToProject(relative, text).getVirtualFile();
  }

  private void deleteDirectory(String relative) throws IOException {
    VirtualFile directory = contentRoot().findFileByRelativePath(relative);
    assertNotNull(directory, relative);
    WriteAction.runAndWait(() -> directory.delete(this));
  }

  private VirtualFile excludeFolder(String relative) throws IOException {
    VirtualFile folder = createDirectory(relative);
    ModuleRootModificationUtil.updateModel(getModule(), model -> model.getContentEntries()[0].addExcludeFolder(folder));
    return folder;
  }

  private VirtualFile contentRoot() {
    return ModuleRootManager.getInstance(getModule()).getContentRoots()[0];
  }

  private String path(String relative) {
    return contentRoot().getPath() + "/" + relative;
  }

  /** The module's generated source folders, in declaration order. */
  private List<String> generatedRootPaths() {
    List<String> paths = new ArrayList<>();
    for (ContentEntry entry : ModuleRootManager.getInstance(getModule()).getContentEntries()) {
      for (SourceFolder folder : entry.getSourceFolders()) {
        if (folder.getJpsElement().getProperties() instanceof JavaSourceRootProperties properties && properties.isForGeneratedSources()) {
          paths.add(VfsUtilCore.urlToPath(folder.getUrl()));
        }
      }
    }
    return paths;
  }

  /** Whether every source root under the export folder carries the non-persistent entity source. */
  private boolean generatedRootsAreUnsaved() {
    List<SourceRootEntity> roots = SequencesKt.toList(
      WorkspaceModel.Companion.getInstance(getProject()).getCurrentSnapshot().entities(SourceRootEntity.class));
    return roots.stream()
      .filter(root -> root.getUrl().getUrl().contains("/Export/"))
      .allMatch(root -> root.getEntitySource() == NonPersistentEntitySource.INSTANCE);
  }
}
