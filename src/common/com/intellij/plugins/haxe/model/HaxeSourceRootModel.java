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
package com.intellij.plugins.haxe.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class HaxeSourceRootModel {
  public final Project project;
  public final VirtualFile root;
  public final PsiDirectory rootPsi;

  public HaxeSourceRootModel(Project project, VirtualFile root) {
    this.project = project;
    this.root = root;
    this.rootPsi = PsiManager.getInstance(project).findDirectory(root);
  }

  public String getPathToFile(PsiDirectory item) {
    ArrayList<String> parts = new ArrayList<String>();
    for (;(item != null) && (item != rootPsi); item = item.getParent()) {
      parts.add(0, item.getName());
    }
    return StringUtils.join(parts, '/');
  }

  @Nullable
  public PsiDirectory access(String path) {
    if ((path == null) || path.isEmpty()) return rootPsi;
    PsiDirectory current = rootPsi;
    for (String part : path.split("/")) {
      if (current == null) break;
      current = current.findSubdirectory(part);
    }
    return current;
  }
}
