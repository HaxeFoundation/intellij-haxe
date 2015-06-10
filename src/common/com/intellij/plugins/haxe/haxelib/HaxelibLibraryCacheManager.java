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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;

/**
 * Manage a cache of SDKs.  In actuality, this class is really just a
 * cache of library managers, keyed by SDK name.  For most projects, there
 * will only be one in use.
 *
 * If we need more speed or compactness, we could track the library managers
 * by the actual haxelib executable path.  SDKs specify a path which
 * multiple SDKs could use, but, in practice, probably don't.
 *
 * This is a pretty small class and putting this functionality directly into
 * HaxelibLibraryManager is an option.  This implementation just allowed for
 * fewer changes to that api, which was becoming less useful/manageable as I
 * started adding SDK and/or Module parameters everywhere.
 */
public class HaxelibLibraryCacheManager {

  static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxeLibraryManager");
  static {
    LOG.setLevel(Level.DEBUG);
  }


  final SdkCache mySdkCache;

  public HaxelibLibraryCacheManager() {
    mySdkCache = new SdkCache();
  }

  /**
   * Get the library manager associated with the given module.  This will
   * be either using the SDK specified on the module, or the one inherited
   * from the project, if applicable.
   *
   * @param module - module to look up libraries for.
   * @return a library manager for the given module.
   */
  @NotNull
  public HaxelibLibraryCache getLibraryManager(@NotNull Module module) {
    Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
    return getLibraryCache(sdk);
  }

  /**
   * Get the library manager associated with the given SDK.  There is
   * only one per project with the given name (e.g. modules that specify
   * the same SDK will get the same library manager object).  Therefore,
   * if all modules specify the same SDK it is functionally identical to
   * inheriting the project SDK.
   *
   * @param sdk - SDK to look up libraries for.
   * @return a library manager for the given SDK.
   */
  @NotNull
  public HaxelibLibraryCache getLibraryCache(@NotNull Sdk sdk) {
    SdkEntry entry = mySdkCache.get(sdk.getName());
    if (null == entry) {
      // Not in the cache?  Put it there.
      entry = new SdkEntry(sdk);
      mySdkCache.add(entry);
    }
    return entry.getLibraryCache();
  }

  /**
   * Cache entry for the SDK table.
   */
  final class SdkEntry {
    final Sdk mySdk;
    final HaxelibLibraryCache myMgr;

    public SdkEntry(@NotNull Sdk sdk) {
      mySdk = sdk;
      myMgr = new HaxelibLibraryCache(sdk);
    }

    @NotNull
    public String getName() {
      return mySdk.getName();
    }

    @NotNull
    public HaxelibLibraryCache getLibraryCache() {
      return myMgr;
    }

    public boolean nameEquals(@NotNull SdkEntry entry) {
      return getName().equals(entry.getName());
    }

    public boolean nameEquals(@NotNull String name) {
      return getName().equals(name);
    }
  }

  /**
   * Cache of library managers per SDK.
   */
  final class SdkCache {
    // Optimize the common case where we only have one...
    SdkEntry cacheOfOne = null;

    Hashtable<String, SdkEntry> myCache;

    public SdkCache() {
    }

    /**
     * Lazy initialization.
     */
    private void initCache() {
      myCache = new Hashtable<String, SdkEntry>();
    }

    /**
     * Add a library manager to the cache.  If a library manager of the same
     * name already exists, the old one will be kept and the new ignored.
     *
     * @param entry An SdkEntry object
     */
    public void add(@NotNull SdkEntry entry) {
      if (null == myCache) {
        if (null == cacheOfOne ) {
          cacheOfOne = entry;
        } else if (! cacheOfOne.nameEquals(entry)) {  // Don't need to add another of the same name.
          initCache();
          add(entry);
          add(cacheOfOne);
          cacheOfOne = null;
        }
      } else {
        if (! myCache.containsKey(entry.getName())) {  // Don't replace a full cache with an empty one.
          SdkEntry oldEntry = myCache.put(entry.getName(), entry);
          if (null != oldEntry) {
            LOG.warn("Duplicating cached data for entry " + entry.getName());
          }
        }
      }
    }

    public void clear() {
      cacheOfOne = null;
      if (null != myCache) {
        myCache.clear();
      }
    }

    @Nullable
    public SdkEntry get(@NotNull String name) {
      if (null != cacheOfOne && cacheOfOne.nameEquals(name)) {
        return cacheOfOne;
      }
      if (null != myCache) {
        return myCache.get(name);
      }
      return null;
    }
  }

}
