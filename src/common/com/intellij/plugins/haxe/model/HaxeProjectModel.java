/*
 * Copyright 2017-2018 Ilya Malanin
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.model.HaxeStdTypesFileModel.STD_TYPES_HX;

public class HaxeProjectModel {
  private static final Key<HaxeProjectModel> HAXE_PROJECT_MODEL_KEY = new Key<>("HAXE_PROJECT_MODEL");
  private final Project project;

  private boolean rootsInitialized = false;
  private List<HaxeSourceRootModel> roots;
  private HaxeSourceRootModel sdkRoot;
  private HaxePackageModel stdPackage;

  private HaxeProjectModel(Project project) {
    this.project = project;
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

  private HaxeSourceRootModel resolveSdkRoot() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final VirtualFile[] roots;

      roots = OrderEnumerator.orderEntries(project).getAllSourceRoots();
      if (roots.length > 0) {
        VirtualFile stdRootForTests = roots[0].findChild("std");
        if (stdRootForTests != null) {
          return new HaxeSourceRootModel(this, stdRootForTests);
        }
      }
    } else {
      VirtualFile root = detectProperSDKSourceRoot(OrderEnumerator.orderEntries(project).sdkOnly().getAllSourceRoots());

      if (root != null) {
        return new HaxeSourceRootModel(this, root);
      }
    }
    return HaxeSourceRootModel.DUMMY;
  }

  private VirtualFile detectProperSDKSourceRoot(VirtualFile[] roots) {
    for (VirtualFile root : roots) {
      if (root.findChild(STD_TYPES_HX) != null) return root;
    }

    return null;
  }

  public Project getProject() {
    return project;
  }

  public String getName() {
    return project.getName();
  }

  public List<HaxeSourceRootModel> getRoots() {
    if (!rootsInitialized) setupRoots();
    return roots;
  }

  private void setupRoots() {
    validateRoots();
    project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        validateRoots();
      }
    });
    rootsInitialized = true;
  }

  private void validateRoots() {
    final OrderEnumerator enumerator = OrderEnumerator.orderEntries(project).withoutSdk();

    roots = Stream.concat(
      Arrays.stream(enumerator.getClassesRoots()),
      Arrays.stream(enumerator.getSourceRoots())
    )
      .distinct()
      .map(root -> new HaxeSourceRootModel(this, root))
      .collect(Collectors.toList());

    sdkRoot = resolveSdkRoot();
    stdPackage = new HaxeStdPackageModel(this, sdkRoot);
  }

  @NotNull
  public HaxeSourceRootModel getSdkRoot() {
    if (!rootsInitialized) setupRoots();
    return sdkRoot;
  }

  @NotNull
  public HaxePackageModel getStdPackage() {
    if (!rootsInitialized) setupRoots();
    return stdPackage;
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

    if (result.isEmpty() && stdPackage != null) {
      resolvedValue = stdPackage.resolve(info);
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
}