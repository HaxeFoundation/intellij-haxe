package com.intellij.plugins.haxe.v2.buildsystem;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code --next} chains split through the HXML grammar's PSI - the root
 * file's chain entries are the sections, each expanded to its own effective
 * content. Files are created in-project so real PSI backs every case.
 */
@DisplayName("Build system: build file sections")
public class HaxeBuildFileSectionsTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    // every fixture is created in-project by the test itself
    return "";
  }

  @Test
  @DisplayName("splits on root next and applies the each block")
  public void testSplitsOnRootNextAndAppliesTheEachBlock() {
    VirtualFile root = addHxml("inline/build.hxml", """
      -cp src
      -lib utest
      --each
      -js bin/app.js
      --next
      -hl bin/app.hl
      """);

    List<String> sections = HaxeBuildFileInspector.sectionContents(getProject(), root);
    assertEquals(2, sections.size());
    for (String section : sections) {
      HaxeBuildFileInfo info = HxmlFileParser.parse(section);
      assertEquals(List.of("src"), info.classpaths(), "the --each block applies to every section");
      assertEquals(1, info.libraries().size());
    }
    assertEquals(HaxeTarget.JAVA_SCRIPT, HxmlFileParser.parse(sections.get(0)).target());
    assertEquals(HaxeTarget.HL, HxmlFileParser.parse(sections.get(1)).target());
  }

  @Test
  @DisplayName("each section expands its includes independently")
  public void testEachSectionExpandsItsIncludesIndependently() {
    // a file EVERY section includes must expand into every one of them, not
    // just the first
    addHxml("shared/common.hxml", "-lib utest\n");
    addHxml("shared/neko.hxml", "common.hxml\n-neko bin/app.n\n");
    addHxml("shared/js.hxml", "common.hxml\n-js bin/app.js\n");
    VirtualFile root = addHxml("shared/build.hxml", """
      neko.hxml
      --next js.hxml
      """);

    List<String> sections = HaxeBuildFileInspector.sectionContents(getProject(), root);
    assertEquals(2, sections.size());
    for (String section : sections) {
      int libraryCount = HxmlFileParser.parse(section).libraries().size();
      assertEquals(1, libraryCount, "the shared include must reach every section: " + section);
    }
    assertEquals(HaxeTarget.NEKO, HxmlFileParser.parse(sections.get(0)).target());
    assertEquals(HaxeTarget.JAVA_SCRIPT, HxmlFileParser.parse(sections.get(1)).target());
    assertEquals("neko.hxml", HxmlFileParser.leadingIncludedFile(sections.get(0)));
    assertEquals("js.hxml", HxmlFileParser.leadingIncludedFile(sections.get(1)));
  }

  @Test
  @DisplayName("an included files internal next stays inside its section")
  public void testAnIncludedFilesInternalNextStaysInsideItsSection() {
    // the root's chain entries are the units: a file chaining a prep step and
    // a build internally is ONE selectable section, and compiling it scoped
    // still runs both halves (haxe splits the section's own --next itself)
    addHxml("nested/cs.hxml", """
      -cmd echo prep
      --next
      --main TestMain
      -cs bin/cs
      """);
    VirtualFile root = addHxml("nested/build.hxml", """
      -js bin/app.js
      --next cs.hxml
      """);

    List<String> sections = HaxeBuildFileInspector.sectionContents(getProject(), root);
    assertEquals(2, sections.size(), "the internal --next must not create a third section");
    String csSection = sections.get(1);
    assertTrue(csSection.contains("-cmd echo prep"), "the prep step travels with its build: " + csSection);
    assertEquals(HaxeTarget.CSHARP, HxmlFileParser.parse(csSection).target());

    List<String> ids = HxmlFileParser.sectionIds(root.getName(), sections);
    assertEquals(List.of("build.hxml", "cs.hxml"), ids);
  }

  @Test
  @DisplayName("a lone trailing next produces no section")
  public void testALoneTrailingNextProducesNoSection() {
    // the compiler silently skips an empty section
    VirtualFile root = addHxml("trailing/build.hxml", """
      -js bin/app.js
      --next
      """);

    List<String> sections = HaxeBuildFileInspector.sectionContents(getProject(), root);
    assertEquals(1, sections.size(), "a single real build means a single section");
  }

  private VirtualFile addHxml(String relativePath, String content) {
    return myFixture.addFileToProject(relativePath, content).getVirtualFile();
  }
}
