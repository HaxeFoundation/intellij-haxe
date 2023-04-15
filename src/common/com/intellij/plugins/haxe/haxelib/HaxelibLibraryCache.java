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

import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import lombok.CustomLog;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
public final class HaxelibLibraryCache {

  static {
    log.setLevel(LogLevel.DEBUG);
  }

  private final InternalCache myCache;
  private ConcurrentSkipListSet<String> knownLibraries;
  private final Sdk mySdk;


  public HaxelibLibraryCache(@NotNull Sdk sdk) {

    List<String> installedLibs = HaxelibUtil.getInstalledLibraryNames(sdk);

    mySdk = sdk;
    myCache = new InternalCache();
    knownLibraries = new ConcurrentSkipListSet<String>();

    for (String libName : installedLibs) {
      HaxeLibrary lib = HaxeLibrary.load(this, libName, mySdk);
      if (null != lib) {
        myCache.add(lib);
        knownLibraries.add(lib.getName());
      }
    }
  }

  /**
   * Get a union of all of the classpaths for the given libraries.
   *
   * @param libraryNames a set of libraries of current interest.
   * @return a (possibly empty) collection of classpaths.
   */
  @NotNull
  public HaxeClasspath getClasspathForHaxelibs(@Nullable List<String> libraryNames) {
    if (null == libraryNames || libraryNames.isEmpty())
      return HaxeClasspath.EMPTY_CLASSPATH;

    HaxeClasspath paths = new HaxeClasspath(libraryNames.size());
    for (String libName : libraryNames) {
      HaxeClasspath libPath = getClasspathForHaxelib(libName);
      paths.addAll(libPath);
    }
    return paths;
  }

  /**
   * Get the classpath for a specific library.  If it does not reside in
   * the cache, it will be looked up and cached for future use.
   *
   * @param libraryName name of the library of interest.
   * @return a (possibly empty) list of classpaths known for that library.
   */
  @NotNull
  public HaxeClasspath getClasspathForHaxelib(String libraryName) {

    HaxeDebugTimeLog timeLog = HaxeDebugTimeLog.startNew("getClasspathForLibrary",
                                                         HaxeDebugTimeLog.Since.Start);
    try {
      if (libraryIsKnown(libraryName)) {

        timeLog.stamp("Loading library:" + libraryName);

        // Try the cache first.
        HaxeLibrary lib = myCache.get(libraryName);
        if (null != lib) {
          timeLog.stamp("Returning cached results");
          return lib.getClasspathEntries();
        }

        timeLog.stamp("Cache miss");

        // It's not in the cache, so go get it and cache the results.
        HaxeLibrary newlib = HaxeLibrary.load(this, libraryName, mySdk);
        if(null != newlib) {
          myCache.add(newlib);
          timeLog.stamp("Finished loading library: " + libraryName);
          return newlib.getClasspathEntries();
        }
      }

      timeLog.stamp("Unknown library !!!  " + libraryName + " !!! ");

      return HaxeClasspath.EMPTY_CLASSPATH;
    }
    finally {
      timeLog.printIfTimeExceeds(2); // Short-timed logs just clutter up the ouput.
    }
  }

  @Nullable
  public HaxeLibrary getLibrary(String name, HaxelibSemVer requestedVersion) {
    if (libraryIsKnown(name)) {
      HaxeLibrary lib = myCache.get(name);  // We only ever load the "current" one.
      if (null != lib && (null == requestedVersion || requestedVersion.matchesRequestedVersion(lib.getVersion()))) {
        return lib;
      }
    }
    return null;
  }

  @NotNull
  public Sdk getSdk() {
    return mySdk;
  }

  /**
   * Find a library on the haxelib path and return its complete class path.
   *
   * @param libraryName file to find.
   * @return a list of path names in the requested library, if any.
   */
  @NotNull
  public HaxeClasspath findHaxelibPath(@NotNull String libraryName) {
    if (! libraryIsKnown(libraryName)) {
      return HaxeClasspath.EMPTY_CLASSPATH;
    }

    HaxeLibrary cacheEntry = myCache.get(libraryName);
    if (cacheEntry != null) {
      return cacheEntry.getClasspathEntries();
    }

    return HaxelibClasspathUtils.getHaxelibLibraryPath(mySdk, libraryName);
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
      knownLibraries = new ConcurrentSkipListSet<String>(libs);
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
   * Get a list of all of the libraries known to this library manager.
   * @return a (possibly empty) list of all known libraries.
   */
  public List<String> getKnownLibraries() {
    Collection<String> knownLibs = retrieveKnownLibraries();
    ArrayList<String> aryLibs = new ArrayList<String>(knownLibs.size());
    aryLibs.addAll(knownLibs);
    return aryLibs;
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

  /**
   * A simple cache of entries.  This is used to cache the return values
   * from the haxelib command.  It should be checked before running
   * haxelib.
   */
  private final class InternalCache {
    final Hashtable<String, HaxeLibrary> myCache;

    public InternalCache() {
      myCache = new Hashtable<String, HaxeLibrary>();
    }

    public InternalCache(List<HaxeLibrary> entries) {
      this();
      addAll(entries);
    }

    public void addAll(List<HaxeLibrary> entries) {
      for (HaxeLibrary entry : entries) {
        add(entry);
      }
    }

    public void add(HaxeLibrary entry) {
      HaxeLibrary oldEntry = myCache.put(entry.getName(), entry);
      if (null != oldEntry) {
        log.warn("Duplicating cached data for entry " + entry.getName());
      }
    }

    public void clear() {
      myCache.clear();
    }

    @Nullable
    public HaxeLibrary get(@NotNull String name) {
      return myCache.get(name);
    }
  }


}
