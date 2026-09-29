package com.intellij.plugins.haxe.haxelib;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Haxelib: semantic version")
public class HaxelibSemVerTest {

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"1.2.3", "9.2.0-rc.1", "1.0.0-alpha.0", "1.2.3+build.5"})
  @DisplayName("release versions accepted including pre-release suffixes")
  public void releaseVersionsAcceptedIncludingPreReleaseSuffixes(String version) {
    assertTrue(HaxelibSemVer.isReleaseVersion(version));
  }

  /**
   * Pseudo versions, partial versions and source specs are not releases; the
   * comma form is haxelib's DIRECTORY naming, not a version argument.
   */
  // "[{index}] {0}": the empty-string row would otherwise render a BLANK
  // display name, which the platform rejects
  @ParameterizedTest(name = "[{index}] {0}")
  @NullSource
  @ValueSource(strings = {"", "any", "git", "hg", "dev", "8.0", "8,0,2", "git:https://example.invalid/repo.git"})
  @DisplayName("non release version arguments rejected")
  public void nonReleaseVersionArgumentsRejected(String version) {
    assertFalse(HaxelibSemVer.isReleaseVersion(version));
  }

  @Test
  @DisplayName("create renders prerelease and build metadata")
  public void createRendersPrereleaseAndBuildMetadata() {
    assertEquals("9.2.0-rc.1", HaxelibSemVer.create("9.2.0-rc.1").toString());
    assertEquals("1.0.0-alpha.0+build.5", HaxelibSemVer.create("1.0.0-alpha.0+build.5").toString());
  }

  @Test
  @DisplayName("prerelease splits identity but build metadata does not")
  public void prereleaseSplitsIdentityButBuildMetadataDoesNot() {
    assertNotEquals(HaxelibSemVer.create("1.0.0-rc.1"), HaxelibSemVer.create("1.0.0"));
    assertNotEquals(HaxelibSemVer.create("1.0.0-rc.1"), HaxelibSemVer.create("1.0.0-rc.2"));
    // build metadata is ignored for precedence, so it does not split identity
    assertEquals(HaxelibSemVer.create("1.0.0+b1"), HaxelibSemVer.create("1.0.0+b2"));
  }

  @Test
  @DisplayName("comma directory forms parse as versions")
  public void commaDirectoryFormsParseAsVersions() {
    assertEquals(HaxelibSemVer.create("8.0.2"), HaxelibSemVer.create("8,0,2"));
    assertEquals(HaxelibSemVer.create("1.0.0-rc.1"), HaxelibSemVer.create("1,0,0-rc,1"));
  }

  @Test
  @DisplayName("dir string renders the comma form")
  public void dirStringRendersTheCommaForm() {
    assertEquals("1,0,0-rc,1", HaxelibSemVer.create("1.0.0-rc.1").toDirString());
    assertEquals("8,0,2", HaxelibSemVer.create("8.0.2").toDirString());
  }

  /** (create argument, the pseudo-version constant it maps to; garbage maps to zero). */
  static final List<Arguments> PSEUDO_VERSIONS = List.of(
    arguments(null, HaxelibSemVer.ANY_VERSION),
    arguments("any", HaxelibSemVer.ANY_VERSION),
    arguments("git", HaxelibSemVer.GIT_VERSION),
    arguments("hg", HaxelibSemVer.HG_VERSION),
    arguments("dev", HaxelibSemVer.DEVELOPMENT_VERSION),
    arguments("not-a-version", HaxelibSemVer.ZERO_VERSION));

  @ParameterizedTest(name = "{0}")
  @FieldSource("PSEUDO_VERSIONS")
  @DisplayName("create maps pseudo versions to constants and garbage to zero")
  public void createMapsPseudoVersionsToConstantsAndGarbageToZero(String argument, HaxelibSemVer expected) {
    assertSame(expected, HaxelibSemVer.create(argument));
  }
}
