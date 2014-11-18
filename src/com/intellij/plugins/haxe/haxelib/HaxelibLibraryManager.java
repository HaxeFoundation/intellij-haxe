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
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil;
import com.intellij.plugins.haxe.util.HaxeDebugTimeLog;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Manages library retrieval and caching.
 *
 * This should be instantiated once for each project opened.  (Projects,
 * particularly those that keep separate versions of the libraries in
 * source control using separate branches, are not necessarily using the
 * same haxe installation.)
 */
public final class HaxelibLibraryManager {

  static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxeLibraryManager");
  {
    LOG.setLevel(Level.DEBUG);
  }

  final HaxelibLibraryCache myCache;
  ConcurrentSkipListSet<String> knownLibraries;
  final Sdk mySdk;


  public HaxelibLibraryManager(@NotNull Sdk sdk) {
    myCache = new HaxelibLibraryCache();
    knownLibraries = null;
    mySdk = sdk;
  }

  public List<HaxelibItem> getClasspathForLibrary(String libraryName) {

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
        List<HaxelibItem> itemList = findHaxelibPath(libraryName);
        myCache.add(new HaxelibLibraryEntry(libraryName, itemList));

        timeLog.stamp("haxelib finished with " + itemList.size() + " entries");
        return itemList;
      }

      timeLog.stamp("Unknown library !!!  " + libraryName + " !!! ");

      return HaxelibItem.EMPTY_LIST;
    }
    finally {

      timeLog.printIfTimeExceeds(2); // Short-timed logs just clutter up the ouput.
    }
  }


  /**
   * Find a file on the haxelib path and return its complete path name.
   *
   * @param libraryName file to find.
   * @return a list of possible path names for the requested file, if any.
   */
  @NotNull
  public List<HaxelibItem> findHaxelibPath(@NotNull String libraryName) {
    List<HaxelibItem> haxelibNewItems;

    if (libraryIsKnown(libraryName)) {
      haxelibNewItems = HaxelibClasspathUtils.getHaxelibLibraryPath(mySdk, libraryName);
    } else {
      haxelibNewItems = HaxelibItem.EMPTY_LIST;
    }

    return haxelibNewItems;
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
    Collection<String> knownLibs = getKnownLibraries();
    ArrayList<String> aryLibs = new ArrayList<String>(knownLibs.size());
    aryLibs.addAll(knownLibs);
    return aryLibs;
  }

  /**
   * Encapsulate haxelib output so that it can be cached.
   */
  final class HaxelibLibraryEntry {
    final String myName;
    final ArrayList<HaxelibItem> myClasspathEntries;

    public HaxelibLibraryEntry(String name, List<HaxelibItem> classpathEntries) {
      // Deep copy.   XXX: May not be necessary.
      myName = name;
      myClasspathEntries = new ArrayList<HaxelibItem>(classpathEntries);
    }

    public String getName() {
      return myName;
    }

    public ArrayList<HaxelibItem> getClasspathEntries() {
      return myClasspathEntries;
    }
  }

  /**
   * A simple cache of entries.  This is used to cache the return values
   * from the haxelib command.  It should be checked before running
   * haxelib.
   */
  final class HaxelibLibraryCache {
    final Hashtable<String, HaxelibLibraryEntry> myCache;

    public HaxelibLibraryCache() {
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
