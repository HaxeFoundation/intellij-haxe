package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  static final String CLASSPATH_ONLY_PROJECT_SOURCE = """
    <project>
      <app main="Main"/>
      <classpath path="src"/>
    </project>
    """;

  static final String NME_PROJECT_SOURCE = """
    <project>
      <app main="Main"/>
      <classpath name="src"/>
      <haxelib name="nme"/>
    </project>
    """;

  static final String NME_SAMPLE_PROJECT_SOURCE = """
    <project>
      <app main="Main" file="Sample"/>
      <window width="800" height="600"/>
    </project>
    """;

  static final String MAVEN_POM_SOURCE = """
    <project>
      <modelVersion>4.0.0</modelVersion>
      <artifactId>shapes</artifactId>
    </project>
    """;

  /** (file name, source text, expected type - null when the file is no build file). */
  static final List<Arguments> DETECTIONS = List.of(
    arguments("build.hxml", "-main Main\n", HaxeBuildFileType.HXML),
    arguments("lime-project.xml", LIME_PROJECT_SOURCE, HaxeBuildFileType.LIME),
    arguments("openfl-project.xml", OPENFL_PROJECT_SOURCE, HaxeBuildFileType.OPENFL),
    // lime's own extension classifies like xml
    arguments("lime-project.lime", LIME_PROJECT_SOURCE, HaxeBuildFileType.LIME),
    arguments("openfl-project.lime", OPENFL_PROJECT_SOURCE, HaxeBuildFileType.OPENFL),
    // <classpath> is lime's alias of <source>
    arguments("classpath-only.xml", CLASSPATH_ONLY_PROJECT_SOURCE, HaxeBuildFileType.LIME),
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
  public void testDetectsTheBuildFileType(String fileName, String source, HaxeBuildFileType expected) {
    VirtualFile file = myFixture.addFileToProject(fileName, source).getVirtualFile();

    assertEquals(expected, HaxeBuildFileScanner.detectType(getProject(), file));
  }
}
