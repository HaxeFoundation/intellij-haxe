package com.intellij.plugins.haxe.haxelib;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Haxelib: installed index")
public class HaxelibInstalledIndexTest {

  @Test
  @DisplayName("parse reads names versions and the selected one")
  public void testParseReadsNamesVersionsAndTheSelectedOne() {
    HaxelibInstalledIndex index = HaxelibInstalledIndex.parse(List.of(
      "Checking repository",
      "lime: 8.0.0 [8.1.0]",
      "utest: [dev:/home/username/utest]",
      ""));

    assertEquals(Set.of("lime", "utest"), index.getInstalledLibraries());
    assertEquals(Set.of("8.0.0", "8.1.0"), index.getInstalledVersions("lime"));
    assertEquals("8.1.0", index.getSelectedVersion("lime"));
    assertEquals("dev", index.getSelectedVersion("utest"));
  }

  @Test
  @DisplayName("a notice line is not a library")
  public void testANoticeLineIsNotALibrary() {
    HaxelibInstalledIndex index = HaxelibInstalledIndex.parse(List.of(
      "Warning: Repository requires reformatting. To reformat, run `haxelib fixrepo`.",
      "lime: 8.0.0 [8.1.0]"));

    assertEquals(Set.of("lime"), index.getInstalledLibraries());
  }

  @Test
  @DisplayName("an unknown command answer is an empty index")
  public void testAnUnknownCommandAnswerIsAnEmptyIndex() {
    HaxelibInstalledIndex index = HaxelibInstalledIndex.parse(List.of("Unknown command list"));

    assertTrue(index.getInstalledLibraries().isEmpty());
  }
}
