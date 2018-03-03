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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by ebishton on 8/10/17.
 */
public class HaxeFileUtil {

  /** File-system-independent separator char.  This is what comes back from VirtualFile
   * and FileUtil.normalize();
   */
  public static final char SEPARATOR = '/';
  public static final String SEPARATOR_STRING = "" + SEPARATOR;

  private HaxeFileUtil() {}

  /**
   * Get the canonical name of a file, even when the directories are symlinks.
   *
   * @param file
   * @return
   */
  public static VirtualFile getCanonicalFile(VirtualFile file) {
    try {
      java.io.File f = new java.io.File(file.getPath());
      java.io.File absolute = null == f ? null : f.getCanonicalFile();
      if (null != absolute) {
        // Of course, IDEA's notion of a URI requires "://" after the protocol separator
        // (as opposed to Java's ":").  So we can't just use the Java URI.
        VirtualFileManager vfm = VirtualFileManager.getInstance();
        String ideaUri = vfm.constructUrl(absolute.toURI().getScheme(), absolute.getPath());
        return VirtualFileManager.getInstance().findFileByUrl(ideaUri);
      }
    } catch(IOException e) {
      ; // Swallow
    }
    return null;
  }

  /**
   * Quick 'n' dirty url fixup, if necessary.
   *
   * @param url
   * @return
   */
  @Nullable
  public static String fixUrl(@Nullable String url) {
    if (null == url || url.isEmpty())
      return url;
    return url.startsWith(LocalFileSystem.PROTOCOL)
           ? url
           : VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, url);
  }

  /**
   * Determine whether a path/url is an absolute file name.
   *
   * @param path path to test
   * @return true if the path is absolute
   */
  public static boolean isAbsolutePath(@Nullable String path) {
    if (null == path || path.isEmpty()) {
      return false;
    }
    String stripped = VirtualFileManager.getInstance().extractPath(path);
    return (FileUtil.isAbsolute(stripped));
  }

  /**
   * Make a relative path out of a list of strings. (First char is NOT a separator.)
   * @param strings ordered set of strings to use as directory names.
   * @return
   */
  @Nullable
  public static String joinPath(@Nullable List<String> strings) {
    if (null == strings || strings.isEmpty()) {
      return null;
    }
    return HaxeStringUtil.join(SEPARATOR_STRING, strings);
  }

  /**
   * Make a relative path out of a list of strings. (First char is NOT a separator.)
   * @param strings ordered set of strings to use as directory names.
   * @return
   */
  @Nullable
  public static String joinPath(@NotNull String... strings) {
    if (null == strings || 0 == strings.length) {
      return null;
    }
    return HaxeStringUtil.join(SEPARATOR_STRING, strings);
  }

  /**
   * Split a path into a list of strings.
   * @param path Path to split.
   * @return a list of strings, or Collections.EMPTY_LIST if path was null or empty.
   */
  @NotNull
  public static List<String> splitPath(@Nullable String path) {
    if (null == path || path.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    String p = FileUtil.normalize(path);
    String[] parts = p.split(SEPARATOR_STRING);
    return Arrays.asList(parts);
  }

  /**
   * Find a file, given a path.  If the path is not absolute, then try as relative
   * to other given paths.
   *
   * @param path Path to test.  Null or empty path is not an error, but will return a null result.
   * @param rootPaths Variable number of other root paths to search.
   * @return null if the file could not be located; a VirtualFile if it could.
   */
  @Nullable
  public static VirtualFile locateFile(String path, String... rootPaths) {
    if (null == path || path.isEmpty()) {
      return null;
    }

    String tryPath = fixUrl(path);

    VirtualFileManager vfs = VirtualFileManager.getInstance();
    VirtualFile file = vfs.findFileByUrl(tryPath);
    if (null == file && !isAbsolutePath(tryPath)) {
      String nonUrlPath = vfs.extractPath(path);
      for (String root : rootPaths) {
        if (null != root && !root.isEmpty()) {
          tryPath = fixUrl(joinPath(root, nonUrlPath));
          file = vfs.findFileByUrl(tryPath);
          if (null != file) {
            break;
          }
        }
      }
    }

    return file;
  }
}
