package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeWorkDirectoryStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An hxml's work directory is sniffed from where its classpaths resolve: the
 * file's own folder when they resolve there, else the content root (the vshaxe
 * convention, e.g. OpenFL's scripts/completion.hxml). The per-file override
 * wins over the sniff, and non-hxml files keep their own folder.
 */
@DisplayName("Build tools: build file work directories")
public class HaxeBuildWorkDirectoriesTest extends HaxeCodeInsightFixtureTestCase {

  private static final String SRC_CLASSPATH_HXML_SOURCE = "-cp src\n-main Main";

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("root relative classpaths anchor the subfolder hxml at the content root")
  public void testRootRelativeClasspathsAnchorTheSubfolderHxmlAtTheContentRoot() {
    myFixture.addFileToProject("src/Main.hx", "class Main {}");
    VirtualFile hxml = myFixture.addFileToProject("scripts/completion.hxml", SRC_CLASSPATH_HXML_SOURCE).getVirtualFile();

    VirtualFile anchor = HaxeBuildWorkDirectories.anchor(getProject(), hxml);
    assertNotNull(anchor);
    assertEquals(hxml.getParent().getParent(), anchor, "src/ exists only at the root, so the root must anchor");
    assertEquals("scripts/completion.hxml", HaxeBuildWorkDirectories.fileArgument(getProject(), hxml));
  }

  @Test
  @DisplayName("folder relative classpaths keep the hxml in its own folder")
  public void testFolderRelativeClasspathsKeepTheHxmlInItsOwnFolder() {
    myFixture.addFileToProject("munit/src/Main.hx", "class Main {}");
    VirtualFile hxml = myFixture.addFileToProject("munit/test.hxml", SRC_CLASSPATH_HXML_SOURCE).getVirtualFile();

    assertEquals(hxml.getParent(), HaxeBuildWorkDirectories.anchor(getProject(), hxml),
                 "src/ resolves beside the file, so its folder stays authoritative");
    assertEquals("test.hxml", HaxeBuildWorkDirectories.fileArgument(getProject(), hxml));
  }

  @Test
  @DisplayName("unresolvable classpaths keep the hxml in its own folder")
  public void testUnresolvableClasspathsKeepTheHxmlInItsOwnFolder() {
    VirtualFile hxml = myFixture.addFileToProject("scripts/completion.hxml", SRC_CLASSPATH_HXML_SOURCE).getVirtualFile();

    assertEquals(hxml.getParent(), HaxeBuildWorkDirectories.anchor(getProject(), hxml));
    assertEquals("completion.hxml", HaxeBuildWorkDirectories.fileArgument(getProject(), hxml));
  }

  @Test
  @DisplayName("root hxml keeps the bare file name argument")
  public void testRootHxmlKeepsTheBareFileNameArgument() {
    VirtualFile hxml = myFixture.addFileToProject("build.hxml", SRC_CLASSPATH_HXML_SOURCE).getVirtualFile();

    assertEquals(hxml.getParent(), HaxeBuildWorkDirectories.anchor(getProject(), hxml));
    assertEquals("build.hxml", HaxeBuildWorkDirectories.fileArgument(getProject(), hxml));
  }

  @Test
  @DisplayName("stored override wins over the sniffed default")
  public void testStoredOverrideWinsOverTheSniffedDefault() {
    myFixture.addFileToProject("src/Main.hx", "class Main {}");
    VirtualFile hxml = myFixture.addFileToProject("scripts/completion.hxml", SRC_CLASSPATH_HXML_SOURCE).getVirtualFile();
    VirtualFile scriptsFolder = hxml.getParent();
    HaxeWorkDirectoryStore.getInstance(getProject()).setWorkDirectory(hxml.getPath(), scriptsFolder.getPath());

    assertEquals(scriptsFolder, HaxeBuildWorkDirectories.anchor(getProject(), hxml));
    assertEquals("completion.hxml", HaxeBuildWorkDirectories.fileArgument(getProject(), hxml));

    HaxeWorkDirectoryStore.getInstance(getProject()).setWorkDirectory(hxml.getPath(), null);
    assertEquals(scriptsFolder.getParent(), HaxeBuildWorkDirectories.anchor(getProject(), hxml),
                 "clearing the override must fall back to the sniffed default");
  }

  @Test
  @DisplayName("non hxml files keep their own folder")
  public void testNonHxmlFilesKeepTheirOwnFolder() {
    VirtualFile other = myFixture.addFileToProject("scripts/notes.txt", "notes").getVirtualFile();

    assertEquals(other.getParent(), HaxeBuildWorkDirectories.anchor(getProject(), other));
  }

  @Test
  @DisplayName("build command embeds the anchored file argument")
  public void testBuildCommandEmbedsTheAnchoredFileArgument() {
    myFixture.addFileToProject("src/Main.hx", "class Main {}");
    VirtualFile hxml = myFixture.addFileToProject("scripts/completion.hxml", SRC_CLASSPATH_HXML_SOURCE).getVirtualFile();

    List<String> command = HxmlProjects.buildCommand(getProject(), null, hxml);
    assertEquals("scripts/completion.hxml", command.get(1),
                 "the file argument must resolve from the work directory the command runs in");
  }
}
