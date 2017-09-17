/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.util.io.URLUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * An entry in a classpath.
 */
public class HaxeClasspathEntry {
  private static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  //static { LOG.setLevel(Level.DEBUG);}

  String myName;
  String myUrl;
  boolean myIsManagedEntry;

  public HaxeClasspathEntry(@Nullable String name, @NotNull String url) {
    myName = name;
    myUrl = url;

    // Try to fix the URL if it wasn't correct.
    if (!url.contains(URLUtil.SCHEME_SEPARATOR)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Fixing malformed URL passed by " + HaxeDebugUtil.printCallers(5));
      }
      VirtualFileSystem vfs = VirtualFileManager.getInstance().getFileSystem(LocalFileSystem.PROTOCOL);
      VirtualFile file = vfs.findFileByPath(url);
      if (null != file) {
        myUrl = file.getUrl();
      }
    }

    if (null != myName) {
      if (HaxelibNameUtil.isManagedLibrary(myName)) {
        myName = HaxelibNameUtil.parseHaxelib(myName);
        myIsManagedEntry = true;
      }
    }
  }

  @Nullable
  public String getName() {
    if (null == myName) {
      myName = HaxelibNameUtil.parseHaxelibNameFromPath(myUrl);
    }

    if (myIsManagedEntry) {
      return HaxelibNameUtil.stringifyHaxelib(myName);
    }
    return myName;
  }

  @NotNull
  public String getUrl() {
    return myUrl;
  }

  @Override
  final public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeClasspathEntry)) return false;

    HaxeClasspathEntry entry = (HaxeClasspathEntry)o;

    if (!myUrl.equals(entry.myUrl)) return false;

    return true;
  }

  @Override
  public final int hashCode() {
    return myUrl.hashCode();
  }

  public boolean isManagedEntry() {
    return myIsManagedEntry;
  }

  public void markAsManagedEntry() {
    myIsManagedEntry = true;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("ClasspathEntry");
    sb.append(":hash="); sb.append(hashCode());
    sb.append(":");      sb.append(myUrl);
    return sb.toString();
  }

  /**
   * Return a hashcode identical to the hash code for this type of object.
   * This is used for searching the HaxeClasspath, which uses a hash table to
   * manage its entries.
   *
   * @param url
   * @return
   */
  public final static int hashUrl(String url) {
    return url.hashCode();
  }
}
