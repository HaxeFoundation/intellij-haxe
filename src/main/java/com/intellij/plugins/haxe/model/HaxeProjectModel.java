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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.model.HaxeStdTypesFileModel.STD_TYPES_HX;

/**
 * The root/std model resolution works against. MODULE-SCOPED when obtained
 * via {@link #fromElement}: each module resolves against its OWN SDK's std
 * and its own dependency roots, so modules on different haxe versions never
 * see each other's standard library. Elements outside any module (opened
 * library files, scratches) and {@link #fromProject} callers get the
 * project-wide union.
 */
public class HaxeProjectModel {
  private static final Key<HaxeProjectModel> HAXE_PROJECT_MODEL_KEY = new Key<>("HAXE_PROJECT_MODEL");
  private static final Key<HaxeProjectModel> HAXE_MODULE_MODEL_KEY = new Key<>("HAXE_MODULE_PROJECT_MODEL");

  private final Project project;
  @Nullable private final Module module;

  // caches live on the project-level instance only; module views delegate
  private final Map<Module, RootsCache> moduleRootsCaches = new ConcurrentHashMap<>();
  private volatile RootsCache projectRootsCache;

  private HaxeProjectModel(Project project, @Nullable Module module) {
    this.project = project;
    this.module = module;
    if (module == null) {
      addProjectListeners();
    }
  }

  public static HaxeProjectModel fromElement(PsiElement element) {
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return fromProject(element.getProject());
    }
    HaxeProjectModel model = module.getUserData(HAXE_MODULE_MODEL_KEY);
    if (model == null) {
      model = new HaxeProjectModel(module.getProject(), module);
      module.putUserData(HAXE_MODULE_MODEL_KEY, model);
    }
    return model;
  }

  public static HaxeProjectModel fromProject(Project project) {
    HaxeProjectModel model = project.getUserData(HAXE_PROJECT_MODEL_KEY);
    if (model == null) {
      model = new HaxeProjectModel(project, null);
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

    HaxeSourceRootModel sdkRoot = getSdkRoot();
    if (searchScope != null && sdkRoot.root != null && searchScope.contains(sdkRoot.root)) {
        String ref = info.getPresentableText();
      // hack to fix issue where standard lib members are referenced with "std" prefix in code even though "std" is not part of the package
      // ex. "std.Any", "std.haxe.Json" etc should be resolved correctly even if package does not match (haxe compiler accepts these)
       if (ref.startsWith("std.")) {
         resolvedValue = sdkRoot.resolve(new FullyQualifiedInfo(ref.replaceFirst("std.","")));
       }else {
         resolvedValue = sdkRoot.resolve(info);
       }
      if (resolvedValue != null) result.add(resolvedValue);
    }

    // the std fallback answers from THIS model's sdk root; a scope that
    // excludes that root (another module's scope in a multi-SDK project)
    // must not receive results from it
    boolean stdInScope = searchScope == null || sdkRoot.root == null || searchScope.contains(sdkRoot.root);
    if (result.isEmpty() && stdInScope) {
      resolvedValue = getStdPackage().resolve(info);
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
    List<HaxeModel> result = resolve(new FullyQualifiedInfo(info.packageName, null, null, null), scope);
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
    project.getMessageBus().connect().subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        projectRootsCache = null;
        moduleRootsCaches.clear();
      }
    });
  }

  private RootsCache getRootsCache() {
    HaxeProjectModel projectModel = fromProject(project);
    if (module == null) {
      RootsCache cache = projectModel.projectRootsCache;
      if (cache == null) {
        cache = RootsCache.forProject(this);
        projectModel.projectRootsCache = cache;
      }
      return cache;
    }
    return projectModel.moduleRootsCaches.computeIfAbsent(module, m -> RootsCache.forModule(this, m));
  }
}

class RootsCache {
  final List<HaxeSourceRootModel> roots;
  final HaxeSourceRootModel sdkRoot;
  final HaxeStdPackageModel stdPackageModel;

  final HaxeLogPackageModel logPackageModel;

  private RootsCache(List<HaxeSourceRootModel> roots, HaxeSourceRootModel sdkRoot) {
    this.roots = roots;
    this.sdkRoot = sdkRoot;
    this.stdPackageModel = new HaxeStdPackageModel(sdkRoot);
    this.logPackageModel = new HaxeLogPackageModel(sdkRoot);
  }

  static RootsCache forProject(HaxeProjectModel model) {
    OrderEnumerator withoutSdk = OrderEnumerator.orderEntries(model.getProject()).withoutSdk();
    OrderEnumerator sdkOnly = OrderEnumerator.orderEntries(model.getProject()).sdkOnly();
    OrderEnumerator everything = OrderEnumerator.orderEntries(model.getProject());
    return new RootsCache(collectRoots(model, withoutSdk), findStdRoot(model, sdkOnly, everything));
  }

  /**
   * The module's OWN view: its dependency roots and ITS SDK's std — never
   * another module's. This is what keeps a Haxe 4 module and a Haxe 5 module
   * in one project resolving against their respective standard libraries.
   */
  static RootsCache forModule(HaxeProjectModel model, Module module) {
    OrderEnumerator withoutSdk = OrderEnumerator.orderEntries(module).recursively().withoutSdk();
    OrderEnumerator sdkOnly = OrderEnumerator.orderEntries(module).sdkOnly();
    OrderEnumerator everything = OrderEnumerator.orderEntries(module).recursively();
    return new RootsCache(collectRoots(model, withoutSdk), findStdRoot(model, sdkOnly, everything));
  }

  private static List<HaxeSourceRootModel> collectRoots(HaxeProjectModel model, OrderEnumerator withoutSdk) {
    return Stream.concat(
        Arrays.stream(withoutSdk.getSourceRoots()),
        Arrays.stream(withoutSdk.getClassesRoots())
    )
      .distinct()
      .map(root -> new HaxeSourceRootModel(model, root))
      .collect(Collectors.toList());
  }

  private static HaxeSourceRootModel findStdRoot(HaxeProjectModel model,
                                                OrderEnumerator sdkOnly,
                                                OrderEnumerator everything) {
    VirtualFile[] roots;
    roots = ApplicationManager.getApplication().isUnitTestMode()
            ? everything.getAllSourceRoots()
            : sdkOnly.getAllSourceRoots();
    for (VirtualFile root : roots) {
      if (root.findChild(STD_TYPES_HX) != null) {
        return new HaxeSourceRootModel(model, root);
      }
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      roots = everything.getAllSourceRoots();
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
