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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import lombok.CustomLog;
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
@CustomLog
public class ProjectLibraryCacheManager implements Disposable {

  static {
    log.setLevel(LogLevel.DEBUG);
  }


  final Hashtable<Module, SdkEntry> myCacheMap = new Hashtable<>();

  public ProjectLibraryCacheManager() {
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
  public ModuleLibraryCache getLibraryManager(@NotNull Module module) {
    Sdk sdk = HaxelibSdkUtils.lookupSdk(module);
    if(myCacheMap.containsKey(module)) {
      return myCacheMap.get(module).getLibraryCache();
    }else {
      SdkEntry entry = new SdkEntry(module, sdk);
      myCacheMap.put(module,  entry);
      return entry.getLibraryCache();
    }
  }

  public void clear() {
    myCacheMap.clear();
  }

  public void removeInstance(Module module) {
    myCacheMap.remove(module);
  }

  @Override
  public void dispose() {
    myCacheMap.clear();
  }


  /**
   * Cache entry for the SDK table.
   */
  static final class SdkEntry {
    final Module module;
    final ModuleLibraryCache myMgr;

    public SdkEntry(@NotNull Module module, @NotNull Sdk sdk) {
      this.module = module;
      myMgr = new ModuleLibraryCache(module, sdk);
    }

    @NotNull
    public String getName() {
      return module.getName();
    }

    @NotNull
    public ModuleLibraryCache getLibraryCache() {
      return myMgr;
    }

  }
}
