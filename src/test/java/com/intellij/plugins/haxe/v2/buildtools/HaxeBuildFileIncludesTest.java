package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The imported configuration files of a build file: hxml references resolved
 * against the work directory at every nesting level, and the
 * {@code <include>} project files of lime-family xml, with folder includes,
 * cycles, unresolvable paths and files outside the content roots handled.
 */
@DisplayName("Build tools: build file includes")
public class HaxeBuildFileIncludesTest extends HaxeCodeInsightFixtureTestCase {

  private static final String EMPTY_PROJECT_XML_SOURCE = "<project/>";

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("hxml references are collected through every nesting level")
  public void testHxmlReferencesAreCollectedThroughEveryNestingLevel() {
    VirtualFile build = addFile("build.hxml", """
      common.hxml
      -main Main
      missing.hxml
      --next other.hxml
      """);
    VirtualFile common = addFile("common.hxml", """
      -cp src
      config/deep.hxml
      """);
    VirtualFile deep = addFile("config/deep.hxml", "-D deep");
    VirtualFile other = addFile("other.hxml", "-D other");

    Set<String> includes = includesOf(build, HaxeBuildFileType.HXML);

    assertEquals(Set.of(common.getPath(), deep.getPath(), other.getPath()), includes);
  }

  @Test
  @DisplayName("hxml references resolve against the work directory not the referencing file")
  public void testHxmlReferencesResolveAgainstTheWorkDirectoryNotTheReferencingFile() {
    myFixture.addFileToProject("src/Main.hx", "class Main {}");
    VirtualFile rootCommon = addFile("common.hxml", "-D root");
    addFile("scripts/common.hxml", "-D scripts");
    VirtualFile build = addFile("scripts/completion.hxml", """
      -cp src
      -main Main
      common.hxml
      """);

    Set<String> includes = includesOf(build, HaxeBuildFileType.HXML);

    assertEquals(Set.of(rootCommon.getPath()), includes,
                 "src/ resolves at the content root, so the compiler runs there and reads the root's common.hxml");
  }

  @Test
  @DisplayName("xml includes are collected without evaluating conditions")
  public void testXmlIncludesAreCollectedWithoutEvaluatingConditions() {
    VirtualFile project = addFile("project.xml", """
      <project>
        <haxelib name="lime"/>
        <include path="defs.xml"/>
        <include path="conf" if="html5"/>
        <include name="legacy.xml" unless="debug"/>
        <include path="${haxelib:openfl}/include.xml"/>
        <include haxelib="openfl"/>
        <include path="missing.xml"/>
        <assets path="img"/>
      </project>
      """);
    VirtualFile defs = addFile("defs.xml", EMPTY_PROJECT_XML_SOURCE);
    VirtualFile confInclude = addFile("conf/include.xml", EMPTY_PROJECT_XML_SOURCE);
    VirtualFile legacy = addFile("legacy.xml", EMPTY_PROJECT_XML_SOURCE);
    addFile("img/logo.svg", "<svg/>");

    Set<String> includes = includesOf(project, HaxeBuildFileType.LIME);

    assertEquals(Set.of(defs.getPath(), confInclude.getPath(), legacy.getPath()), includes);
  }

  @Test
  @DisplayName("folder include takes the first of include lime nmml xml")
  public void testFolderIncludeTakesTheFirstOfIncludeLimeNmmlXml() {
    VirtualFile project = addFile("project.xml", "<project><include path=\"conf\"/></project>");
    VirtualFile limeInclude = addFile("conf/include.lime", EMPTY_PROJECT_XML_SOURCE);
    addFile("conf/include.nmml", EMPTY_PROJECT_XML_SOURCE);
    addFile("conf/include.xml", EMPTY_PROJECT_XML_SOURCE);

    Set<String> includes = includesOf(project, HaxeBuildFileType.LIME);

    assertEquals(Set.of(limeInclude.getPath()), includes);
  }

  @Test
  @DisplayName("nested xml includes recurse and cycles are cut")
  public void testNestedXmlIncludesRecurseAndCyclesAreCut() {
    VirtualFile project = addFile("project.xml", "<project><include path=\"a.xml\"/></project>");
    VirtualFile a = addFile("a.xml", "<project><include path=\"b.xml\"/></project>");
    VirtualFile b = addFile("b.xml", """
      <project>
        <include path="a.xml"/>
        <include path="project.xml"/>
      </project>
      """);

    Set<String> includes = includesOf(project, HaxeBuildFileType.OPENFL);

    assertEquals(Set.of(a.getPath(), b.getPath()), includes, "the build file itself is not one of its includes");
  }

  @Test
  @DisplayName("files outside the content roots are not watched")
  public void testFilesOutsideTheContentRootsAreNotWatched() {
    String outsidePath = FileUtil.toSystemIndependentName(getTestDataPath() + "targets/lime-project.xml");
    VirtualFile outside = LocalFileSystem.getInstance().refreshAndFindFileByPath(outsidePath);
    assertNotNull(outside, "the fixture file must exist outside the test project");
    VirtualFile project = addFile("project.xml", "<project><include path=\"" + outside.getPath() + "\"/></project>");

    Set<String> includes = includesOf(project, HaxeBuildFileType.LIME);

    assertTrue(includes.isEmpty(), "a file no content root covers cannot be watched");
  }

  @Test
  @DisplayName("hxp scripts import nothing")
  public void testHxpScriptsImportNothing() {
    VirtualFile script = addFile("project.hxp", "class Project extends HXProject {}");

    assertTrue(includesOf(script, HaxeBuildFileType.HXP_PROJECT).isEmpty());
  }

  private VirtualFile addFile(String relativePath, String text) {
    return myFixture.addFileToProject(relativePath, text).getVirtualFile();
  }

  private Set<String> includesOf(VirtualFile file, HaxeBuildFileType type) {
    return HaxeBuildFileIncludes.configurationIncludes(getProject(), new HaxeBuildFile(file, type));
  }
}
