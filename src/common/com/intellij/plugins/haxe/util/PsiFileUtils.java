/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PsiFileUtils {

  static public String getListPath(List<PsiFileSystemItem> range) {
    String out = "";
    for (PsiFileSystemItem item : range) {
      if (out.length() != 0) out += "/";
      out += item.getName();
    }
    return out;
  }

  static public List<PsiFileSystemItem> getRange(PsiFileSystemItem from, PsiFileSystemItem to) {
    LinkedList<PsiFileSystemItem> items = new LinkedList<PsiFileSystemItem>();
    PsiFileSystemItem current = to;
    while (current != null) {
      items.addFirst(current);
      if (current == from) break;
      current = current.getParent();
    }
    return items;
  }

  static public PsiFileSystemItem findRoot(PsiFileSystemItem file) {
    //HaxelibModuleManager
    Project project = file.getProject();
    Module[] modules = ModuleManager.getInstance(project).getModules();
    List<VirtualFile> roots = new ArrayList<VirtualFile>();
    for (Module module : modules) {
      Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
      roots.addAll(Arrays.asList(sdk.getRootProvider().getFiles(OrderRootType.CLASSES)));
    }
    roots.addAll(Arrays.asList(ProjectRootManager.getInstance(project).getContentSourceRoots()));

    PsiFileSystemItem current = file;
    while (current != null) {
      for (VirtualFile root : roots) {
        //System.out.println(root.getCanonicalPath());
        //System.out.println(current.getVirtualFile().getCanonicalPath());
        if (root.getCanonicalPath().equals(current.getVirtualFile().getCanonicalPath())) return current;
      }
      current = current.getParent();
    }
    return null;
  }

  static public String getDirectoryPath(PsiDirectory dir) {
    String out = "";
    while (dir != null) {
      if (out.length() != 0) out = "/" + out;
      out = dir.getName() + out;
      dir = dir.getParent();
    }
    return out;
  }
}

