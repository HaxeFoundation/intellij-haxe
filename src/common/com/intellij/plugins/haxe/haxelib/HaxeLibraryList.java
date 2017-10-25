/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
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
import com.intellij.openapi.roots.ModuleRootManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manage a list of Haxe libraries.  The same library will NOT appear twice in a single
 * list.
 */
public class HaxeLibraryList {

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxeLibraryList");

  /**
   * Minimum size to use for new lists.
   */
  public static final int MINIMUM_SIZE = 8;

  // Any access of myOrderedEntries MUST be synchronized if this classpath is
  // used in a multi-threaded environment.
  // myOrderedEntries is lazily allocated, to reduce unnecessary churn.
  private Set<HaxeLibraryReference> myOrderedEntries;
  final private int mySizeHint;
  final private Sdk mySdk;

  /**
   * Create a deep copy of a library list.  All references are cloned.
   * @param initialList list to copy.
   */
  HaxeLibraryList(HaxeLibraryList initialList) {
    this(initialList.mySdk);
    synchronized(initialList) {
      if (initialList.hasEntries()) {
        Set<HaxeLibraryReference> entries = getEntries(initialList.size());
        for (HaxeLibraryReference ref : initialList.getEntries()) {
          entries.add(ref.clone());
        }
      }
    }
  }

  HaxeLibraryList(@NotNull Sdk sdk, Collection<? extends HaxeLibraryReference> references) {
    this(sdk);
    getEntries(references.size()).addAll(references);
  }

  HaxeLibraryList(@NotNull Module module) {
    this(HaxelibSdkUtils.lookupSdk(module));
  }

  HaxeLibraryList(@NotNull Sdk sdk) {
    this(sdk, MINIMUM_SIZE);
  }

  HaxeLibraryList(@NotNull Sdk sdk, int sizeHint) {
    myOrderedEntries = null;
    mySdk = sdk;
    mySizeHint = sizeHint;
  }

  /**
   * Use this to access the orderEntries.  Do not access them directly -- they are
   * allocated lazily.
   *
   * @return the order entries for this object.
   */
  protected Set<HaxeLibraryReference> getEntries() {
    return getEntries(mySizeHint);
  }

  /**
   * Use this to access the orderEntries.  Do not access them directly -- they are
   * allocated lazily.
   * @param sizeHint - A hint to the allocator, if allocation is necessary.
   * @return the order entries for this object.
   */
  protected Set<HaxeLibraryReference> getEntries(int sizeHint) {
    synchronized (this) {
      if (null == myOrderedEntries) {
        sizeHint = Integer.max(this.mySizeHint, sizeHint);
        if (sizeHint < MINIMUM_SIZE) {
          sizeHint = MINIMUM_SIZE;
        }
        myOrderedEntries = new LinkedHashSet<HaxeLibraryReference>(2 * sizeHint);
      }
      return myOrderedEntries;
    }
  }

  /**
   * Determine whether there are any entries in the list.  Used to avoid
   * the allocation of getEntries() when we're only reading the list.
   * @return
   */
  protected boolean hasEntries() {
    synchronized (this) {
      return null != myOrderedEntries && !myOrderedEntries.isEmpty();
    }
  }

  /**
   * Add an entry to the end of this classpath.  If the entry with the same
   * URL already exists, the current one is maintained, along with its current
   * position, and the new one is ignored.
   *
   * @param item to add to the end of the list.
   */
  public void add(@Nullable HaxeLibraryReference item) {
    if (null == item)
      return;

    synchronized(this) {
      if (!contains(item))
        getEntries().add(item);
    }
  }

  /**
   * Add a set of libraries to this list. New entries
   * will be added at the end of this list.  Duplicate entries are ignored
   * (current entries with the same URL are maintained, with their current
   * ordering).
   *
   * @param entries to add to the end of this classpath.  Entries must be
   *                externally synchronized.
   */
  public void addAll(@Nullable Collection<HaxeLibraryReference> entries) {
    if (null == entries || entries.isEmpty())
      return;

    synchronized(this) {
      getEntries(entries.size()).addAll(entries);
    }
  }

  /**
   * Add more libraries to this list.  New entries from the other libraries
   * will be added at the end of this list.  Duplicate entries are ignored
   * (current entries with the same URL are maintained, with their current
   * ordering).
   *
   * @param libraries to add to this libraries
   */
  public void addAll(HaxeLibraryList libraries) {
    if (null == libraries || libraries.isEmpty())
      return;

    synchronized(this) {
      synchronized(libraries) {
        getEntries(libraries.size()).addAll(libraries.getEntries());
      }
    }
  }

  /**
   * Remove all entries from this classpath.
   */
  public void clear() {
    synchronized(this) {
      if (hasEntries())
        getEntries().clear();
    }
  }

  /**
   * Determine whether this list contains an entry that semantically matches
   * the given entry.  (That is, it represents the same file system path.)
   *
   * @param item
   * @return
   */
  public boolean contains(@Nullable HaxeLibraryReference item) {
    if (null == item)
      return false;

    synchronized(this) {
      if (hasEntries())
        return getEntries().contains(item);
      return false;
    }
  }

  /**
   * Get an actual classpath for a library list.
   *
   * @param includeLibRoot Whether library roots are included. (e.g. haxelib/mylib/2,0,2)
   * @param includeSrcRoot Whether library source roots are included (when different from libRoot).
   * @return a HaxeClasspath containing directories that correspond to the libraries in this list.
   */
  @NotNull
  public HaxeClasspath getLibraryClasspaths(boolean includeLibRoot, boolean includeSrcRoot) {
    if (!hasEntries()) {
      return HaxeClasspath.EMPTY_CLASSPATH;
    }
    synchronized(this) {
      Set<HaxeLibraryReference> entries = getEntries();
      HaxeClasspath classpath = new HaxeClasspath(entries.size());
      for (HaxeLibraryReference ref : entries) {
        HaxeLibrary lib = ref.getLibrary();
        if (null != lib) { // May not be available...  That's OK, just skip it.
          // Duplicates are already filtered out of classpaths.
          if (includeLibRoot) classpath.add(lib.getLibraryRoot());
          if (includeSrcRoot) classpath.add(lib.getSourceRoot());
        }
      }
      return classpath;
    }
  }

  /**
   * Get the owner of this list.
   *
   * @return Sdk that contains the libraries in this list.
   */
  public Sdk getOwner() {
    synchronized (this) {
      return mySdk;
    }
  }

  /**
   * Tell whether this classpath is empty (has no entries).  This is a constant
   * time operation, whereas size() would not be.
   *
   * @return true if empty, false if not.
   */
  public boolean isEmpty() {
    // hasEntries is already synchronized
    return !hasEntries();
  }

  /**
   * Iterate over the list and perform a task.  The lambda must return a boolean
   * value indicating whether to continue iterating through the paths, or stop
   * immediately: true to continue, false to stop.
   *
   * @param lambda functional interface for the action to perform.
   * @return what the action returned: true to keep going, or false to stop.
   */
  public boolean iterate(@NotNull Lambda lambda) {
    boolean continu = true;
    if (hasEntries()) {
      synchronized (this) {
        for (HaxeLibraryReference entry : getEntries()) {
          continu = lambda.processEntry(entry);
          if (!continu)
            break;
        }
      }
    }
    return continu;
  }

  /**
   * Remove a library from the list.
   *
   * @param item to remove.
   */
  public void remove(@Nullable HaxeLibraryReference item) {
    if (null == item || !hasEntries())
      return;

    synchronized(this) {
      getEntries().remove(item);
    }
  }

  /**
   * Remove all entries from this list that occur in the given list.
   * It is NOT an error of otherpath contains entries that do not exist in
   * this classpath.
   *
   * @param otherPath with entries to remove
   */
  public void removeAll(@Nullable HaxeLibraryList otherPath) {
    if (null == otherPath || otherPath.isEmpty())
      return;
    synchronized(this) {
      if (hasEntries()) {
        synchronized (otherPath) {
          getEntries().removeAll(otherPath.getEntries());
        }
      }
    }
  }

  /**
   * Remove all entries from this list that occur in the given list.
   * It is NOT an error to specify entries that do not already exist.
   *
   * @param otherEntries to remove.
   */
  public void removeAll(@Nullable Collection<HaxeLibraryReference> otherEntries) {
    if (null == otherEntries || otherEntries.isEmpty())
      return;

    synchronized(this) {
      if (this.hasEntries()) {
        Iterator iterator = getEntries().iterator();
        while (iterator.hasNext()) {
          HaxeClasspathEntry entry = (HaxeClasspathEntry)iterator.next();
          if (otherEntries.contains(entry)) {
            iterator.remove();
          }
        }
      }
    }
  }

  /**
   * Get the number of entries in this list.  BEWARE: This is NOT a
   * constant time operation.  It is dependent upon the number of elements
   * in this list.
   *
   * @return the number of entries.
   */
  public int size() {
    synchronized(this) {
      if (hasEntries()) {
        return getEntries().size();
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    return "size = " + size();
  }

  /**
   * Wrapper to do a discrete bit of work while honoring the synchronized
   * aspects of the path.
   */
  public interface Lambda {
    /**
     * Process a single entry in the library list.  The list is already
     * synchronized when this is called.
     *
     * @param entry A class path element (path).
     * @return true if the loop should keep running, false if not.
     */
    public boolean processEntry(HaxeLibraryReference entry);
  }

  /**
   * Dumps this classpath to a single idea.log entry.
   * @param header to display at the start of the entry.
   */
  public void debugDump(String header) {

    final StringBuilder log = new StringBuilder();
    if (null != header && !header.isEmpty()) {
      log.append(header);
    }

    iterate ( new Lambda() {
      @Override
      public boolean processEntry(HaxeLibraryReference entry) {
        HaxeLibrary lib = entry.getLibrary();
        String version = lib != null ? lib.getVersion().toString() : "invalid reference";
        log.append("\n   ");
        log.append(entry.getPresentableName());
        log.append(": ");
        log.append(version);
        return true;
      }
    });
    LOG.debug(log.toString());
  }

}
