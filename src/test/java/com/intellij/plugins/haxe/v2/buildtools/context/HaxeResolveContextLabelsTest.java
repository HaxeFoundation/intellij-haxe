package com.intellij.plugins.haxe.v2.buildtools.context;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInspector;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeSectionSelectionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Resolve context: labels")
public class HaxeResolveContextLabelsTest extends HaxeCodeInsightFixtureTestCase {
  private static final String SECTIONS_HXML_SOURCE = """
    -cp src
    -main Main
    -hl bin/app.hl
    --next
    -cp src
    -main Main
    -js bin/app.js
    """;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a single section hxml reads as its name")
  public void testASingleSectionHxmlReadsAsItsName() {
    HaxeResolveContext context = context("build.hxml", "-cp src\n-main Main\n-hl bin/app.hl\n");

    assertEquals("build.hxml", HaxeResolveContextLabels.label(getProject(), context));
  }

  @Test
  @DisplayName("a multi section hxml carries its selected section")
  public void testAMultiSectionHxmlCarriesItsSelectedSection() {
    HaxeResolveContext context = context("sections.hxml", SECTIONS_HXML_SOURCE);

    assertEquals("sections.hxml [HashLink]", HaxeResolveContextLabels.label(getProject(), context));

    VirtualFile file = context.buildFile().file();
    List<String> sections = HaxeBuildFileInspector.sectionContents(getProject(), file);
    String secondSectionId = HxmlFileParser.sectionIds(file.getName(), sections).get(1);
    HaxeSectionSelectionStore.getInstance(getProject()).setSelectedSection(file, secondSectionId);
    assertEquals("sections.hxml [JavaScript]", HaxeResolveContextLabels.label(getProject(), context));
  }

  private HaxeResolveContext context(String name, String content) {
    VirtualFile file = myFixture.addFileToProject(name, content).getVirtualFile();
    return new HaxeResolveContext(new HaxeBuildFile(file, HaxeBuildFileType.HXML));
  }
}
