/*
 * Copyright 2017-2017 Ilya Malanin
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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HaxePackageModel implements HaxeExposableModel {
  private final HaxeProjectModel project;
  private final HaxeSourceRootModel root;
  private final HaxePackageModel parent;
  private final String name;
  protected final String path;
  private final FullyQualifiedInfo qualifiedInfo;


  public HaxePackageModel(@NotNull HaxeProjectModel project,
                          @NotNull HaxeSourceRootModel root,
                          @NotNull String name,
                          @Nullable HaxePackageModel parent) {
    this.project = project;
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
    } else if (name.indexOf('.') >= 0) {
      String[] packages = StringUtils.split(name, '.');
      HaxePackageModel result = this;
      for (String packageName : packages) {
        if (result == null) return null;
        result = result.getChild(packageName);
      }

      return result;
    } else {
      PsiDirectory directory = root.access(path.isEmpty() ? name : path + '.' + name);
      if (directory != null) {
        return new HaxePackageModel(project, root, name, this);
      }
    }

    return null;
  }

  @NotNull
  public List<HaxePackageModel> getChildren() {
    PsiDirectory directory = root.access(path);
    if (directory != null) {
      return Arrays.stream(directory.getSubdirectories())
        .map(subDirectory -> new HaxePackageModel(project, root, subDirectory.getName(), this))
        .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  @Nullable
  public HaxeFileModel getFileModel(String fileName) {
    final HaxeFile file = getFile(fileName);
    return file != null ? new HaxeFileModel(file) : null;
  }

  protected HaxeFile getFile(String fileName) {
    PsiDirectory directory = root.access(path);
    if (directory != null && directory.isValid()) {
      PsiFile file = directory.findFile(fileName + ".hx");
      if (file != null && file.isValid() && file instanceof HaxeFile) {
        return (HaxeFile)file;
      }
    }

    return null;
  }

  @Nullable
  public HaxeClassModel getClassModel(@NotNull String className) {
    HaxeFileModel file = getFileModel(className);
    if (file != null) {
      return file.getClassModel(className);
    }
    return null;
  }

  @Override
  public List<HaxeModel> getExposedMembers() {
    PsiDirectory directory = root.access(path);
    if (directory != null) {
      PsiFile[] files = directory.getFiles();

      return Arrays.stream(files)
        .filter(file -> file instanceof HaxeFile)
        .map(file -> {
          HaxeFileModel fileModel = HaxeFileModel.fromElement(file);
          return fileModel != null ? fileModel.getMainClassModel() : null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
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