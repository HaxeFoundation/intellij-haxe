package com.intellij.plugins.haxe.v2.toolwindow;

import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.toolwindow.tree.HaxeToolWindowNodes.LibraryNode;
import com.intellij.pom.Navigatable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType.LIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tool window: navigation")
public class HaxeToolWindowNavigationTest extends HaxeCodeInsightFixtureTestCase {

  @TempDir
  Path libraryDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The library sync a project open starts would replace the haxelib entries the tests attach. */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    awaitLibrarySync();
  }

  @Test
  @DisplayName("a synced library row leads to the library")
  public void testASyncedLibraryRowLeadsToTheLibrary() throws IOException {
    syncedLibrary("somelib");

    Navigatable target = HaxeToolWindowNavigation.forSelection(getProject(), libraryRow("somelib"));

    assertNotNull(target);
    assertTrue(target.canNavigate());
  }

  @Test
  @DisplayName("a library row that is not synced leads nowhere")
  public void testALibraryRowThatIsNotSyncedLeadsNowhere() {
    Navigatable target = HaxeToolWindowNavigation.forSelection(getProject(), libraryRow("otherlib"));

    assertNotNull(target);
    assertFalse(target.canNavigate());
  }

  private LibraryNode libraryRow(String name) {
    VirtualFile projectXml = myFixture.addFileToProject("project.xml", "<project/>").getVirtualFile();
    return new LibraryNode(new HaxeBuildFile(projectXml, LIME), name, null, "1.0.0", true, null);
  }

  private void syncedLibrary(String name) throws IOException {
    Path root = Files.createDirectories(libraryDirectory.resolve(name));
    VirtualFile rootFile = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root));
    ModuleRootModificationUtil.addModuleLibrary(myFixture.getModule(), "haxelib: " + name + " 1.0.0", List.of(rootFile.getUrl()), List.of());
  }
}
