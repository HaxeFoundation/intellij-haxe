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

import com.intellij.plugins.haxe.util.HaxeStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manage semantic versioning according to Haxe library rules.  (See the 'haxelib' tool
 * and conditional compilation comparison rules.)
 */
public class HaxelibSemVer {

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

  private static final String GIT_SCM = "git";
  private static final String MERCURIAL_SCM = "hg";
  private static final String DEV = "dev";
  public static final ConstantVer ANY_VERSION = new ConstantVer(0,0,0, "any");
  public static final ConstantVer DEVELOPMENT_VERSION = new ConstantVer(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE, DEV);
  public static final ConstantVer GIT_VERSION = new ConstantVer(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE, GIT_SCM);

  public static final ConstantVer HG_VERSION = new ConstantVer(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE, MERCURIAL_SCM);
  public static final HaxelibSemVer ZERO_VERSION = new HaxelibSemVer(0,0,0);

  public static final String VERSION_REGEX = "([0-9]+)[,.]([0-9]+)[,.]([0-9]+)";
  private static final Pattern versionPattern = Pattern.compile(VERSION_REGEX);

  public static boolean isAny(HaxelibSemVer semVer) {
     return semVer == ANY_VERSION;
  }



  private int major;
  private int minor;
  private int patch;

  private HaxelibSemVer(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  /**
   * Create a new semver based upon the string passed in.
   * @param semver Semantic version string.  Either comma or dot separated are accepted.
   * @return A new instance for strings that match the semantic versioning pattern.
   *         Strings that do not match the semantic versioning pattern will return ZERO_VERSION
   *         if they are non-empty, and ANY_VERSION if semver is empty or null.
   */
  @NotNull
  public static HaxelibSemVer create(@Nullable String semver) {
    if (null == semver || semver.isEmpty()) {
      return ANY_VERSION;
    }

    Matcher matcher = versionPattern.matcher(semver);
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
    return new HaxelibSemVer(Integer.parseInt(matcher.group(1)),
                             Integer.parseInt(matcher.group(2)),
                             Integer.parseInt(matcher.group(3)));

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
   * Converts this version into a float for comparison according to Haxe's rules.
   * TODO: Hook this up to the #if...#endif processing.
   */
  public Float toCompareValue() {
    StringBuilder v = new StringBuilder();
    v.append(major);
    v.append('.');
    v.append(minor);
    v.append(patch);
    return Float.valueOf(v.toString());
  }

  @NotNull
  public String toDirString() {
    return HaxeStringUtil.join(",", Integer.toString(major), Integer.toString(minor), Integer.toString(patch));
  }

  @Override
  public String toString() {
    return HaxeStringUtil.join(".", Integer.toString(major), Integer.toString(minor), Integer.toString(patch));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxelibSemVer ver = (HaxelibSemVer)o;

    if (major != ver.major) return false;
    if (minor != ver.minor) return false;
    return patch == ver.patch;
  }



  @Override
  public int hashCode() {
    int result = major;
    result = 31 * result + minor;
    result = 31 * result + patch;
    return result;
  }
}
