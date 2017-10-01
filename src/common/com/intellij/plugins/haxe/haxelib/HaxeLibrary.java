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

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.InvalidParameterException;
import java.util.*;

public class HaxeLibrary {

  private static HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  static {LOG.setLevel(Level.DEBUG);}  // Remove when finished debugging.

  private static String CURRENT_DIR = ".";

  final private HaxelibLibraryCache myCache;
  final private String myName;
  final private String myLibraryRoot;
  final private String myRelativeClasspath;
  final private HaxelibMetadata myMetadata;
  final private HaxelibSemVer mySemVer;
  // TODO: Add the extraParams.hxml data here.  Use the hxml parser; see LimeUtil.getLimeProjectModel() as an example.


  private HaxeLibrary(@NotNull String name, @NotNull VirtualFile libraryRoot, @NotNull HaxelibLibraryCache owner) {
    myCache = owner;
    myLibraryRoot = libraryRoot.getUrl();

    myMetadata = HaxelibMetadata.load(libraryRoot);
    HaxeLibraryInfo pathInfo = HaxelibUtil.deriveLibraryInfoFromPath(owner.getSdk(), libraryRoot.getPath());

    String mdname = myMetadata.getName();
    if (null != mdname && !mdname.isEmpty()) {
      myName = mdname;
    } else if (null != name && !name.isEmpty()) {
      myName = name;
    } else {
      myName = pathInfo.getName();
    }
    assert myName != null;

    HaxelibSemVer semVer = HaxelibSemVer.create(myMetadata.getVersion());
    if (HaxelibSemVer.ZERO_VERSION == semVer) {
      semVer = pathInfo.getVersion();
    }
    mySemVer = semVer;

    String cp = myMetadata.getClasspath();
    if (null == cp || cp.isEmpty()) {
      cp = pathInfo.getClasspath();
    }
    if (null != cp && !cp.isEmpty()) {
      myRelativeClasspath = cp;
    } else {
      myRelativeClasspath = CURRENT_DIR;
    }
  }

  /**
   * Get the list of libraries that this library depends upon (but not /their/ dependents).
   *
   * Private because it's only used by collectDependents.
   * Returns a List instead of a HaxeLibraryList to keep synchronized access down.
   */
  @NotNull
  private List<HaxeLibraryDependency> getDirectDependents() {
    List<HaxelibMetadata.Dependency> mdDependencies = myMetadata.getDependencies();
    if (null == mdDependencies || mdDependencies.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    List<HaxeLibraryDependency> dependencies = new ArrayList<HaxeLibraryDependency>(mdDependencies.size());
    for (HaxelibMetadata.Dependency md : mdDependencies) {
      HaxeLibraryDependency newdep = new HaxeLibraryDependency(myCache, md.getName(), md.getVersion(), this);
      dependencies.add(newdep);
    }
    return dependencies;
  }

  /**
   * Get all dependent libraries in search order.
   */
  @NotNull
  public HaxeLibraryList collectDependents() {
    LinkedHashSet<HaxeLibraryDependency> collection = new LinkedHashSet<HaxeLibraryDependency>();
    collectDependentsInternal(collection);
    return new HaxeLibraryList(myCache.getSdk(), collection);
  }

  private void collectDependentsInternal(/*modifies*/ final @NotNull Collection<HaxeLibraryDependency> collection) {
    List<HaxeLibraryDependency> dependencies = getDirectDependents();

    for (HaxeLibraryDependency dependency : dependencies) {
      if (!collection.contains(dependency)) { // Don't go down the same path again...
        // TODO: Deal with version mismatches here.  Add multiple versions, but don't add a specific version if the latest version is equal to it.
        collection.add(dependency);
        HaxeLibrary depLib = dependency.getLibrary();
        if (null != depLib) {
          depLib.collectDependentsInternal(collection);
        } // TODO: Else mark dependency unfulfilled somehow??
      }
    }
  }

  /**
   * Get the internal name of the library.
   */
  @NotNull
  public String getName() {
    return myName;
  }

  /**
   * Get the display name of the library.
   */
  @NotNull
  public String getPresentableName() {
    // TODO: Figure out what extra decorations we might need, like the version, 'dependency of', etc.
    return getName();
  }

  @Nullable
  public HaxeClasspath getClasspathEntries() {
    HaxeClasspath cp = new HaxeClasspath();
    cp.add(getSourceRoot());
    return cp;
  }

  @NotNull
  public HaxeClasspathEntry getSourceRoot() {
    if (CURRENT_DIR == myRelativeClasspath) {
      return getLibraryRoot();
    }
    return new HaxeClasspathEntry(myName, HaxeFileUtil.joinPath(myLibraryRoot, myRelativeClasspath));
  }

  @NotNull
  public HaxeClasspathEntry getLibraryRoot() {
    return new HaxeClasspathEntry(myName, myLibraryRoot);
  }

  @NotNull
  public HaxelibSemVer getVersion() {
    return mySemVer;
  }

  /**
   * Load a library from disk.  This *DOES NOT* place the library into the library manager.
   *
   * @param libName - name of the library (as haxelib understands it) to load.
   * @return the loaded HaxeLibrary of the given name; null if not found.
   */
  @Nullable
  public static HaxeLibrary load(HaxelibLibraryCache owner, String libName, Sdk sdk) {
    // Ask haxelib for the path to this library.
    VirtualFile libraryRoot = HaxelibUtil.getLibraryRoot(sdk, libName);
    if (null == libraryRoot) {
      // XXX: This case might occur if the library is not managed by haxelib, but then
      //      that should be a classpath, not a lib.
      return null;
    }

    try {
      return new HaxeLibrary(libName, libraryRoot, owner);
    } catch (InvalidParameterException e) {
      ; // libName must not have been an url
    }
    return null;
  }

  /**
   * Create a new reference for this library.
   * @param isManaged whether or not this reference is a "managed reference".
   */
  @NotNull
  public HaxeLibraryReference createReference(boolean isManaged) {
    return new HaxeLibraryReference(myCache, myName, mySemVer, isManaged);
  }

  /**
   * Create a new unmanaged reference for this library.
   */
  @NotNull
  public HaxeLibraryReference createReference() {
    return new HaxeLibraryReference(myCache, myName, mySemVer);
  }

  @NotNull
  public HaxeLibraryReference createReference(HaxelibSemVer override) {
    return new HaxeLibraryReference(myCache, myName, override);
  }
}
