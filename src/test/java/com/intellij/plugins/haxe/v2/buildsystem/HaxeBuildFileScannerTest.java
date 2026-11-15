package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildFilesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.CLASSPATH_ONLY_PROJECT_SOURCE;
import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.LIBRARY_INCLUDE_SOURCE;
import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.MAVEN_POM_SOURCE;
import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.NME_SAMPLE_PROJECT_SOURCE;
import static com.intellij.plugins.haxe.buildsystem.ProjectXmlFixtures.SECTIONED_LIME_PROJECT_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build system: build file scanner")
public class HaxeBuildFileScannerTest extends HaxeCodeInsightFixtureTestCase {

  static final String LIME_PROJECT_SOURCE = """
    <?xml version="1.0" encoding="utf-8"?>
    <project>
      <app main="Main" path="export" file="Shapes"/>
      <source path="src"/>
      <haxelib name="lime"/>
    </project>
    """;

  static final String OPENFL_PROJECT_SOURCE = """
    <project>
      <app main="Main"/>
      <source path="src"/>
      <haxelib name="openfl"/>
    </project>
    """;

  static final String NME_PROJECT_SOURCE = """
    <project>
      <app main="Main"/>
      <classpath name="src"/>
      <haxelib name="nme"/>
    </project>
    """;

  /** (file name, source text, expected type - null when the file is no build file). */
  static final List<Arguments> DETECTIONS = List.of(
    arguments("build.hxml", "-main Main\n", HaxeBuildFileType.HXML),
    arguments("lime-project.xml", LIME_PROJECT_SOURCE, HaxeBuildFileType.LIME),
    arguments("openfl-project.xml", OPENFL_PROJECT_SOURCE, HaxeBuildFileType.OPENFL),
    // lime's own extension classifies like xml
    arguments("lime-project.lime", LIME_PROJECT_SOURCE, HaxeBuildFileType.LIME),
    // <classpath> is lime's alias of <source>
    arguments("classpath-only.xml", CLASSPATH_ONLY_PROJECT_SOURCE, HaxeBuildFileType.LIME),
    // the recognised elements may sit inside a <section>
    arguments("sectioned.xml", SECTIONED_LIME_PROJECT_SOURCE, HaxeBuildFileType.LIME),
    // lime merges a library's include file into the build naming the library and never builds one itself
    arguments("include.xml", LIME_PROJECT_SOURCE, null),
    arguments("include.hxp", "class Include extends HXProject {}\n", null),
    arguments("extension.xml", LIBRARY_INCLUDE_SOURCE, null),
    arguments("project.hxp", "class Project extends HXProject {}\n", HaxeBuildFileType.HXP_PROJECT),
    arguments("nme-project.nmml", NME_PROJECT_SOURCE, HaxeBuildFileType.NMML),
    // the extension picks the nme tool even when the project declares openfl
    arguments("openfl-project.nmml", OPENFL_PROJECT_SOURCE, HaxeBuildFileType.NMML),
    // NME needs neither <haxelib> nor <source>
    arguments("sample-shape.nmml", NME_SAMPLE_PROJECT_SOURCE, HaxeBuildFileType.NMML),
    arguments("no-project.nmml", "<config/>", null),
    arguments("pom.xml", MAVEN_POM_SOURCE, null),
    arguments("notes.txt", "<project/>", null));

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @ParameterizedTest(name = "{0} -> {2}")
  @FieldSource("DETECTIONS")
  @DisplayName("detects the build file type")
  public void testDetectsTheBuildFileType(String fileName, String source, HaxeBuildFileType expected) {
    VirtualFile file = myFixture.addFileToProject(fileName, source).getVirtualFile();

    assertEquals(expected, HaxeBuildFileScanner.detectType(getProject(), file));
  }

  /** (file name, source text detection rejects, type the file's extension gives it once added by hand). */
  static final List<Arguments> ADDED_BY_HAND = List.of(
    arguments("unrecognized.xml", "<project/>", HaxeBuildFileType.LIME),
    arguments("unrecognized.lime", "<project/>", HaxeBuildFileType.LIME),
    arguments("unrecognized.nmml", "<config/>", HaxeBuildFileType.NMML));

  @ParameterizedTest(name = "{0} -> {2}")
  @FieldSource("ADDED_BY_HAND")
  @DisplayName("a file added by hand that detection rejects takes its extension's type")
  public void testAFileAddedByHandThatDetectionRejectsTakesItsExtensionsType(String fileName, String source, HaxeBuildFileType expected) {
    VirtualFile file = myFixture.addFileToProject(fileName, source).getVirtualFile();
    assertNull(HaxeBuildFileScanner.detectType(getProject(), file));

    String containerId = myFixture.getModule().getName();
    HaxeBuildFilesStore store = HaxeBuildFilesStore.getInstance(getProject());
    store.addFile(containerId, file.getPath());
    try {
      assertEquals(expected, HaxeBuildFileScanner.detectType(getProject(), file));
    }
    finally {
      // the shared project keeps its stores between tests
      store.removeFile(containerId, file.getPath());
    }
  }
}
