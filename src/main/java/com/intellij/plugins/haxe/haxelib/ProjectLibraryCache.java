/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
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

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.projectRoots.Sdk;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Manages library retrieval and caching.
 *
 * This should be instantiated once for each SDK in the project.  (Projects,
 * particularly those that keep separate versions of the libraries in
 * source control using separate branches, are not necessarily using the
 * same haxe installation.)
 */
@CustomLog
public final class ProjectLibraryCache {

  static {
    log.setLevel(LogLevel.DEBUG);
  }
  private ConcurrentSkipListSet<String> knownLibraries;
  private final Hashtable<String, HaxeLibrary> myCache;
  private final Sdk mySdk;


  public ProjectLibraryCache(@NotNull Sdk sdk) {


    mySdk = sdk;
    myCache = new Hashtable<>();
    knownLibraries = new ConcurrentSkipListSet<>();


    loadInstalledLibrariesList(sdk);
  }
  public void reload() {
    myCache.clear();
    knownLibraries = new ConcurrentSkipListSet<>();
    loadInstalledLibrariesList(mySdk);
  }

  private void loadInstalledLibrariesList(@NotNull Sdk sdk) {
    List<String> installedLibs = HaxelibUtil.getInstalledLibraryNames(sdk);
    for (String libName : installedLibs) {
      HaxeLibrary lib = HaxeLibrary.load(this, libName, mySdk);
      if (null != lib) {
        myCache.put(lib.getName(), lib);
        knownLibraries.add(lib.getName());
      }
    }
  }



  @Nullable
  public HaxeLibrary getLibrary(String name, HaxelibSemVer requestedVersion) {
    if (libraryIsKnown(name)) {
      HaxeLibrary lib = myCache.get(name);  // We only ever load the "current" one.
      if (null != lib && (null == requestedVersion || requestedVersion.matchesRequestedVersion(lib.getVersion()))) {
        return lib;
      } else {

      }
    }
    return null;
  }

  @NotNull
  public Sdk getSdk() {
    return mySdk;
  }


  /**
   * Retrieve the known libraries, first from the cache, then, if missing,
   * from haxelib.
   *
   * @return a collection of known libraries.
   */
  @NotNull
  private Collection<String> retrieveKnownLibraries() {
    // If we don't have the list, then load it.
    if (null == knownLibraries) {
      List<String> libs = HaxelibClasspathUtils.getInstalledLibraries(mySdk);
      knownLibraries = new ConcurrentSkipListSet<>(libs);
    }
    return knownLibraries;
  }

  /**
   * Tell if a given library is known to haxelib.
   *
   * @param libraryName the library of interest.  Case sensitive!
   * @return true if the library is found, false otherwise.
   */
  public boolean libraryIsKnown(String libraryName) {
    return retrieveKnownLibraries().contains(libraryName);
  }


  /**
   * Lookup a library from its path (classpath entry).
   *
   * @param path Potential path to a library.
   * @return a library matching that path.
   */
  @Nullable
  public HaxeLibrary getLibraryByPath(String path) {
    // Looking up a library in the cache resolves to a serial search.  Instead,
    // parse the path for a library name and version, and then look it up.

    HaxeLibraryInfo info = HaxelibUtil.deriveLibraryInfoFromPath(mySdk, path);
    if (null != info) {
      return getLibrary(info.name, info.semver);
    }
    return null;
  }

}
