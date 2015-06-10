/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
public final class HaxelibLibraryCache {

  static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxeLibraryManager");
  {
    LOG.setLevel(Level.DEBUG);
  }

  private final InternalCache myCache;
  ConcurrentSkipListSet<String> knownLibraries;
  final Sdk mySdk;


  public HaxelibLibraryCache(@NotNull Sdk sdk) {
    myCache = new InternalCache();
    knownLibraries = null;
    mySdk = sdk;

    /* TODO: EMB Note: This block of code belongs in HaxelibUtils.getInstalledLibraries.
     *       I'm leaving it here for now, to simplify the merge, but really should be moved.
     */

    /* 'haxelib' has been  enhanced to support 'list-path' command.
     *  (Changes may not have made it out to Open Source yet.)
     * Here, we're checking the output to see if the version of haxelib
     * currently installed supports the new command.  If so, we'll
     * use the new functionality (and save many seconds by not having to
     * fork a new process for each installed library).
     */
    boolean isHaxelibListPathCmdSupported = true;
    final List<String> haxelibCmdOutput = HaxelibCommandUtils.issueHaxelibCommand(sdk, "list-path");
    if ((haxelibCmdOutput.size() < 1) || (haxelibCmdOutput.get(0).contains("Unknown command"))) {
      isHaxelibListPathCmdSupported = false;
    }
    if (isHaxelibListPathCmdSupported) {
        final List<String> installedHaxelibs = new ArrayList<String>();
        // Initialize the internal cache with the list of libraries known to haxelib,
        // using the haxelib path specified in the SDK.
        for (String s : haxelibCmdOutput) {
            // haxelib list-path output format is, library-name:version:install/path
            final String[] haxelibProperties = s.split(":");
            if (haxelibProperties.length < 3) continue; // avoid ArrayIndexOutOfBoundsException
            // Note: when the class HaxelibItem is enhanced to support creating a new instance
            // of it just from name... above line should be changed to '< 1'... also... below
            // code should create only using element [0]... there's a TODO in HaxelibItem.java
            installedHaxelibs.add(haxelibProperties[0]);
            final HaxeClasspath classpath = new HaxeClasspath();
            classpath.add(new HaxelibItem(haxelibProperties[0], haxelibProperties[2]));
            myCache.add(new HaxelibLibraryEntry(haxelibProperties[0], classpath));
        }
        knownLibraries = new ConcurrentSkipListSet<String>(installedHaxelibs);
    }
    /* END of code that should be moved to HaxelibUtils.getInstalledLibraries. */
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

        timeLog.stamp("Loading library classpath:" + libraryName);

        // Try the cache first.
        HaxelibLibraryEntry lib = myCache.get(libraryName);
        if (null != lib) {
          timeLog.stamp("Returning cached results");
          return lib.getClasspathEntries();
        }

        timeLog.stamp("Cache miss");

        // It's not in the cache, so go get it and cache the results.
        HaxeClasspath itemList = findHaxelibPath(libraryName);
        myCache.add(new HaxelibLibraryEntry(libraryName, itemList));

        timeLog.stamp("haxelib finished with " + itemList.size() + " entries");
        return itemList;
      }

      timeLog.stamp("Unknown library !!!  " + libraryName + " !!! ");

      return HaxeClasspath.EMPTY_CLASSPATH;
    }
    finally {
      timeLog.printIfTimeExceeds(2); // Short-timed logs just clutter up the ouput.
    }
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

    HaxelibLibraryEntry cacheEntry = myCache.get(libraryName);
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
   * Encapsulate haxelib output so that it can be cached.
   */
  private final class HaxelibLibraryEntry {
    final String myName;
    final HaxeClasspath myClasspathEntries;

    public HaxelibLibraryEntry(String name, HaxeClasspath classpathEntries) {
      myName = name;
      myClasspathEntries = new HaxeClasspath(classpathEntries);
    }

    public String getName() {
      return myName;
    }

    public HaxeClasspath getClasspathEntries() {
      return myClasspathEntries;
    }
  }

  /**
   * A simple cache of entries.  This is used to cache the return values
   * from the haxelib command.  It should be checked before running
   * haxelib.
   */
  private final class InternalCache {
    final Hashtable<String, HaxelibLibraryEntry> myCache;

    public InternalCache() {
      myCache = new Hashtable<String, HaxelibLibraryEntry>();
    }

    public void add(HaxelibLibraryEntry entry) {
      HaxelibLibraryEntry oldEntry = myCache.put(entry.getName(), entry);
      if (null != oldEntry) {
        LOG.warn("Duplicating cached data for entry " + entry.getName());
      }
    }

    public void clear() {
      myCache.clear();
    }

    @Nullable
    public HaxelibLibraryEntry get(@NotNull String name) {
      return myCache.get(name);
    }
  }


}
