package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Build tools: project default target and section")
public class HaxeProjectDefaultsStoreTest extends HaxeLightFixtureTestCase {

  private static final List<String> SECTION_IDS = List.of("build.hxml#1", "build.hxml#2", "build.hxml#3");

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // the stores are services of the shared light project
      targets().loadState(new HaxeTargetSelectionStore.State());
      sections().loadState(new HaxeSectionSelectionStore.State());
      defaults().loadState(new HaxeProjectDefaultsStore.State());
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("without a pick the project's default target applies")
  public void testWithoutAPickTheProjectsDefaultTargetApplies() {
    VirtualFile buildFile = buildFile();
    defaults().setTargetId(buildFile, "Windows");

    assertEquals("Windows", targets().getSelectedTargetId(buildFile));
  }

  @Test
  @DisplayName("a pick overrides the project's default target and leaves it")
  public void testAPickOverridesTheProjectsDefaultTargetAndLeavesIt() {
    VirtualFile buildFile = buildFile();
    defaults().setTargetId(buildFile, "Windows");

    targets().setSelectedTargetId(buildFile, "Neko");

    assertEquals("Neko", targets().getSelectedTargetId(buildFile));
    assertEquals("Windows", defaults().getTargetId(buildFile));
  }

  @Test
  @DisplayName("picking the project's default target drops the pick")
  public void testPickingTheProjectsDefaultTargetDropsThePick() {
    VirtualFile buildFile = buildFile();
    defaults().setTargetId(buildFile, "Windows");
    targets().setSelectedTargetId(buildFile, "Neko");

    targets().setSelectedTargetId(buildFile, "Windows");

    defaults().setTargetId(buildFile, "Android");
    assertEquals("Android", targets().getSelectedTargetId(buildFile), "a dropped pick follows the project's default");
  }

  @Test
  @DisplayName("using the picked target as the project's default drops the pick")
  public void testUsingThePickedTargetAsTheProjectsDefaultDropsThePick() {
    VirtualFile buildFile = buildFile();
    targets().setSelectedTargetId(buildFile, "Neko");

    targets().setProjectDefault(buildFile, "Neko");

    assertEquals("Neko", defaults().getTargetId(buildFile));
    defaults().setTargetId(buildFile, "Android");
    assertEquals("Android", targets().getSelectedTargetId(buildFile), "a dropped pick follows the project's default");
  }

  /** (the user's pick, the project's default, the selected section's index); null stores nothing. */
  static final List<Arguments> SECTION_CHOICES = List.of(
    arguments(null, null, 0),
    arguments(null, "build.hxml#2", 1),
    arguments("build.hxml#3", "build.hxml#2", 2),
    // a stored section the hxml no longer has counts as unset
    arguments("build.hxml#9", "build.hxml#2", 1),
    arguments(null, "build.hxml#9", 0));

  @ParameterizedTest(name = "pick {0}, default {1} -> {2}")
  @FieldSource("SECTION_CHOICES")
  @DisplayName("the pick wins over the project's default section, which wins over the first")
  public void testThePickWinsOverTheProjectsDefaultSectionWhichWinsOverTheFirst(@Nullable String picked,
                                                                                  @Nullable String projectDefault,
                                                                                  int expected) {
    VirtualFile buildFile = buildFile();
    if (projectDefault != null) defaults().setSectionId(buildFile, projectDefault);
    if (picked != null) sections().setSelectedSection(buildFile, picked);

    assertEquals(expected, sections().getSelectedSection(buildFile, SECTION_IDS));
  }

  @Test
  @DisplayName("using the picked section as the project's default drops the pick")
  public void testUsingThePickedSectionAsTheProjectsDefaultDropsThePick() {
    VirtualFile buildFile = buildFile();
    sections().setSelectedSection(buildFile, "build.hxml#3");

    sections().setProjectDefault(buildFile, "build.hxml#3");

    assertEquals("build.hxml#3", defaults().getSectionId(buildFile));
    defaults().setSectionId(buildFile, "build.hxml#2");
    assertEquals(1, sections().getSelectedSection(buildFile, SECTION_IDS), "a dropped pick follows the project's default");
  }

  private VirtualFile buildFile() {
    return myFixture.addFileToProject("build.hxml", "-main Main").getVirtualFile();
  }

  private HaxeTargetSelectionStore targets() {
    return HaxeTargetSelectionStore.getInstance(getProject());
  }

  private HaxeSectionSelectionStore sections() {
    return HaxeSectionSelectionStore.getInstance(getProject());
  }

  private HaxeProjectDefaultsStore defaults() {
    return HaxeProjectDefaultsStore.getInstance(getProject());
  }
}
