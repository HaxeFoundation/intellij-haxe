/*
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2020 Eric Bishton
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

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.model.HaxeStdTypesFileModel.STD_TYPES_HX;

public class HaxeProjectModel {
  private static final Key<HaxeProjectModel> HAXE_PROJECT_MODEL_KEY = new Key<>("HAXE_PROJECT_MODEL");
  private final Project project;

  private RootsCache rootsCache;

  private HaxeProjectModel(Project project) {
    this.project = project;
    addProjectListeners();
  }

  public static HaxeProjectModel fromElement(PsiElement element) {
    return fromProject(element.getProject());
  }

  public static HaxeProjectModel fromProject(Project project) {
    HaxeProjectModel model = project.getUserData(HAXE_PROJECT_MODEL_KEY);
    if (model == null) {
      model = new HaxeProjectModel(project);
      project.putUserData(HAXE_PROJECT_MODEL_KEY, model);
    }

    return model;
  }

  public Project getProject() {
    return project;
  }

  public String getName() {
    return project.getName();
  }

  public List<HaxeSourceRootModel> getRoots() {
    return getRootsCache().roots;
  }

  @NotNull
  public HaxeSourceRootModel getSdkRoot() {
    return getRootsCache().sdkRoot;
  }

  @NotNull
  public HaxePackageModel getStdPackage() {
    return getRootsCache().stdPackageModel;
  }
  @NotNull
  public HaxePackageModel getExprPackage() {
    return getRootsCache().exprPackageModel;
  }
  @NotNull
  public HaxeLogPackageModel getLogPackage() {
    return getRootsCache().logPackageModel;
  }

  @Nullable
  public List<HaxeModel> resolve(FullyQualifiedInfo info) {
    return resolve(info, null);
  }

  @Nullable
  public List<HaxeModel> resolve(FullyQualifiedInfo info, @Nullable GlobalSearchScope searchScope) {
    if (info == null) return null;
    HaxeModel resolvedValue;
    List<HaxeModel> result = new ArrayList<>();
    for (HaxeSourceRootModel root : getRoots()) {
      if (searchScope == null || !searchScope.contains(root.root)) {
        continue;
      }
      resolvedValue = root.resolve(info);
      if (resolvedValue != null) result.add(resolvedValue);
    }

    if (result.isEmpty()) {
      resolvedValue = getStdPackage().resolve(info);
      if (resolvedValue != null) result.add(resolvedValue);
    }

    if (result.isEmpty()) {
      resolvedValue = getExprPackage().resolve(info);
      if (resolvedValue != null) result.add(resolvedValue);
    }

    return result;
  }

  @Nullable
  public HaxePackageModel resolvePackage(FullyQualifiedInfo info) {
    return resolvePackage(info, null);
  }

  @Nullable
  public HaxePackageModel resolvePackage(FullyQualifiedInfo info, @Nullable GlobalSearchScope scope) {
    List<HaxeModel> result = resolve(new FullyQualifiedInfo(info.packagePath, null, null, null), scope);
    if (result != null && !result.isEmpty() && result.get(0) instanceof HaxePackageModel) {
      return (HaxePackageModel)result.get(0);
    }
    return null;
  }

  public HaxeSourceRootModel getContainingRoot(PsiDirectory parent) {
    if (parent == null) return null;

    for (HaxeSourceRootModel root : getRoots()) {
      if (root.contains(parent)) {
        return root;
      }
    }
    return null;
  }

  private void addProjectListeners() {
    project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        rootsCache = null;
      }
    });
  }

  private RootsCache getRootsCache() {
    if (rootsCache == null) {
      rootsCache = RootsCache.fromProjectModel(this);
    }
    return rootsCache;
  }
}

class RootsCache {
  final List<HaxeSourceRootModel> roots;
  final HaxeSourceRootModel sdkRoot;
  final HaxeStdPackageModel stdPackageModel;
  final HaxeExprPackageModel exprPackageModel;

  final HaxeLogPackageModel logPackageModel;

  private RootsCache(List<HaxeSourceRootModel> roots, HaxeSourceRootModel sdkRoot) {
    this.roots = roots;
    this.sdkRoot = sdkRoot;
    this.stdPackageModel = new HaxeStdPackageModel(sdkRoot);
    this.exprPackageModel = new HaxeExprPackageModel(sdkRoot);
    this.logPackageModel = new HaxeLogPackageModel(sdkRoot);
  }

  static RootsCache fromProjectModel(HaxeProjectModel model) {
    return new RootsCache(getProjectRoots(model), getSdkRoot(model));
  }

  private static List<HaxeSourceRootModel> getProjectRoots(final HaxeProjectModel model) {
    final OrderEnumerator enumerator = OrderEnumerator.orderEntries(model.getProject()).withoutSdk();

    return Stream.concat(
        Arrays.stream(enumerator.getSourceRoots()),
        Arrays.stream(enumerator.getClassesRoots())
    )
      .distinct()
      .map(root -> new HaxeSourceRootModel(model, root))
      .collect(Collectors.toList());
  }

  private static HaxeSourceRootModel getSdkRoot(final HaxeProjectModel model) {
    VirtualFile[] roots;
    roots = ApplicationManager.getApplication().isUnitTestMode()
            ? OrderEnumerator.orderEntries(model.getProject()).getAllSourceRoots()
            : OrderEnumerator.orderEntries(model.getProject()).sdkOnly().getAllSourceRoots();
    for (VirtualFile root : roots) {
      if (root.findChild(STD_TYPES_HX) != null) {
        return new HaxeSourceRootModel(model, root);
      }
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      roots = OrderEnumerator.orderEntries(model.getProject()).getAllSourceRoots();
      if (roots.length > 0) {
        VirtualFile stdRootForTests = roots[0].findChild("std");
        if (stdRootForTests != null) {
          return new HaxeSourceRootModel(model, stdRootForTests);
        }
      }
    }
    return HaxeSourceRootModel.DUMMY;
  }
}