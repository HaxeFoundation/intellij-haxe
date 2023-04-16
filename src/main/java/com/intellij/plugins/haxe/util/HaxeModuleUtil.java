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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

public class HaxeModuleUtil {

  /**
   * Attempt to locate a file in the module directories that may not be a direct source
   * file (like the .hxml files).  If the given file is absolute, then module (.iml) directory,
   * source and classpaths will not be searched.
   *
   * @param module
   * @param haxeProjectPath
   * @return
   */

  @Nullable
  private VirtualFile locateModuleFile(Module module, String haxeProjectPath) {
    // Try to find the file.  If it's absolute, we either get it the first time or give up.
    VirtualFile projectFile = HaxeFileUtil.locateFile(haxeProjectPath);
    if ( ! HaxeFileUtil.isAbsolutePath(haxeProjectPath)) {
      if (null == projectFile) {
        ModuleRootManager rootMgr = ModuleRootManager.getInstance(module);
        projectFile = HaxeFileUtil.locateFile(haxeProjectPath, rootMgr.getSourceRootUrls());
      }
      if (null == projectFile) {
        projectFile = HaxeFileUtil.locateFile(haxeProjectPath, module.getModuleFilePath());
      }
    }
    return projectFile;
  }

}
