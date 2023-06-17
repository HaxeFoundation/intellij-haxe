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

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * A convenient way to carry around data about a library.  Unfortunately, it
 * has almost the same data as the library *and* a reference carries.  But those
 * things have different purposes.
 *
 * XXX: Whether this class stays or goes is still an open discussion.  It could
 * easily become a member of a library and all of the duplicate members can be
 * eliminated.  OTOH, it makes the library classes more complex.  Plus, this
 * needs to exist outside of a library, because it is used to pass the parsed
 * data from a classpath around.
 */
public class HaxeLibraryInfo {
  final public String name;
  final public HaxelibSemVer semver;
  final public String relativeClasspath;
  public String versionString;


  HaxeLibraryInfo(@NotNull String name, @NotNull String version, @Nullable String relativeClasspath) {
    this(name, HaxelibSemVer.create(version), null != relativeClasspath ? relativeClasspath : ".");
    if (semver == HaxelibSemVer.ZERO_VERSION) {
      versionString = version;
    }
  }

  HaxeLibraryInfo(@NotNull String name, @NotNull HaxelibSemVer semver, @NotNull String relativeClasspath) {
    this.name = name;
    this.semver = semver;
    this.relativeClasspath = relativeClasspath;
  }

  @NotNull
  public String getClasspath() {
    return relativeClasspath;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public HaxelibSemVer getVersion() {
    return semver;
  }
  @NotNull
  public String  getVersionString() {
    return versionString != null ? versionString : semver.toString();
  }
}
