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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manages library retrieval and caching.
 *
 * This should be instantiated once for each Module.
 * haxelib projects can be configured to use a local haxelib repo that differs from its SDK default.
 * since modules may share SDK but have different installed libraries we have to keep a cache for each module.
 */
@CustomLog
public final class ModuleLibraryCache {

  static {
    log.setLevel(LogLevel.DEBUG);
  }
  private final Hashtable<String, List<HaxeLibrary>> myCache;
  private final Sdk mySdk;
  private final Module myModule;
  private VirtualFile repositoryPath;
  private  HaxelibInstalledIndex haxelibIndex;


  public ModuleLibraryCache(@NotNull Module module, @NotNull Sdk sdk) {

    mySdk = sdk;
    myModule = module;
    myCache = new Hashtable<>();
    haxelibIndex = HaxelibInstalledIndex.EMPTY;


    loadInstalledLibrariesList(sdk);
  }
  public void reload() {
    repositoryPath = null;
    myCache.clear();
    haxelibIndex = HaxelibInstalledIndex.EMPTY;
    loadInstalledLibrariesList(mySdk);
  }

  private void loadInstalledLibrariesList(@NotNull Sdk sdk) {
    if(!HaxelibSdkUtils.isValidHaxeSdk(sdk)){
      log.warn("Unable to load install library list, invalid SDK paths");
      return;
    }
    VirtualFile workDir = getHaxelibWorkDirectory();
    haxelibIndex = HaxelibInstalledIndex.fetchFromHaxelib(sdk, workDir);

    for (String libName : haxelibIndex.getInstalledLibraries()) {
      Set<String> versions = haxelibIndex.getInstalledVersions(libName);
      myCache.put(libName, new ArrayList<>());
      for (String libVersion : versions) {
        HaxeLibrary lib = HaxeLibrary.load(this, libName, libVersion, mySdk);
        if (null != lib) {
          myCache.get(libName).add(lib);
        }
      }
    }
  }

  @Nullable
  public VirtualFile getHaxelibWorkDirectory() {
    return ProjectUtil.guessModuleDir(myModule);
  }


  @Nullable
  public HaxeLibrary getLibrary(String name, @Nullable HaxelibSemVer requestedVersion) {
    if (libraryIsInstalled(name)) {
      List<HaxeLibrary> libVersions = myCache.getOrDefault(name, List.of());
      String haxeLibSelectedVersion = haxelibIndex.getSelectedVersion(name);


      HaxelibSemVer libVersion = Optional.ofNullable(requestedVersion).orElseGet(() -> {
        // if no version specified use haxelib default
        String version = haxeLibSelectedVersion;
        return HaxelibSemVer.create(version);
      });

      if(libVersion == HaxelibSemVer.ANY_VERSION && haxeLibSelectedVersion != null ) {
        HaxelibSemVer selected = HaxelibSemVer.create(haxeLibSelectedVersion);

        Optional<HaxeLibrary> selectedLib = libVersions.stream()
          .filter(library -> selected.matchesRequestedVersion(library.getVersion()))
          .findFirst();

        if (selectedLib.isPresent()) {
          return selectedLib.get();
        }
      }

      Optional<HaxeLibrary> libWithRequestedVersion = libVersions.stream()
        .filter(library -> libVersion.matchesRequestedVersion(library.getVersion()))
        .findFirst();

      if (libWithRequestedVersion.isPresent()) {
        return libWithRequestedVersion.get();
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
    if (haxelibIndex == HaxelibInstalledIndex.EMPTY) {
      VirtualFile workDir = getHaxelibWorkDirectory();
      haxelibIndex = HaxelibInstalledIndex.fetchFromHaxelib(mySdk, workDir);
    }
    return haxelibIndex.getInstalledLibraries();
  }

  /**
   * Tell if a given library is known to haxelib.
   *
   * @param libraryName the library of interest.  Case sensitive!
   * @return true if the library is found, false otherwise.
   */
  public boolean libraryIsInstalled(String libraryName) {
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

    HaxeLibraryInfo info = HaxelibUtil.deriveLibraryInfoFromPath(this, path);
    if (null != info) {
      return getLibrary(info.name, info.semver);
    }
    return null;
  }

  public VirtualFile getRepositoryPath() {
    return repositoryPath;
  }

  public void setRepositoryPath(VirtualFile root) {
    repositoryPath  = root;
  }
}
