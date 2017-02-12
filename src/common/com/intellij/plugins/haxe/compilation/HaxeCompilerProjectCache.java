/*
 * Copyright 2000-2013 Eric Bishton
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
package com.intellij.plugins.haxe.compilation;

import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by ebishton on 2/11/17.
 *
 * Cache to keep compiler arguments that are expensive to calculate.
 * (For example, when the Haxe project target is OpenFL, lime is run to extract
 * the arguments from the project.xml.  Those results are cached here.)
 *
 * This cache employs a file watcher to ensure that internal or external changes
 * to the cache are acted upon.
 */
public class HaxeCompilerProjectCache {

  static final List<String> EMPTY_LIST = new ArrayList<String>(0);


  // We're only keeping one file for now, to keep things simple.
  private cacheEntry myCacheEntry = null;
  private class cacheEntry {

    public Module myModule;
    public String myFilename; // project file name (from module, but fully located path that the compiler will use).
    public String myTarget;
    public List<String> myArguments;

    public cacheEntry(Module module, String projectFile, String target, List<String> args) {
      init(module, projectFile, target, args);
    }

    public void init(Module module, String projectFile, String target, List<String> args) {
      myModule = module;
      myFilename = null != projectFile ? projectFile : "";
      myTarget = null != target ? target : "";
      myArguments = null != args ? args : EMPTY_LIST;
    }
  }


  public HaxeCompilerProjectCache() {
    myCacheEntry = null;

    VirtualFileManager mgr = VirtualFileManager.getInstance();
    mgr.addVirtualFileListener(new VirtualFileAdapter() {
      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        updateCache(event);
      }

      @Override
      public void contentsChanged(@NotNull VirtualFileEvent event) {
        updateCache(event);
      }

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        updateCache(event);
      }

      private void updateCache(VirtualFileEvent event) {
        // If it changed, kick it out of the cache.
        if (null != myCacheEntry && event.getFile().getUrl().equals(myCacheEntry.myFilename)) {
          myCacheEntry = null;
        }
      }
    });
  }

  public void put(Module module, String projectFileUri, String target, List<String> args) {
    // Put always kicks out the previous value, so just re-use the object.
    if (null == myCacheEntry) {
      myCacheEntry = new cacheEntry(module, projectFileUri, target, args);
    } else {
      myCacheEntry.init(module, projectFileUri, target, args);
    }
  }

  private boolean stringsAreEqual(String left, String right) {
    if (null != left && null != right) {
      return left.equals(right);
    }
    return false;
  }

  @Nullable
  public List<String> get(Module module, String projectFileUri, String target) {
    if (null != myCacheEntry
        &&  myCacheEntry.myModule.equals(module)
        &&  stringsAreEqual(myCacheEntry.myFilename, projectFileUri)
        &&  stringsAreEqual(myCacheEntry.myTarget,target)) {
      return myCacheEntry.myArguments;
    }
    return null;
  }

  public void invalidate() {
    myCacheEntry = null;
  }

}
