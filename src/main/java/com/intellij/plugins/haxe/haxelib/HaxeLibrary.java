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

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;

import com.intellij.plugins.haxe.util.HaxeFileUtil;
import lombok.CustomLog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.InvalidParameterException;
import java.util.*;

@CustomLog
public class HaxeLibrary {

  static {      // Take this out when finished debugging.
    log.setLevel(LogLevel.DEBUG);
  }

  private static String CURRENT_DIR = ".";

  final private ProjectLibraryCache myCache;
  final private String myName;
  final private String myLibraryRoot;
  final private String myRelativeClasspath;
  final private HaxelibMetadata myMetadata;
  final private HaxelibSemVer mySemVer;
  // TODO: Add the extraParams.hxml data here.  Use the hxml parser; see LimeUtil.getLimeProjectModel() as an example.


  private HaxeLibrary(@NotNull String name, @NotNull VirtualFile libraryRoot, @NotNull ProjectLibraryCache owner) {
    myCache = owner;
    myLibraryRoot = libraryRoot.getUrl();

    myMetadata = HaxelibMetadata.load(libraryRoot);
    HaxeLibraryInfo pathInfo = HaxelibUtil.deriveLibraryInfoFromPath(owner.getSdk(), libraryRoot.getPath());

    String mdname = myMetadata.getName();
    if (null != mdname && !mdname.isEmpty()) {
      myName = mdname;
    } else if (!name.isEmpty()) {
      myName = name;
    } else {
      myName = pathInfo == null ? "" : pathInfo.getName();
    }

    HaxelibSemVer semVer = HaxelibSemVer.create(myMetadata.getVersion());
    if (HaxelibSemVer.ZERO_VERSION == semVer && pathInfo != null) {
      semVer = pathInfo.getVersion();
    }
    mySemVer = semVer;

    String cp = myMetadata.getClasspath();
    if ((null == cp || cp.isEmpty()) && pathInfo != null) {
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
      return Collections.emptyList();
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
    LinkedHashMap<String, HaxeLibraryDependency> collection = new LinkedHashMap<String, HaxeLibraryDependency>();
    collectDependentsInternal(collection);
    HaxeLibraryList list = new HaxeLibraryList(myCache.getSdk());
    for (HaxeLibraryDependency dep : collection.values()) {
      list.add(dep);
    }
    return list;
  }

  private void collectDependentsInternal(/*modifies*/ final @NotNull LinkedHashMap<String, HaxeLibraryDependency> collection) {
    List<HaxeLibraryDependency> dependencies = getDirectDependents();

    for (HaxeLibraryDependency dependency : dependencies) {
      if (!collection.containsKey(dependency.getKey())) { // Don't go down the same path again...
        // TODO: Deal with version mismatches here.  Add multiple versions, but don't add a specific version if the latest version is equal to it.
        collection.put(dependency.getKey(), dependency);
        HaxeLibrary depLib = dependency.getLibrary();
        if (null != depLib) {
          depLib.collectDependentsInternal(collection);
        } // TODO: Else mark dependency unfulfilled somehow??
      } else {
        HaxeLibraryDependency contained = collection.get(dependency.getKey());
        log.assertTrue(contained != null, "Couldn't get a contained object.");
        if (contained != null) {
          contained.addReliant(dependency);
        }
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
  public static HaxeLibrary load(ProjectLibraryCache owner, String libName, String libVersion, Sdk sdk) {
    // Ask haxelib for the path to this library.
    VirtualFile libraryRoot = HaxelibUtil.getLibraryRoot(sdk, libName,libVersion);
    if (null == libraryRoot) {
      // XXX: This case might occur if the library is not managed by haxelib, but then
      //      that should be a classpath, not a lib.
      return null;
    }

    try {
      return new HaxeLibrary(libName, libraryRoot, owner);
    } catch (InvalidParameterException e) {
      ; // libName must not have been an url
    } catch (Exception e) {
      log.error("Unable to read Haxelib '" + libName +"' reason:" + e.getMessage(), e);
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

  /**
   * Test whether this library is effectively the same as a Library appearing
   * in IDEA's library tables.
   *
   * @param lib - Library to test.
   * @return true if this library uses the same sources as the IDEA library; false otherwise.
   */
  public boolean matchesIdeaLib(Library lib) {
    if (null == lib) {
      return false;
    }

    HaxeClasspath cp = getClasspathEntries();
    VirtualFile[] sources = lib.getFiles(OrderRootType.SOURCES);
    for (VirtualFile file : sources) {
      if (!cp.containsUrl(file.getUrl())) {
        return false;
      }
    }
    return cp.size() == sources.length;
  }
}
