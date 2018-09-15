/*
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2018 Eric Bishton
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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.util.indexing.IndexingDataKeys;
import org.jetbrains.annotations.Nullable;

public class HaxeSourceRootModel {
  public static final HaxeSourceRootModel DUMMY = new HaxeSourceRootModel(null, null);
  public final HaxeProjectModel project;
  public final VirtualFile root;
  public final PsiDirectory directory;
  private final HaxePackageModel rootPackage;

  public HaxeSourceRootModel(HaxeProjectModel projectModel, VirtualFile root) {
    this.project = projectModel;
    this.root = root;
    this.directory = projectModel != null && root != null ? PsiManager.getInstance(project.getProject()).findDirectory(root) : null;
    if (project == null || root == null) {
      rootPackage = null;
    }
    else {
      rootPackage = new HaxePackageModel(this, "", null);
    }
  }


  public boolean contains(PsiFileSystemItem file) {
    if (this == DUMMY) return false;

    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      virtualFile = file.getOriginalElement().getContainingFile().getVirtualFile();
    }
    if (virtualFile == null) {
      virtualFile = file.getUserData(IndexingDataKeys.VIRTUAL_FILE);
    }

    return virtualFile != null && (virtualFile.getCanonicalPath() + '/').startsWith(root.getCanonicalPath() + '/');
  }

  @Nullable
  public PsiDirectory access(String packagePath) {
    if (this == DUMMY) return null;

    if ((packagePath == null) || packagePath.isEmpty()) return directory;
    PsiDirectory current = directory;
    for (String part : HaxeStringUtil.split(packagePath, '.')) {
      if (current == null) break;
      current = current.findSubdirectory(part);
    }
    return current;
  }

  public HaxeModel resolve(FullyQualifiedInfo info) {
    if (rootPackage == null) {
      return null;
    }
    return rootPackage.resolve(info);
  }

  @Nullable
  public String resolvePath(PsiFileSystemItem fileSystemItem) {
    String rootPath = root.getPath();
    String itemPath = fileSystemItem.getVirtualFile().getPath();
    if (itemPath.equals(rootPath)) {
      return "";
    }

    if (itemPath.startsWith(rootPath)) {
      return itemPath.substring(rootPath.length() + 1);
    }

    return null;
  }
}