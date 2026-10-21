package com.intellij.plugins.haxe.haxelib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * The layout of a haxelib repository: {@code <repository>/<library>/<version folder>}.
 * A release installs under its version with every dot spelled as a comma
 * ({@code 4,3,7}), a git or hg checkout under the scm's name. The library
 * folder's hidden {@code .current} file names the selected version; its hidden
 * {@code .dev} file holds the path of a development checkout outside the
 * repository.
 */
public final class HaxelibRepositoryLayout {

  /** The project-local repository {@code haxelib newrepo} creates. */
  public static final String LOCAL_REPOSITORY_DIR_NAME = ".haxelib";
  /** Holds the selected version's name, not a path. */
  public static final String CURRENT_VERSION_FILE_NAME = ".current";
  /** Holds the path of the development checkout that stands in for the library. */
  public static final String DEV_POINTER_FILE_NAME = ".dev";
  /** The manifest at a library's root: its name, version and the libraries it requires. */
  public static final String MANIFEST_FILE_NAME = "haxelib.json";
  /** Compiler arguments at a library's root that haxelib adds to every build using the library. */
  public static final String EXTRA_PARAMS_FILE_NAME = "extraParams.hxml";

  // a release version folder: digit groups joined by commas (4,3,7), optionally
  // followed by a prerelease or build suffix in the same spelling (1,0,0-rc,1)
  private static final Pattern RELEASE_DIR_NAME = Pattern.compile("\\d+(?:,\\d+)+(?:[-+].*)?");

  private HaxelibRepositoryLayout() {
  }

  /** The folder a version installs into: dots become commas; a pseudo-version (git, hg, dev) keeps its name. */
  @NotNull
  public static String versionDirName(@NotNull String version) {
    return version.replace('.', ',');
  }

  /** The version a version folder denotes: commas become dots, an scm folder names its pseudo-version; null for any other folder. */
  @Nullable
  public static String versionOf(@NotNull String dirName) {
    if (isScmDirName(dirName)) return dirName;
    return RELEASE_DIR_NAME.matcher(dirName).matches() ? dirName.replace(',', '.') : null;
  }

  /** Whether the folder holds a git or hg checkout. */
  public static boolean isScmDirName(@NotNull String dirName) {
    return HaxelibSemVer.GIT_SCM.equals(dirName) || HaxelibSemVer.MERCURIAL_SCM.equals(dirName);
  }
}
