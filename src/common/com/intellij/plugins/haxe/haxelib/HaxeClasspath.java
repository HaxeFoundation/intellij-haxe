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

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manage a classpath.
 */
public class HaxeClasspath {

  static Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.haxelib.HaxeClasspath");

  /**
   * An immutable empty classpath that can be re-used.
   */
  public static final HaxeClasspath EMPTY_CLASSPATH = new HaxeClasspath(true);

  // Any access of myOrderedEntries MUST be synchronized if this classpath is
  // used in a multi-threaded environment.
  protected Set<HaxeClasspathEntry> myOrderedEntries;

  /**
   * Constructor used solely to create the EMPTY_CLASSPATH;
   * @param createEmpty
   */
  private HaxeClasspath(boolean createEmpty) {
    myOrderedEntries = Collections.emptySet();
  }

  HaxeClasspath(HaxeClasspath initialEntries) {
    this();
    synchronized (initialEntries) {
      myOrderedEntries.addAll(initialEntries.myOrderedEntries);
    }
  }

  HaxeClasspath(Collection<HaxeClasspathEntry> initialEntries) {
    this(initialEntries.size());
    myOrderedEntries.addAll(initialEntries);
  }

  HaxeClasspath() {
    this(16);
  }

  HaxeClasspath(int sizeHint) {
    myOrderedEntries = new LinkedHashSet<HaxeClasspathEntry>(2 * sizeHint);
  }

  /**
   * Add an entry to the end of this classpath.  If the entry with the same
   * URL already exists, the current one is maintained, along with its current
   * position, and the new one is ignored.
   *
   * @param item to add to the end of the classpath.
   */
  public void add(HaxeClasspathEntry item) {
    synchronized(this) {
      if (!contains(item))
      myOrderedEntries.add(item);
    }
  }

  /**
   * Add a set of entries to this classpath. New entries
   * will be added at the end of this classpath.  Duplicate entries are ignored
   * (current entries with the same URL are maintained, with their current
   * ordering).
   *
   * @param entries to add to the end of this classpath.
   */
  public void addAll(Collection<HaxeClasspathEntry> entries) {
    synchronized(this) {
      myOrderedEntries.addAll(entries);
    }
  }

  /**
   * Add another classpath to this one.  New entries from the other classpath
   * will be added at the end of this classpath.  Duplicate entries are ignored
   * (current entries with the same URL are maintained, with their current
   * ordering).
   *
   * @param classpath to add to this classpath
   */
  public void addAll(HaxeClasspath classpath) {
    synchronized(this) {
      synchronized(classpath) {
        myOrderedEntries.addAll(classpath.myOrderedEntries);
      }
    }
  }

  /**
   * Remove all entries from this classpath.
   */
  public void clear() {
    synchronized(this) {
      myOrderedEntries.clear();
    }
  }

  /**
   * Determine whether this classpath contains an entry that semantically matches
   * the given entry.  (That is, it represents the same file system path.)
   *
   * @param item
   * @return
   */
  public boolean contains(HaxeClasspathEntry item) {
    synchronized(this) {
      return myOrderedEntries.contains(item);
    }
  }

  /**
   * Determine if a given URL is represented by any Entry/Item in the classpath.
   * NOTE: This method *does NOT* attempt to normalize the URL.  Relative paths
   * will NOT match.
   *
   * @param url we are looking for.
   * @return true if an entry matches the URL; false otherwise.
   */
  public boolean containsUrl(final String url) {
    if (null == url || url.isEmpty())
      return false;

    synchronized(this) {
      // OK, this works because the hash code for a HaxeClasspathEntry is the
      // myUrl hash code.  That makes hash lookups the same for urls and entries.
      // It also makes equals work because the HaxeClasspathEntry's equals
      // method is overridden in the same way, to only check the url string.
      class Comparator {
        public int hashCode() { return HaxeClasspathEntry.hashUrl(url); }
        public boolean equals(Object o) {
          HaxeClasspathEntry that = (HaxeClasspathEntry)o;
          if (!that.getUrl().equals(url)) return false;  // In case of hash collision.
          return true;
        }
      }
      return myOrderedEntries.contains(new Comparator());
    }
  }

  /**
   * Tell whether this classpath is empty (has no entries).  This is a constant
   * time operation, whereas size() would not be.
   *
   * @return true if empty, false if not.
   */
  public boolean isEmpty() {
    synchronized(this) {
      return myOrderedEntries.isEmpty();
    }
  }

  /**
   * Iterate over the list and perform a task.  The lambda must return a boolean
   * value indicating whether to continue iterating through the paths, or stop
   * immediately: true to continue, false to stop.
   *
   * @param lambda functional interface for the action to perform.
   * @return what the action returned: true to keep going, or false to stop.
   */
  public boolean iterate(Lambda lambda) {
    boolean continu = true;
    synchronized(this) {
      for (HaxeClasspathEntry entry : myOrderedEntries) {
        continu = lambda.processEntry(entry);
        if (!continu)
          break;
      }
    }
    return continu;
  }

  /**
   * Remove an entry from the classpath.
   *
   * @param item to remove.
   */
  public void remove(HaxeClasspathEntry item) {
    synchronized(this) {
      myOrderedEntries.remove(item);
    }
  }

  /**
   * Remove all entries from this classpath that occur in the given classpath.
   * It is NOT an error of otherpath contains entries that do not exist in
   * this classpath.
   *
   * @param otherPath with entries to remove
   */
  public void removeAll(@NotNull HaxeClasspath otherPath) {
    synchronized(this) {
      synchronized (otherPath) {
        myOrderedEntries.removeAll(otherPath.myOrderedEntries);
      }
    }
  }

  /**
   * Remove all entries from this classpath that occur in the given list.
   * It is NOT an error to specify entries that do not already exist.
   *
   * @param entries to remove.
   */
  public void removeAll(@NotNull Collection<HaxeClasspathEntry> entries) {
    synchronized(this) {
      if (entries.isEmpty() || myOrderedEntries.isEmpty()) {
        return;
      }
      Iterator iterator = myOrderedEntries.iterator();
      while (iterator.hasNext()) {
        HaxeClasspathEntry entry = (HaxeClasspathEntry)iterator.next();
        if (entries.contains(entry)) {
          iterator.remove();
        }
      }
    }
  }

  /**
   * Get the number of entries in this classpath.  BEWARE: This is NOT a
   * constant time operation.  It is dependent upon the number of elements
   * in this classpath.
   *
   * @return the number of entries.
   */
  public int size() {
    synchronized(this) {
      return myOrderedEntries.size();
    }
  }

  /**
   * Wrapper to do a discrete bit of work while honoring the synchronized
   * aspects of the path.
   */
  public interface Lambda {
    /**
     * Process a single entry in the classpath list.  The list is already
     * synchronized when this is called.
     *
     * @param entry A class path element (path).
     * @return true if the loop should keep running, false if not.
     */
    public boolean processEntry(HaxeClasspathEntry entry);
  }

  /**
   * Dumps this classpath to a single idea.log entry.
   * @param header to display at the start of the entry.
   */
  public void debugDump(String header) {
    class Collector implements Lambda {  // XXX: Wish this could be anonymous.
      public String myLog;
      public Collector(String header) {myLog = header;}
      @Override
      public boolean processEntry(HaxeClasspathEntry entry) {
        myLog += "\n   " + entry.getName() + "\n      " + entry.getUrl();
        return true;
      }
    };
    Collector logCollector = new Collector(null == header ? "HaxeClasspath dump" : header);
    iterate(logCollector);
    LOG.debug(logCollector.myLog);
  }

}
