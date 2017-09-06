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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.IOException;

/**
 * Created by ebishton on 8/10/17.
 */
public class HaxeFileUtil {

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
        String canonicalUri = absolute.toURI().toString();
        String ideaUri = vfm.constructUrl(absolute.toURI().getScheme(), absolute.getPath());
        return VirtualFileManager.getInstance().findFileByUrl(ideaUri);
      }
    } catch(IOException e) {
      ; // Swallow
    }
    return null;
  }

}
