/*
 * Copyright 2017 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.haxelib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manage semantic versioning according to Haxe library rules.  (See the 'haxelib' tool
 * and conditional compilation comparison rules.)
 */
public class HaxelibSemVer implements Comparable<HaxelibSemVer> {

  public static class ConstantVer extends HaxelibSemVer {
    public final String name;
    public ConstantVer(int major, int minor, int patch, String name) {
      super(major, minor, patch);
      this.name = name;
    }
    @Override public boolean matchesRequestedVersion(HaxelibSemVer requestedVersion) {
      if(requestedVersion instanceof ConstantVer constantVer) {
        return constantVer.name.equals(name);
      }
      return false;
    }
    @Override public String toString() { return name; }
    // Don't override equals or hashcode.
    @NotNull
    public String toDirString() {
      return name;
    }
  }

  /** The git pseudo-version name — also haxelib's on-disk directory for the checkout. */
  public static final String GIT_SCM = "git";
  private static final String MERCURIAL_SCM = "hg";
  /** The dev pseudo-version name (the {@code .dev} pointer file's target). */
  public static final String DEV = "dev";

  public static final ConstantVer ANY_VERSION = new ConstantVer(0,0,0, "any");
  public static final ConstantVer DEVELOPMENT_VERSION = new ConstantVer(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE, DEV);
  public static final ConstantVer GIT_VERSION = new ConstantVer(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE, GIT_SCM);

  public static final ConstantVer HG_VERSION = new ConstantVer(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE, MERCURIAL_SCM);
  public static final HaxelibSemVer ZERO_VERSION = new HaxelibSemVer(0,0,0);

  public static final String VERSION_REGEX = "([0-9]+)[,.]([0-9]+)[,.]([0-9]+)";
  public static final Pattern versionPattern = Pattern.compile(VERSION_REGEX);

  public static boolean isAny(HaxelibSemVer semVer) {
     return semVer == ANY_VERSION;
  }

  /** Whether the string names the dev or git pseudo-version rather than a release. */
  public static boolean isPseudoVersion(String version) {
    return DEV.equals(version) || GIT_SCM.equals(version);
  }

  // the semver.org 2.0.0 grammar (its suggested regex, as Java named groups):
  // major.minor.patch, no leading zeros, optional -prerelease (dot-separated
  // alphanumeric/hyphen identifiers) and optional +buildmetadata
  private static final Pattern SEMVER_PATTERN = Pattern.compile(
    "(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)"
    + "(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"
    + "(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?");

  /**
   * Whether the string names one concrete release that haxelib commands accept
   * as a version argument ({@code install}/{@code set}): a dot-form semver
   * release ({@code 1.2.3}, {@code 9.2.0-rc.1}). False for null/blank,
   * pseudo-versions (any/git/hg/dev), source specs and directory comma forms.
   */
  public static boolean isReleaseVersion(@Nullable String version) {
    return version != null && SEMVER_PATTERN.matcher(version).matches();
  }

  private int major;
  private int minor;
  private int patch;
  @Nullable private String prerelease;
  // per semver, build metadata is ignored for precedence/equality
  @Nullable private String buildMetadata;

  private HaxelibSemVer(int major, int minor, int patch) {
    this(major, minor, patch, null, null);
  }

  private HaxelibSemVer(int major, int minor, int patch, @Nullable String prerelease, @Nullable String buildMetadata) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.prerelease = prerelease;
    this.buildMetadata = buildMetadata;
  }

  /**
   * Create a new semver based upon the string passed in.
   * @param semver Semantic version string: {@code major.minor.patch} with optional
   *               {@code -prerelease} and {@code +buildmetadata} parts. Comma
   *               separators (haxelib's directory naming, {@code 1,0,0-rc,1})
   *               normalize to dots before parsing.
   * @return A new instance for strings that match the semantic versioning pattern.
   *         Strings that do not match the semantic versioning pattern will return ZERO_VERSION
   *         if they are non-empty, and ANY_VERSION if semver is empty or null.
   */
  @NotNull
  public static HaxelibSemVer create(@Nullable String semver) {
    if (null == semver || semver.isEmpty()) {
      return ANY_VERSION;
    }

    Matcher matcher = SEMVER_PATTERN.matcher(semver.replace(',', '.'));
    if (!matcher.matches()) {
      if (ANY_VERSION.name.equals(semver)) {
        return ANY_VERSION;
      }
      return switch (semver.toLowerCase()) {
        case  MERCURIAL_SCM -> HG_VERSION;
        case  GIT_SCM -> GIT_VERSION;
        case  DEV -> DEVELOPMENT_VERSION;
        default -> ZERO_VERSION;
      };

    }
    return new HaxelibSemVer(Integer.parseInt(matcher.group("major")),
                             Integer.parseInt(matcher.group("minor")),
                             Integer.parseInt(matcher.group("patch")),
                             matcher.group("prerelease"),
                             matcher.group("buildmetadata"));

  }
  @Nullable
  public static HaxelibSemVer create(float f) {
    String[] split = String.valueOf(f).split("\\.");
    int major = Integer.parseInt(split[0]);
    int minor = split.length > 1 ? Integer.parseInt(split[1]) : 0;
    int patch = 0;
    return new HaxelibSemVer(major, minor,  patch);

  }

  /**
   * Checks whether this version number matches the requested version.  Takes into
   * account haxelib rules regarding development and empty/missing requirements.
   *
   * @param requestedVersion
   * @return
   */
  public boolean matchesRequestedVersion(HaxelibSemVer requestedVersion) {
    if (ANY_VERSION == requestedVersion) {
      return true;
    }
    return equals(requestedVersion);
  }

  /**
   * The compiler's {@code #if version("...")} parse: exactly
   * {@code major.minor.patch} with an optional {@code -prerelease} - build
   * metadata and shorter forms are rejected (the compiler hard-errors on
   * them; callers map null to an unevaluable condition).
   */
  @Nullable
  public static HaxelibSemVer parseCompilerVersion(@NotNull String version) {
    Matcher matcher = SEMVER_PATTERN.matcher(version);
    if (!matcher.matches() || matcher.group("buildmetadata") != null) {
      return null;
    }
    return new HaxelibSemVer(Integer.parseInt(matcher.group("major")),
                             Integer.parseInt(matcher.group("minor")),
                             Integer.parseInt(matcher.group("patch")),
                             matcher.group("prerelease"),
                             null);
  }

  /**
   * semver.org 2.0.0 precedence: numeric parts first; a release outranks any
   * of its prereleases; prerelease segments compare pairwise (numeric pairs
   * as numbers, numeric below alphanumeric, otherwise ASCII) with the longer
   * list winning a shared prefix. Build metadata never counts.
   */
  @Override
  public int compareTo(@NotNull HaxelibSemVer other) {
    if (major != other.major) return Integer.compare(major, other.major);
    if (minor != other.minor) return Integer.compare(minor, other.minor);
    if (patch != other.patch) return Integer.compare(patch, other.patch);
    if (prerelease == null && other.prerelease == null) return 0;
    if (prerelease == null) return 1;
    if (other.prerelease == null) return -1;
    return comparePrereleases(prerelease, other.prerelease);
  }

  private static int comparePrereleases(String left, String right) {
    // prerelease segments are separated by literal dots
    String[] leftSegments = left.split("\\.");
    String[] rightSegments = right.split("\\.");
    for (int i = 0; i < Math.min(leftSegments.length, rightSegments.length); i++) {
      int order = comparePrereleaseSegment(leftSegments[i], rightSegments[i]);
      if (order != 0) return order;
    }
    return Integer.compare(leftSegments.length, rightSegments.length);
  }

  private static int comparePrereleaseSegment(String left, String right) {
    boolean leftNumeric = left.chars().allMatch(Character::isDigit);
    boolean rightNumeric = right.chars().allMatch(Character::isDigit);
    if (leftNumeric && rightNumeric) return Long.compare(Long.parseLong(left), Long.parseLong(right));
    if (leftNumeric != rightNumeric) return leftNumeric ? -1 : 1;
    return left.compareTo(right);
  }

  /** Haxelib's on-disk directory name for this version: every dot becomes a comma ({@code 1,0,0-rc,1}). */
  @NotNull
  public String toDirString() {
    return toString().replace('.', ',');
  }

  @Override
  public String toString() {
    StringBuilder version = new StringBuilder();
    version.append(major).append('.').append(minor).append('.').append(patch);
    if (prerelease != null) version.append('-').append(prerelease);
    if (buildMetadata != null) version.append('+').append(buildMetadata);
    return version.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxelibSemVer ver = (HaxelibSemVer)o;

    if (major != ver.major) return false;
    if (minor != ver.minor) return false;
    if (patch != ver.patch) return false;
    // build metadata excluded: semver ignores it for precedence
    return Objects.equals(prerelease, ver.prerelease);
  }



  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + patch;
    result = 31 * result + Objects.hashCode(prerelease);
    return result;
  }
}
