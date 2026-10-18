package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.ide.inspections.buildfiles.HaxeMissingLibraryInspection;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeInstalledLibraries.InstalledLibrary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Inspection: missing library")
public class HaxeMissingLibraryInspectionTest extends HaxeLightFixtureTestCase {

  /** haxelib's picture for every test: openfl 9.2.0, lime in two versions with 8.1.0 selected. */
  private static final Map<String, InstalledLibrary> INSTALLED = Map.of(
    "openfl", new InstalledLibrary("9.2.0", Set.of("9.2.0")),
    "lime", new InstalledLibrary("8.1.0", Set.of("8.0.0", "8.1.0")));

  @Override
  protected String getBasePath() {
    // every fixture is inline
    return "";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new HaxeMissingLibraryInspection());
    HaxeInstalledLibraries.getInstance(getProject()).setSnapshotForTests(INSTALLED);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // the shared light project would carry the picture into the next test class
      HaxeInstalledLibraries.getInstance(getProject()).setSnapshotForTests(null);
    }
    finally {
      super.tearDown();
    }
  }

  /** (file name, source with every expected warning marked). */
  static final List<Arguments> MARKED_SOURCES = List.of(
    // hxml: a name haxelib lacks, a pinned version it lacks; names compare case-insensitively
    arguments("build.hxml",
              """
              -lib openfl
              -lib lime:8.0.0
              -lib <warning descr="Library 'nope' is not installed">nope</warning>
              -lib <warning descr="Version 8.2.0 of 'lime' is not installed">lime</warning>:8.2.0
              --library OpenFL
              -cp src
              """),
    // project xml: <haxelib> tags, a version attribute pins
    arguments("project.xml",
              """
              <project>
                <haxelib name="openfl"/>
                <haxelib name="lime" version="8.1.0"/>
                <haxelib name="<warning descr="Library 'nope' is not installed">nope</warning>"/>
                <haxelib name="<warning descr="Version 9.3.0 of 'openfl' is not installed">openfl</warning>" version="9.3.0"/>
                <haxedef name="flag"/>
              </project>
              """),
    // nmml (the NME classification of the project file)
    arguments("build.nmml",
              """
              <project>
                <haxelib name="<warning descr="Library 'nme' is not installed">nme</warning>"/>
              </project>
              """),
    // ignores xml that is not a project file
    arguments("settings.xml",
              """
              <config>
                <haxelib name="nope"/>
              </config>
              """));

  @ParameterizedTest(name = "{0}")
  @FieldSource("MARKED_SOURCES")
  @DisplayName("flags libraries haxelib lacks in build files only")
  public void testFlagsLibrariesHaxelibLacksInBuildFilesOnly(String fileName, String markedSource) {
    myFixture.configureByText(fileName, markedSource);

    myFixture.checkHighlighting();
  }

  @Test
  @DisplayName("reports nothing before the first picture lands")
  public void testReportsNothingBeforeTheFirstPictureLands() {
    HaxeInstalledLibraries.getInstance(getProject()).setSnapshotForTests(null);
    myFixture.configureByText("build.hxml", "-lib nope\n");

    myFixture.checkHighlighting();
  }

  @Test
  @DisplayName("offers the install as the fix")
  public void testOffersTheInstallAsTheFix() {
    myFixture.configureByText("build.hxml", "-lib no<caret>pe\n");

    IntentionAction fix = myFixture.findSingleIntention("Install 'nope' Using Haxelib");

    assertEquals("Install using haxelib", fix.getFamilyName());
  }
}
