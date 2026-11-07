package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A build's source directories: its declared class paths and the generated folders the build tool compiles beside them. */
@DisplayName("Build tools: build classpaths")
public class HaxeBuildClasspathsTest extends HaxeCodeInsightFixtureTestCase {
  private static final String PROJECT_XML_SOURCE = """
    <?xml version="1.0" encoding="utf-8"?>
    <project>
      <app main="Main" path="Export" file="Game"/>
      <source path="Source"/>
      <haxelib name="openfl"/>
    </project>
    """;

  @Override
  protected String getBasePath() {
    // every fixture is created in-project by the test itself
    return "";
  }

  @AfterEach
  public void resetSetting() {
    HaxeFrameworkTargetSettings.getInstance().setMarksGeneratedSources(Framework.OPENFL, true);
  }

  @Test
  @DisplayName("the selected targets generated folders are source directories while they exist")
  public void testTheSelectedTargetsGeneratedFoldersAreSourceDirectoriesWhileTheyExist() throws IOException {
    HaxeBuildFile buildFile = openflBuildFile();
    createDirectory("Source");
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(buildFile.file(), "HashLink");
    List<String> beforeBuild = HaxeBuildClasspaths.sourceDirectories(getProject(), buildFile.file().getPath());
    assertEquals(List.of(path("Source")), beforeBuild, "nothing generated yet");

    createDirectory("Export/hl/haxe/_generated");

    List<String> afterBuild = HaxeBuildClasspaths.sourceDirectories(getProject(), buildFile.file().getPath());
    assertEquals(List.of(path("Source"), path("Export/hl/haxe"), path("Export/hl/haxe/_generated")), afterBuild);
  }

  @Test
  @DisplayName("generated folders count regardless of the generated sources setting")
  public void testGeneratedFoldersCountRegardlessOfTheGeneratedSourcesSetting() throws IOException {
    HaxeBuildFile buildFile = openflBuildFile();
    createDirectory("Source");
    createDirectory("Export/html5/haxe");
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(buildFile.file(), "HTML5");
    // the setting decides what the IDE marks as roots; the compiler compiles the folders either way
    HaxeFrameworkTargetSettings.getInstance().setMarksGeneratedSources(Framework.OPENFL, false);

    List<String> directories = HaxeBuildClasspaths.sourceDirectories(getProject(), buildFile.file().getPath());

    assertEquals(List.of(path("Source"), path("Export/html5/haxe")), directories);
  }

  private HaxeBuildFile openflBuildFile() {
    VirtualFile projectXml = myFixture.addFileToProject("project.xml", PROJECT_XML_SOURCE).getVirtualFile();
    return new HaxeBuildFile(projectXml, HaxeBuildFileType.OPENFL);
  }

  private void createDirectory(String relative) throws IOException {
    myFixture.getTempDirFixture().findOrCreateDir(relative);
  }

  private String path(String relative) {
    return ModuleRootManager.getInstance(getModule()).getContentRoots()[0].getPath() + "/" + relative;
  }
}
