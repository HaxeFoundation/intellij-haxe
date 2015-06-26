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

import com.intellij.plugins.haxe.model.resolver.HaxeResolver2Package;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class HaxePackageModel {
  @Nullable final HaxePackageModel parent;
  @NotNull public final String fullName;
  @NotNull public final String name;
  @NotNull public final HaxeProjectModel project;

  public HaxePackageModel(@NotNull HaxeProjectModel project, @NotNull String name, @Nullable HaxePackageModel parent) {
    this.project = project;
    this.name = name;
    this.parent = parent;
    if (parent != null && !parent.fullName.isEmpty()) {
      this.fullName = parent.fullName + "." + name;
    } else {
      this.fullName = name;
    }
  }

  @Nullable
  static public HaxePackageModel getPackageFromFile(PsiFile file) {
    HaxeProjectModel project = HaxeProjectModel.fromElement(file);
    HaxeSourceRootModel root = project.getRootContaining(file);
    if (root != null) {
      String pathToPackage = root.getPathToFile(file.getParent());
      return project.rootPackage.access(pathToPackage);
    }
    return null;
  }


  private HaxePackageModel createChild(String name) {
    return new HaxePackageModel(project, name, this);
  }

  @Nullable
  public HaxePackageModel access(String path) {
    if (path.isEmpty()) return this;
    if (path.contains(".")) {
      int i = path.indexOf('.');
      HaxePackageModel child = getChild(path.substring(0, i));
      return (child != null) ? child.access(path.substring(i + 1)) : null;
    } else {
      return getChild(path);
    }
  }

  @NotNull
  public HaxeProjectModel getProject() {
    return project;
  }

  @Nullable
  public HaxePackageModel getChild(String name) {
    return null;
  }

  public Set<HaxePackageModel> getChilds() {
    Set<HaxePackageModel> childs = new HashSet<HaxePackageModel>();
    for (PsiDirectory packageDirectory : getPackageDirectories()) {
      for (PsiDirectory directory : packageDirectory.getSubdirectories()) {
        childs.add(createChild(directory.getName()));
      }
    }
    return childs;
  }

  public List<PsiDirectory> getPackageDirectories() {
    LinkedList<PsiDirectory> out = new LinkedList<PsiDirectory>();
    String fullPath = fullName.replace('.', '/');
    for (HaxeSourceRootModel model : project.getRoots()) {
      PsiDirectory packageDirectory = model.access(fullPath);
      if (packageDirectory != null) out.push(packageDirectory);
    }
    return out;
  }

  public HaxeClassModel getHaxeClass(String name) {
    String hxname = name + ".hx";
    List<HaxeSourceRootModel> roots = project.getRoots();
    List<PsiDirectory> directories = getPackageDirectories();
    for (PsiDirectory directory : directories) {
      PsiFile file = directory.findFile(hxname);
      if (file != null) {
        HaxeFileModel fileModel = HaxeFileModel.fromElement(file);
        return fileModel.getHaxeClass(name);
      }
    }

    return null;
  }

  public List<HaxeClassModel> getHaxeClasses() {
    throw new NotImplementedException();
  }

  public HaxeResolver2Package getResolver() {
    return new HaxeResolver2Package(this);
  }
}
