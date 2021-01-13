/*
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.util.HaxeFileUtil;
import com.intellij.plugins.haxe.util.HaxeNameUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class HaxePackageModel implements HaxeExposableModel {
  private final HaxeProjectModel project;
  private final HaxeSourceRootModel root;
  private final HaxePackageModel parent;
  private final String name;
  protected final String path;
  private final FullyQualifiedInfo qualifiedInfo;


  public HaxePackageModel(@NotNull HaxeSourceRootModel root,
                          @NotNull String name,
                          @Nullable HaxePackageModel parent) {
    this.project = root.project;
    this.name = name;
    this.root = root;
    this.parent = parent;

    if (parent != null && !parent.path.isEmpty()) {
      path = parent.path + '.' + name;
    } else {
      path = name;
    }

    qualifiedInfo = new FullyQualifiedInfo(path, null, null, null);
  }

  public HaxeProjectModel getProject() {
    return project;
  }

  public String getName() {
    return name;
  }

  public HaxeModel resolve(String fullyQualifiedName) {
    return resolve(new FullyQualifiedInfo(fullyQualifiedName));
  }

  public HaxeModel resolve(FullyQualifiedInfo info) {
    if (info.packagePath.equals(this.path)) {
      if (info.fileName == null && info.className == null) return this;
      HaxeFileModel file = getFileModel(info.fileName);
      if (file != null) return file.resolve(info);
      return null;
    } else if (info.packagePath.indexOf(path) == 0 || path.isEmpty()) {
      String searchName = path.isEmpty() ? info.packagePath : info.packagePath.substring(path.length() + 1);
      HaxePackageModel child = getChild(searchName);
      if (child != null) {
        return child.resolve(info);
      }
      return null;
    }
    return null;
  }

  @Nullable
  public HaxePackageModel getChild(@NotNull String name) {
    if (name.isEmpty()) {
      return this;
    }

    int index = name.indexOf('.');
    if (-1 == index) {
      PsiDirectory directory = root.access(path.isEmpty() ? name : path + '.' + name);
      if (directory != null) {
        return new HaxePackageModel(root, name, this);
      }
      return null;
    }

    HaxePackageModel child = new HaxePackageModel(root, name.substring(0,index), this);
    return child.getChild(name.substring(index+1));
  }


  @NotNull
  public List<HaxePackageModel> getChildren() {
    PsiDirectory directory = root.access(path);
    if (directory != null) {
      return Arrays.stream(directory.getSubdirectories())
        .map(subDirectory -> new HaxePackageModel(root, subDirectory.getName(), this))
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Nullable
  public HaxeFileModel getFileModel(String fileName) {
    final HaxeFile file = getFile(fileName);
    return file != null ? HaxeFileModel.fromElement(file) : null;
  }

  protected HaxeFile getFile(String filePath) {
    List<String> parts = HaxeFileUtil.splitPath(filePath);
    String fname = parts.get(parts.size() - 1);

    if (null != fname && !fname.isEmpty()) {
      String packagePath = HaxeFileUtil.joinPath(parts.subList(0, parts.size() - 1));
      String accessPath = null != packagePath && !packagePath.isEmpty() ? HaxeFileUtil.joinPath(path, packagePath) : path;
      PsiDirectory directory = root.access(accessPath);

      if (directory != null && directory.isValid()) {
        PsiFile file = directory.findFile(fname + ".hx");
        if (file != null && file.isValid() && file instanceof HaxeFile) {
          return (HaxeFile)file;
        }
      }
    }

    return null;
  }

  @Nullable
  public HaxeClassModel getClassModel(@NotNull String className) {
    String fileName = HaxeNameUtils.classNameToFileName(className);
    HaxeFileModel file = getFileModel(fileName);
    if (file != null) {
      return file.getClassModel(className);
    }
    return null;
  }

  @NotNull
  @Override
  public List<HaxeModel> getExposedMembers() {
    PsiDirectory directory = root.access(path);
    if (directory != null) {
      PsiFile[] files = directory.getFiles();

      List<HaxeModel>  result = new ArrayList<>();
      for(PsiFile file : files) {
        if( file instanceof HaxeFile) {
          HaxeFileModel fileModel = HaxeFileModel.fromElement(file);
          if(fileModel != null)result.addAll(fileModel.getExposedMembers());
        }
      }


      return result;
    }

    return Collections.emptyList();
  }

  @Override
  public PsiElement getBasePsi() {
    return JavaPsiFacade.getInstance(this.project.getProject()).findPackage(path);
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    if (parent != null) {
      return parent;
    }

    return null;
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    return qualifiedInfo;
  }

  public HaxeSourceRootModel getRoot() {
    return root;
  }

  public HaxePackageModel getParent() {
    return parent;
  }
}