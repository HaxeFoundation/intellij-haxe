package com.intellij.plugins.haxe.haxelib;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Parsing of {@code haxelib info} output — field labels, release lines, and the preamble noise the CLI prepends. */
@DisplayName("Haxelib: library info parsing")
public class HaxelibLibraryInfoTest {

  // real output shape: the update banner precedes the fields, releases carry
  // date + version + note, and a note may contain colons of its own
  private static final List<String> LIBRARY_INFO = """

    A new version (4.2.0) of haxelib is available.
    Do `haxelib --global update haxelib` to get the latest version.

    Name: mylib
    Tags:\s
    Desc: A sample library for cross-platform development
    Website: https://example.com/libowner/mylib
    License: MIT
    Owner: libowner
    Version: 8.3.2
    Releases:\s
       2013-12-10 05:45:36 0.9.0 : Preliminary release
       2014-10-14 23:02:00 2.0.0-alpha : Alpha version
       2024-05-01 12:00:00 8.3.2 : Bug fixes: see changelog
    """.lines().toList();

  @Test
  @DisplayName("fields parse through the preamble noise")
  public void testFieldsParseThroughThePreambleNoise() {
    HaxelibLibraryInfo info = HaxelibLibraryInfo.parse(LIBRARY_INFO);
    assertNotNull(info);
    assertEquals("mylib", info.name());
    assertEquals("A sample library for cross-platform development", info.description());
    assertEquals("https://example.com/libowner/mylib", info.website());
    assertEquals("MIT", info.license());
    assertEquals("libowner", info.owner());
    assertEquals("8.3.2", info.latestVersion());
  }

  @Test
  @DisplayName("releases carry date version and note")
  public void testReleasesCarryDateVersionAndNote() {
    HaxelibLibraryInfo info = HaxelibLibraryInfo.parse(LIBRARY_INFO);
    assertNotNull(info);
    assertEquals(3, info.releases().size());
    HaxelibLibraryInfo.Release alpha = info.releases().get(1);
    assertEquals("2.0.0-alpha", alpha.version());
    assertEquals("2014-10-14 23:02:00", alpha.date());
    assertEquals("Alpha version", alpha.note());
  }

  @Test
  @DisplayName("a colon inside a release note does not split it")
  public void testAColonInsideAReleaseNoteDoesNotSplitIt() {
    HaxelibLibraryInfo info = HaxelibLibraryInfo.parse(LIBRARY_INFO);
    assertNotNull(info);
    assertEquals("Bug fixes: see changelog", info.releases().get(2).note());
  }

  @Test
  @DisplayName("output without a name field parses to null")
  public void testOutputWithoutANameFieldParsesToNull() {
    List<String> failure = List.of("Error: no such project someunknownlib");
    assertNull(HaxelibLibraryInfo.parse(failure), "unknown-library/network failures are not an info");
  }
}
