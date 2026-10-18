package com.intellij.plugins.haxe.haxelib;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Haxelib: repository layout")
public class HaxelibRepositoryLayoutTest {

  /** (version, the folder it installs into). */
  static final List<Arguments> VERSION_FOLDERS = List.of(
    arguments("4.3.7", "4,3,7"),
    arguments("1.0.0-rc.1", "1,0,0-rc,1"),
    // pseudo-versions keep their name
    arguments("git", "git"),
    arguments("dev", "dev"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("VERSION_FOLDERS")
  @DisplayName("a version installs under its comma spelled folder")
  public void aVersionInstallsUnderItsCommaSpelledFolder(String version, String folder) {
    assertEquals(folder, HaxelibRepositoryLayout.versionDirName(version));
  }

  /** (folder name, the version it denotes; null for a folder that is no version folder). */
  static final List<Arguments> FOLDER_VERSIONS = List.of(
    arguments("4,3,7", "4.3.7"),
    arguments("1,0,0-rc,1", "1.0.0-rc.1"),
    arguments("git", "git"),
    arguments("hg", "hg"),
    // a dev checkout lives outside the repository, behind the .dev pointer
    arguments("dev", null),
    // a plain folder, a dotted file name and a lone number are no version folders
    arguments("src", null),
    arguments("haxelib.json", null),
    arguments("4", null));

  @ParameterizedTest(name = "{0}")
  @FieldSource("FOLDER_VERSIONS")
  @DisplayName("a folder name denotes its version")
  public void aFolderNameDenotesItsVersion(String folder, String version) {
    assertEquals(version, HaxelibRepositoryLayout.versionOf(folder));
  }
}
