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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeModuleSourceRoot;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectStructureDetector;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.model.util.HaxeNameUtils;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HaxeProjectModel {
  static private Key<HaxeProjectModel> HAXE_PROJECT_MODEL = new Key<HaxeProjectModel>("HAXE_PROJECT_MODEL");

  @NotNull
  public final HaxePackageModel rootPackage;
  private final Project project;
  private HaxeTypesModel types;

  private HaxeProjectModel(Project project) {
    this.project = project;
    this.rootPackage = new HaxePackageModel(this, "", null);
  }

  public HaxeTypesModel getTypes() {
    if (this.types == null) this.types = new HaxeTypesModel(this);
    return this.types;
  }

  public Project getProject() {
    return project;
  }

  public String getName() {
    return project.getName();
  }

  public HaxePackageModel getPackageFromPath(String path) {
    return rootPackage.accessOrCreate(path);
  }

  @Nullable
  public HaxeSourceRootModel getRootContaining(PsiFile file) {
    return getRootContaining(file.getParent());
  }

  @Nullable
  public String getPathToDirectory(PsiDirectory directory) {
    HaxeSourceRootModel root = getRootContaining(directory);
    return root != null ? root.getPathToFile(directory) : null;
  }

  @Nullable
  public String getPathToFile(PsiFile file) {
    String path = getPathToDirectory(file.getParent());
    if (path == null) return null;
    return path + "/" + file.getName();
  }

  @Nullable
  public HaxeSourceRootModel getRootContaining(PsiDirectory dir) {
    List<HaxeSourceRootModel> roots = getRoots();
    if (dir == null) return null;
    for (HaxeSourceRootModel root : roots) {
      if (root.contains(dir)) {
        return root;
      }
    }
    return null;
  }

  public List<HaxeSourceRootModel> getRoots() {
    ArrayList<HaxeSourceRootModel> out = new ArrayList<HaxeSourceRootModel>();
    //for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(project).recursively().withoutSdk().exportedOnly().sources().getRoots()) {
    for (VirtualFile sourceRoot : OrderEnumerator.orderEntries(project).recursively().exportedOnly().sources().getRoots()) {
      out.add(new HaxeSourceRootModel(project, sourceRoot));
    }
    return out;
  }

  static public HaxeProjectModel fromElement(PsiElement element) {
      //ModuleUtilCore.findModuleForPsiElement(element);
    return fromProject(element.getProject());
  }

  static public HaxeProjectModel fromProject(Project project) {
    HaxeProjectModel model = project.getUserData(HAXE_PROJECT_MODEL);
    if (model == null) {
      project.putUserData(HAXE_PROJECT_MODEL, model = new HaxeProjectModel(project));
    }
    return model;
  }

  public HaxePackageModel getPackageFromFile(PsiFile file) {
    return HaxePackageModel.getPackageFromFile(file);
  }
}
